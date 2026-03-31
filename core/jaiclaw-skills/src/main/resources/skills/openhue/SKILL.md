---
name: openhue
description: Control Philips Hue lights and scenes via the OpenHue CLI.
requiredBins: [openhue]
version: 1.0.0
---

# OpenHue CLI

Use `openhue` to control Philips Hue lights and scenes via a Hue Bridge.

## When to Use

- "Turn on/off the lights"
- "Dim the living room lights"
- "Set a scene" or "movie mode"
- Controlling specific Hue rooms or zones
- Adjusting brightness, color, or color temperature

## When NOT to Use

- Non-Hue smart devices (other brands)
- HomeKit scenes or Shortcuts
- TV or entertainment system control
- Thermostat or HVAC

## Common Commands

### List Resources

```bash
openhue get light       # List all lights
openhue get room        # List all rooms
openhue get scene       # List all scenes
```

### Control Lights

```bash
openhue set light "Bedroom Lamp" --on
openhue set light "Bedroom Lamp" --off
openhue set light "Bedroom Lamp" --on --brightness 50
openhue set light "Bedroom Lamp" --on --temperature 300
openhue set light "Bedroom Lamp" --on --color red
openhue set light "Bedroom Lamp" --on --rgb "#FF5500"
```

### Control Rooms

```bash
openhue set room "Bedroom" --off
openhue set room "Bedroom" --on --brightness 30
```

### Scenes

```bash
openhue set scene "Relax" --room "Bedroom"
openhue set scene "Concentrate" --room "Office"
```

## Quick Presets

```bash
# Bedtime (dim warm)
openhue set room "Bedroom" --on --brightness 20 --temperature 450

# Work mode (bright cool)
openhue set room "Office" --on --brightness 100 --temperature 250

# Movie mode (dim)
openhue set room "Living Room" --on --brightness 10
```

## Notes

- Bridge must be on local network
- First run requires button press on Hue bridge to pair
- Colors only work on color-capable bulbs (not white-only)
