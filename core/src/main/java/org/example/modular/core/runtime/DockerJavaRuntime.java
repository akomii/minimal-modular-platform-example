package org.example.modular.core.runtime;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import java.io.Closeable;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Docker-backed {@link ModuleRuntime}: runs each module as a single container named after the module id, through the Docker Engine API.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "modules.runtime", havingValue = "docker", matchIfMissing = true)
public class DockerJavaRuntime implements ModuleRuntime {

  private final DockerClient dockerClient;
  private final String modulesDir;

  public DockerJavaRuntime(DockerClient dockerClient, @Value("${modules.dir}") String modulesDir) {
    this.dockerClient = dockerClient;
    this.modulesDir = modulesDir;
  }

  @Override
  public ModuleStatus status(ModuleDefinition module) {
    try {
      String dockerState = dockerClient.inspectContainerCmd(module.getId())
          .exec()
          .getState()
          .getStatus();
      ModuleStatus status = toModuleStatus(dockerState);
      log.debug("Module {} status: {}", module.getId(), status);
      return status;
    } catch (NotFoundException exception) {
      log.debug("Module {} not found", module.getId());
      return ModuleStatus.NOT_CREATED;
    } catch (Exception exception) {
      if (isConnectionError(exception)) {
        log.error("Docker connection failed while checking status for {}", module.getId());
        throw new DockerConnectionException("Docker daemon is unreachable", exception);
      }
      log.error("Failed to check status for {}", module.getId(), exception);
      throw new RuntimeException("Failed to retrieve Docker status for module: " + module.getId(), exception);
    }
  }

