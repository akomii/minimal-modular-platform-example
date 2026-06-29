# config-demo

A minimal module that demonstrates **runtime configuration**. It declares one setting of every
type in its manifest, and on start its container runs [`print-config.sh`](print-config.sh) once —
printing the values core injected as env vars — then **exits on its own** (a one-shot run).

## What it shows

| Key | Type | Default |
|-----|------|---------|
| `GREETING` | string | `Hello from config-demo` |
| `REPEAT` | number | `3` |
| `VERBOSE` | boolean | `true` |
| `API_TOKEN` | secret | `demo-secret` |

The one-shot behaviour comes from the manifest's `command` (`sh /print-config.sh`), which overrides
the image's default command; when the script returns, the container stops.

## Try it

1. Install **config-demo** on the Modules tab.
2. On the **Configuration** tab, edit its values and **Save**, then **Apply** (recreates the
   container with the new env).
3. **Start** it and watch this module's logs — it prints the config and stops itself.
