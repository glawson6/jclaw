---
name: coding-agent
description: Delegate coding tasks to Codex, Claude Code, or Pi agents via background process. Use when building features, reviewing PRs, or refactoring large codebases.
requiredBins: [claude, codex]
version: 1.0.0
---

# Coding Agent (bash-first)

Use **bash** (with optional background mode) for all coding agent work.

## PTY Mode: Codex/Pi/OpenCode yes, Claude Code no

For **Codex, Pi, and OpenCode**, PTY is required (interactive terminal apps):

```bash
# Correct for Codex/Pi/OpenCode
bash pty:true command:"codex exec 'Your prompt'"
```

For **Claude Code** (`claude` CLI), use `--print --permission-mode bypassPermissions` instead:

```bash
# Correct for Claude Code (no PTY needed)
cd /path/to/project && claude --permission-mode bypassPermissions --print 'Your task'
```

## Quick Start: One-Shot Tasks

```bash
# Quick chat (Codex needs a git repo!)
SCRATCH=$(mktemp -d) && cd $SCRATCH && git init && codex exec "Your prompt here"
```

## The Pattern: workdir + background + pty

For longer tasks, use background mode with PTY:

```bash
# Start agent in target directory (with PTY!)
bash pty:true workdir:~/project background:true command:"codex exec --full-auto 'Build a snake game'"
# Returns sessionId for tracking

# Monitor progress
process action:log sessionId:XXX

# Check if done
process action:poll sessionId:XXX

# Kill if needed
process action:kill sessionId:XXX
```

## Codex CLI

**Model:** `gpt-5.2-codex` is the default

### Flags

| Flag | Effect |
|------|--------|
| `exec "prompt"` | One-shot execution, exits when done |
| `--full-auto` | Sandboxed but auto-approves in workspace |
| `--yolo` | NO sandbox, NO approvals (fastest, most dangerous) |

## Claude Code

```bash
# Foreground
bash workdir:~/project command:"claude --permission-mode bypassPermissions --print 'Your task'"

# Background
bash workdir:~/project background:true command:"claude --permission-mode bypassPermissions --print 'Your task'"
```

## Pi Coding Agent

```bash
bash pty:true workdir:~/project command:"pi 'Your task'"

# Non-interactive mode
bash pty:true command:"pi -p 'Summarize src/'"
```

## Parallel Issue Fixing with git worktrees

```bash
# Create worktrees for each issue
git worktree add -b fix/issue-78 /tmp/issue-78 main
git worktree add -b fix/issue-99 /tmp/issue-99 main

# Launch Codex in each (background + PTY!)
bash pty:true workdir:/tmp/issue-78 background:true command:"codex --yolo 'Fix issue #78'"
bash pty:true workdir:/tmp/issue-99 background:true command:"codex --yolo 'Fix issue #99'"

# Create PRs after fixes
cd /tmp/issue-78 && git push -u origin fix/issue-78
gh pr create --repo user/repo --head fix/issue-78 --title "fix: ..." --body "..."

# Cleanup
git worktree remove /tmp/issue-78
```

## Rules

1. Use the right execution mode per agent (PTY for Codex/Pi, --print for Claude)
2. Respect tool choice - if user asks for Codex, use Codex
3. Be patient - don't kill sessions because they're "slow"
4. Monitor with process:log - check progress without interfering
5. Parallel is OK - run many agents at once for batch work
