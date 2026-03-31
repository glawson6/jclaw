---
name: himalaya
description: CLI to manage emails via IMAP/SMTP. Use himalaya to list, read, write, reply, forward, search, and organize emails from the terminal.
requiredBins: [himalaya]
version: 1.0.0
---

# Himalaya Email CLI

Himalaya is a CLI email client that lets you manage emails from the terminal using IMAP, SMTP, Notmuch, or Sendmail backends.

## Prerequisites

1. Himalaya CLI installed (`himalaya --version` to verify)
2. A configuration file at `~/.config/himalaya/config.toml`
3. IMAP/SMTP credentials configured (password stored securely)

## Configuration Setup

Run the interactive wizard to set up an account:

```bash
himalaya account configure
```

## Common Operations

### List Folders

```bash
himalaya folder list
```

### List Emails

```bash
himalaya envelope list
himalaya envelope list --folder "Sent"
himalaya envelope list --page 1 --page-size 20
```

### Search Emails

```bash
himalaya envelope list from john@example.com subject meeting
```

### Read an Email

```bash
himalaya message read 42
```

### Reply to an Email

```bash
himalaya message reply 42
himalaya message reply 42 --all
```

### Forward an Email

```bash
himalaya message forward 42
```

### Write a New Email

```bash
himalaya message write
```

Send directly using template:

```bash
cat << 'EOF' | himalaya template send
From: you@example.com
To: recipient@example.com
Subject: Test Message

Hello from Himalaya!
EOF
```

### Move/Copy Emails

```bash
himalaya message move 42 "Archive"
himalaya message copy 42 "Important"
```

### Delete an Email

```bash
himalaya message delete 42
```

### Manage Flags

```bash
himalaya flag add 42 --flag seen
himalaya flag remove 42 --flag seen
```

## Multiple Accounts

```bash
himalaya account list
himalaya --account work envelope list
```

## Attachments

```bash
himalaya attachment download 42
himalaya attachment download 42 --dir ~/Downloads
```

## Output Formats

```bash
himalaya envelope list --output json
himalaya envelope list --output plain
```

## Tips

- Use `himalaya --help` or `himalaya <command> --help` for detailed usage.
- Message IDs are relative to the current folder; re-list after folder changes.
- Store passwords securely using `pass`, system keyring, or a command that outputs the password.
