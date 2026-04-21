package org.example.modular.core.runtime;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import java.util.ArrayList;
import java.util.List;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.stereotype.Service;

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
      return ModuleStatus.fromDockerState(dockerState);
    } catch (NotFoundException exception) {
      return ModuleStatus.NOT_CREATED;
    } catch (Exception exception) {
      if (isConnectionError(exception)) {
        throw new DockerConnectionException("Docker daemon is unreachable", exception);
      }
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
    if (status(module) != ModuleStatus.NOT_CREATED) {
      throw new InvalidModuleStateException("Module is already installed: " + module.getId());
    }
    createContainer(module);
  }

  private void createContainer(ModuleDefinition module) {
    HostConfig hostConfig = HostConfig.newHostConfig();
    List<ExposedPort> exposedPorts = new ArrayList<>();
    Ports portBindings = new Ports();
    if (module.getPorts() != null) {
      for (String mapping : module.getPorts()) {
        String[] parts = mapping.split(":");
        if (parts.length != 2) {
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
      throw new IllegalStateException("Container creation failed for module: " + module.getId());
    }
  }

  @Override
  public void start(ModuleDefinition module) {
    ModuleStatus currentStatus = status(module);
    if (currentStatus == ModuleStatus.NOT_CREATED) {
      throw new InvalidModuleStateException("Module is not installed: " + module.getId());
    }
    if (currentStatus == ModuleStatus.RUNNING) {
      return;
    }
    dockerClient.startContainerCmd(module.getId()).exec();
  }

  @Override
  public void stop(ModuleDefinition module) {
    ModuleStatus currentStatus = status(module);
    if (currentStatus == ModuleStatus.NOT_CREATED) {
      throw new InvalidModuleStateException("Module is not installed: " + module.getId());
    }
    if (currentStatus != ModuleStatus.RUNNING) {
      return;
    }
    dockerClient.stopContainerCmd(module.getId()).exec();
  }

  @Override
  public void remove(ModuleDefinition module) {
    ModuleStatus currentStatus = status(module);
    if (currentStatus == ModuleStatus.NOT_CREATED) {
      throw new InvalidModuleStateException("Module is not installed: " + module.getId());
    }
    if (currentStatus == ModuleStatus.RUNNING) {
      throw new InvalidModuleStateException("Module is currently running and must be stopped before removal: " + module.getId());
    }
    dockerClient.removeContainerCmd(module.getId()).exec();
  }
}
