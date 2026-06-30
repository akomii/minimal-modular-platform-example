package org.example.modular.core.runtime;

import java.io.Closeable;
import java.util.Map;
import org.example.modular.core.module.ModuleDefinition;

/**
 * Manages a module's container lifecycle — install, start, stop, remove, status and log streaming. The platform talks only to this abstraction; {@link DockerJavaRuntime} is the Docker-backed
 * implementation, and targeting a different container engine means swapping the implementation.
 */
public interface ModuleRuntime {

  /**
   * Reports the module container's current state, returning {@link ModuleStatus#NOT_CREATED} when no container exists.
   */
  ModuleStatus status(ModuleDefinition module);

  /**
   * Pulls the module's image and creates its container without starting it, layering {@code extraEnv} (platform-injected values such as generated secrets) over the manifest env; throws if the module
   * is already installed.
   */
  void install(ModuleDefinition module, Map<String, String> extraEnv);

  /**
   * Recreates the module's container with new {@code extraEnv} from the already-pulled image (no image pull), leaving it stopped; throws if the module is not installed. Used to apply changed
   * configuration to an installed module.
   */
  void reconfigure(ModuleDefinition module, Map<String, String> extraEnv);

  /**
   * Starts the module's container; a no-op if it is already running, and throws if it is not installed.
   */
  void start(ModuleDefinition module);

  /**
   * Stops the module's container; a no-op if it is not running, and throws if it is not installed.
   */
  void stop(ModuleDefinition module);

  /**
   * Removes the module's container; throws if it is not installed or still running (it must be stopped first).
   */
  void remove(ModuleDefinition module);

  /**
   * Streams the container's logs (tailing recent lines, then following new output) to the sink, returning a {@link Closeable} that stops the stream; throws if the module is not installed.
   */
  Closeable streamLogs(ModuleDefinition module, LogSink sink);
}
