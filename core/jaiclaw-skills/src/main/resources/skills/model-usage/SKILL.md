---
name: model-usage
description: Use CodexBar CLI local cost logs to summarize per-model usage for Codex or Claude.
requiredBins: [codexbar]
platforms: [darwin]
version: 1.0.0
---

# Model usage

## Overview

Get per-model usage cost from CodexBar's local cost logs. Supports "current model" (most recent daily entry) or "all models" summaries for Codex or Claude.

## Quick start

```bash
codexbar cost --format json --provider codex
codexbar cost --format json --provider claude
```

## Current model logic

- Uses the most recent daily row with `modelBreakdowns`.
- Picks the model with the highest cost in that row.
- Falls back to the last entry in `modelsUsed` when breakdowns are missing.

## Notes

- Values are cost-only per model; tokens are not split by model in CodexBar output.
- macOS-only (CodexBar is a brew cask).