  /**
   * Walks the exception's cause chain to detect a refused or dropped connection to the Docker daemon.
   */
  private boolean isConnectionError(Exception exception) {
    Throwable current = exception;
    while (current != null) {
      if (current instanceof ConnectException ||
          (current.getMessage() != null && current.getMessage().contains("Connection refused"))) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  /**
   * Maps a raw Docker container state to a {@link ModuleStatus}, treating exited/created/dead as STOPPED and anything unrecognised as UNKNOWN.
   */
  private static ModuleStatus toModuleStatus(String dockerState) {
    if (dockerState == null) {
      return ModuleStatus.UNKNOWN;
    }
    return switch (dockerState.toLowerCase()) {
      case "running" -> ModuleStatus.RUNNING;
      case "exited", "created", "dead" -> ModuleStatus.STOPPED;
      default -> ModuleStatus.UNKNOWN;
    };
  }

  @Override
  public void install(ModuleDefinition module, Map<String, String> extraEnv) {
    log.info("Installing module {}", module.getId());
    if (status(module) != ModuleStatus.NOT_CREATED) {
      log.warn("Cannot install {}: already installed", module.getId());
      throw new InvalidModuleStateException("Module is already installed: " + module.getId());
    }
    pullImage(module.getImage());
    createContainer(module, extraEnv);
    log.info("Successfully installed module {}", module.getId());
  }

  /**
   * Pulls the image and blocks until the pull completes, restoring the thread's interrupt flag if interrupted.
   */
  private void pullImage(String imageName) {
    log.info("Pulling image {}", imageName);
    try {
      dockerClient.pullImageCmd(imageName)
          .exec(new PullImageResultCallback())
          .awaitCompletion();
      log.info("Successfully pulled image {}", imageName);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Image pull interrupted for {}", imageName, e);
      throw new RuntimeException("Image pull interrupted for: " + imageName, e);
    } catch (Exception e) {
      log.error("Failed to pull image {}", imageName, e);
      throw new RuntimeException("Failed to pull image: " + imageName, e);
    }
  }

  /**
   * Creates the container with its port bindings, read-only mounts and merged env, and verifies Docker returned a container id.
   */
  private void createContainer(ModuleDefinition module, Map<String, String> extraEnv) {
    log.debug("Creating container for module {}", module.getId());
    Ports portBindings = portBindings(module);
    HostConfig hostConfig = HostConfig.newHostConfig()
        .withPortBindings(portBindings)
        .withBinds(binds(module))
        // lets containers reach services on the docker host (postgres, keycloak)
        .withExtraHosts("host.docker.internal:host-gateway");
    CreateContainerResponse response = dockerClient.createContainerCmd(module.getImage())
        .withName(module.getId())
        .withExposedPorts(List.copyOf(portBindings.getBindings().keySet()))
        .withEnv(env(module, extraEnv))
        .withHostConfig(hostConfig)
        .exec();
    if (response.getId() == null || response.getId().isBlank()) {
      log.error("Container creation returned empty ID for module {}", module.getId());
      throw new IllegalStateException("Container creation failed for module: " + module.getId());
    }
    log.debug("Container created with ID {}", response.getId());
  }

  /**
   * Merges the manifest env with platform-injected {@code extraEnv} and resolves {@code ${VAR}} references against the injected values.
   */
  private static List<String> env(ModuleDefinition module, Map<String, String> extraEnv) {
    Map<String, String> merged = new LinkedHashMap<>(module.getEnv());
    merged.putAll(extraEnv);
    // manifest values may reference platform-injected variables as ${VAR} (e.g. generated secrets)
    merged.replaceAll((key, value) -> substitute(value, extraEnv));
    return merged.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList();
  }

  /**
   * Replaces each {@code ${name}} placeholder in the value with the matching variable.
   */
  private static String substitute(String value, Map<String, String> vars) {
    String result = value;
    for (Map.Entry<String, String> var : vars.entrySet()) {
      result = result.replace("${" + var.getKey() + "}", var.getValue());
    }
    return result;
  }

  /**
   * Resolves each manifest mount to a read-only bind under the configured modules directory.
   */
  private List<Bind> binds(ModuleDefinition module) {
    return module.getMounts().stream()
        .map(mount -> new Bind(
            Paths.get(modulesDir).toAbsolutePath().resolve(mount.getSource()).toString(),
            new Volume(mount.getTarget()),
            AccessMode.ro))
        .toList();
  }

  /**
   * Parses the module's {@code host:container} port mappings into Docker port bindings.
   */
  private static Ports portBindings(ModuleDefinition module) {
    Ports bindings = new Ports();
    if (module.getPorts() == null) {
      return bindings;
    }
    for (String mapping : module.getPorts()) {
      String[] parts = mapping.split(":");
      if (parts.length != 2) {
        log.error("Invalid port mapping {} for module {}", mapping, module.getId());
        throw new IllegalArgumentException("Invalid port mapping: " + mapping);
      }
      ExposedPort containerPort = ExposedPort.tcp(Integer.parseInt(parts[1]));
      bindings.bind(containerPort, Binding.bindPort(Integer.parseInt(parts[0])));
    }
    return bindings;
  }

  @Override
  public void start(ModuleDefinition module) {
    log.info("Starting module {}", module.getId());
    ModuleStatus currentStatus = status(module);
    if (currentStatus == ModuleStatus.NOT_CREATED) {
      log.warn("Cannot start {}: not installed", module.getId());
      throw new InvalidModuleStateException("Module is not installed: " + module.getId());
    }
    if (currentStatus == ModuleStatus.RUNNING) {
      log.info("Module {} is already running", module.getId());
      return;
    }
    dockerClient.startContainerCmd(module.getId()).exec();
    log.info("Successfully started module {}", module.getId());
  }

  @Override
  public void stop(ModuleDefinition module) {
    log.info("Stopping module {}", module.getId());
    ModuleStatus currentStatus = status(module);
    if (currentStatus == ModuleStatus.NOT_CREATED) {
      log.warn("Cannot stop {}: not installed", module.getId());
      throw new InvalidModuleStateException("Module is not installed: " + module.getId());
    }
    if (currentStatus != ModuleStatus.RUNNING) {
      log.info("Module {} is not running", module.getId());
      return;
    }
    dockerClient.stopContainerCmd(module.getId()).exec();
    log.info("Successfully stopped module {}", module.getId());
  }

  @Override
  public void remove(ModuleDefinition module) {
    log.info("Removing module {}", module.getId());
    ModuleStatus currentStatus = status(module);
    if (currentStatus == ModuleStatus.NOT_CREATED) {
      log.warn("Cannot remove {}: not installed", module.getId());
      throw new InvalidModuleStateException("Module is not installed: " + module.getId());
    }
    if (currentStatus == ModuleStatus.RUNNING) {
      log.warn("Cannot remove {}: currently running", module.getId());
      throw new InvalidModuleStateException("Module is currently running and must be stopped before removal: " + module.getId());
    }
    dockerClient.removeContainerCmd(module.getId()).exec();
    log.info("Successfully removed module {}", module.getId());
  }

  @Override
  public Closeable streamLogs(ModuleDefinition module, LogSink sink) {
    if (status(module) == ModuleStatus.NOT_CREATED) {
      throw new InvalidModuleStateException("Module is not installed: " + module.getId());
    }
    log.info("Streaming logs for module {}", module.getId());
    ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
      @Override
      public void onNext(Frame frame) {
        sink.line(new String(frame.getPayload(), StandardCharsets.UTF_8));
      }

      @Override
      public void onComplete() {
        sink.complete();
      }

      @Override
      public void onError(Throwable throwable) {
        sink.error(throwable);
      }
    };
    return dockerClient.logContainerCmd(module.getId())
        .withStdOut(true)
        .withStdErr(true)
        .withFollowStream(true)
        .withTail(100)
        .exec(callback);
  }
}
