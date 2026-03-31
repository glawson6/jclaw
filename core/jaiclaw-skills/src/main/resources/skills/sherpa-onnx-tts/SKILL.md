---
name: sherpa-onnx-tts
description: Local text-to-speech via sherpa-onnx (offline, no cloud).
requiredEnv: [SHERPA_ONNX_RUNTIME_DIR, SHERPA_ONNX_MODEL_DIR]
platforms: [darwin, linux, win32]
version: 1.0.0
---

# sherpa-onnx-tts

Local TTS using the sherpa-onnx offline CLI.

## Install

1. Download the runtime for your OS
2. Download a voice model

Runtime downloads:

- macOS: `sherpa-onnx-v1.12.23-osx-universal2-shared.tar.bz2`
- Linux x64: `sherpa-onnx-v1.12.23-linux-x64-shared.tar.bz2`
- Windows x64: `sherpa-onnx-v1.12.23-win-x64-shared.tar.bz2`

From: https://github.com/k2-fsa/sherpa-onnx/releases

Voice model (English, Piper):

- `vits-piper-en_US-lessac-high.tar.bz2` from the `tts-models` release tag

Set the environment variables to point at the extracted directories:

- `SHERPA_ONNX_RUNTIME_DIR` - path to extracted runtime
- `SHERPA_ONNX_MODEL_DIR` - path to extracted model (e.g. `models/vits-piper-en_US-lessac-high`)

## Usage

```bash
sherpa-onnx-tts -o ./tts.wav "Hello from local TTS."
```

Notes:

- Pick a different model from the sherpa-onnx `tts-models` release if you want another voice.
- If the model dir has multiple `.onnx` files, set `SHERPA_ONNX_MODEL_FILE` or pass `--model-file`.
- You can also pass `--tokens-file` or `--data-dir` to override the defaults.
