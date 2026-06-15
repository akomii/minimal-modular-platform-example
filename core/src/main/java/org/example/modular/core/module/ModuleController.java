package org.example.modular.core.module;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST API for the management UI to list modules, drive their lifecycle (install, authorize, start, stop, remove) and stream their logs.
 */
@RestController
@RequestMapping("/api/modules")
public class ModuleController {

  private final ModuleService service;

  public ModuleController(ModuleService service) {
    this.service = service;
  }

  @GetMapping
  public List<ModuleDTO> list() {
    return service.list();
  }

  @PostMapping("/{id}/install")
  public ModuleDTO install(@PathVariable String id) {
    return service.install(id);
  }

  @PostMapping("/{id}/authorize")
  public ModuleDTO authorize(@PathVariable String id) {
    return service.authorize(id);
  }

  @PostMapping("/{id}/start")
  public ModuleDTO start(@PathVariable String id) {
    return service.start(id);
  }

  @PostMapping("/{id}/stop")
  public ModuleDTO stop(@PathVariable String id) {
    return service.stop(id);
  }

  @DeleteMapping("/{id}")
  public ModuleDTO remove(@PathVariable String id, @RequestParam(defaultValue = "false") boolean purge) {
    return service.remove(id, purge);
  }

  /**
   * Opens an SSE stream of the module's container logs, closing the underlying follow when the client disconnects.
   */
  @GetMapping(value = "/{id}/logs/stream", produces = "text/event-stream")
  public SseEmitter streamLogs(@PathVariable String id) {
    SseEmitter emitter = new SseEmitter(0L);
    Closeable handle = service.streamLogs(id, new SseEmitterLogSink(emitter));
    emitter.onCompletion(() -> closeQuietly(handle));
    emitter.onTimeout(() -> closeQuietly(handle));
    return emitter;
  }

  private static void closeQuietly(Closeable closeable) {
    try {
      closeable.close();
    } catch (IOException ignored) {
      // stop the docker log follow on client disconnect
    }
  }
}
