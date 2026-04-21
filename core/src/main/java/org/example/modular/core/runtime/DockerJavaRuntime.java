package org.example.modular.core.runtime;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DockerJavaRuntime implements ModuleRuntime {

  private final DockerClient dockerClient;

  public DockerJavaRuntime(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
  }

  @Override
  public ModuleStatus status(ModuleDefinition module) {
    try {
      String dockerState = dockerClient.inspectContainerCmd(module.getId())
          .exec()
          .getState()
          .getStatus();
      ModuleStatus status = ModuleStatus.fromDockerState(dockerState);
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

  private boolean isConnectionError(Exception exception) {
    Throwable current = exception;
    while (current != null) {
      if (current instanceof java.net.ConnectException ||
          (current.getMessage() != null && current.getMessage().contains("Connection refused"))) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  @Override
  public void install(ModuleDefinition module) {
    log.info("Installing module {}", module.getId());
    if (status(module) != ModuleStatus.NOT_CREATED) {
      log.warn("Cannot install {}: already installed", module.getId());
      throw new InvalidModuleStateException("Module is already installed: " + module.getId());
    }
    pullImage(module.getImage());
    createContainer(module);
    log.info("Successfully installed module {}", module.getId());
  }

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

  private void createContainer(ModuleDefinition module) {
    log.debug("Creating container for module {}", module.getId());
    HostConfig hostConfig = HostConfig.newHostConfig();
    List<ExposedPort> exposedPorts = new ArrayList<>();
    Ports portBindings = new Ports();
    if (module.getPorts() != null) {
      for (String mapping : module.getPorts()) {
        String[] parts = mapping.split(":");
        if (parts.length != 2) {
          log.error("Invalid port mapping {} for module {}", mapping, module.getId());
          throw new IllegalArgumentException("Invalid port mapping: " + mapping);
        }

        int hostPort = Integer.parseInt(parts[0]);
        int containerPort = Integer.parseInt(parts[1]);
        ExposedPort exposedPort = ExposedPort.tcp(containerPort);
        exposedPorts.add(exposedPort);
        portBindings.bind(exposedPort, Binding.bindPort(hostPort));
      }
    }
    hostConfig.withPortBindings(portBindings);
    CreateContainerResponse response = dockerClient.createContainerCmd(module.getImage())
        .withName(module.getId())
        .withExposedPorts(exposedPorts)
        .withHostConfig(hostConfig)
        .exec();
    if (response.getId() == null || response.getId().isBlank()) {
      log.error("Container creation returned empty ID for module {}", module.getId());
      throw new IllegalStateException("Container creation failed for module: " + module.getId());
    }
    log.debug("Container created with ID {}", response.getId());
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
}
