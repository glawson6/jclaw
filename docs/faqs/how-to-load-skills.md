# How to Load Skills

JClaw supports four skill loading scenarios, all controlled via `jclaw.skills.*` configuration properties.

## Configuration Properties

```yaml
jclaw:
  skills:
    # Which bundled skills to include. Default: ["*"] (all)
    # ["*"] = all bundled, [] = none, ["coding", "github"] = only those
    allow-bundled:
      - "*"

    # External directory for custom/override skills. Default: null (none)
    workspace-dir: /opt/jclaw/skills
```

## Scenarios

### 1. Additive — All bundled + your own

Use all bundled skills and add custom ones from a workspace directory.

```yaml
jclaw:
  skills:
    allow-bundled:
      - "*"
    workspace-dir: /opt/jclaw/skills
```

Any custom skills placed in `/opt/jclaw/skills/` are loaded alongside the bundled ones.

### 2. Replace — Only your own

Disable all bundled skills and load only custom ones.

```yaml
jclaw:
  skills:
    allow-bundled: []
    workspace-dir: /opt/jclaw/skills
```

### 3. Selective — Cherry-pick bundled + your own

Include only specific bundled skills, plus any custom ones.

```yaml
jclaw:
  skills:
    allow-bundled:
      - coding
      - conversation
      - web-research
    workspace-dir: /opt/jclaw/skills
```

### 4. Override — Replace a specific bundled skill

Provide a skill with the **same name** as a bundled skill in your workspace directory. The workspace version wins.

```yaml
jclaw:
  skills:
    allow-bundled:
      - "*"
    workspace-dir: /opt/jclaw/skills
```

Place a custom `coding/SKILL.md` in `/opt/jclaw/skills/` to override the bundled `coding` skill.

## Workspace Directory Structure

```
/opt/jclaw/skills/
├── my-custom-skill/
│   └── SKILL.md
├── coding/                  # overrides bundled "coding" skill
│   └── SKILL.md
└── another-skill/
    └── SKILL.md
```

## SKILL.md File Format

Each skill is a markdown file with YAML frontmatter:

```markdown
---
name: my-skill
description: Short description of what this skill does
version: 1.0.0
platforms: [darwin, linux]          # optional: restrict to platforms
requiredBins: [docker]             # optional: require binaries on PATH
alwaysInclude: false               # optional: always include in prompt
tenantIds: [tenant-a, tenant-b]    # optional: restrict to tenants
---
# My Skill

Instructions and content for the skill go here.
```

- **name**: Used for override matching — a workspace skill with the same name as a bundled skill replaces it
- **platforms** / **requiredBins**: Skills are filtered out if the platform doesn't match or required binaries aren't found
- **alwaysInclude**: When `true`, the skill is always included in the agent's system prompt regardless of relevance scoring

## Default Behavior

With no configuration (or `allow-bundled: ["*"]` and no `workspace-dir`), all bundled skills are loaded — matching the original behavior.
