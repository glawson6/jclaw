# JaiClaw Maven Plugin

Maven plugin that statically analyzes a JaiClaw project's `application.yml` and estimates per-request input token overhead. Use it to enforce token budgets in CI/CD pipelines without a running application.

## Quick Start

Add to your JaiClaw application's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.jaiclaw</groupId>
            <artifactId>jaiclaw-maven-plugin</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <goals><goal>analyze</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

This runs during the `verify` phase and prints a token usage report.

## Configuration

### Enforce a Token Budget

Fail the build if estimated tokens exceed a threshold:

```xml
<plugin>
    <groupId>io.jaiclaw</groupId>
    <artifactId>jaiclaw-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals><goal>analyze</goal></goals>
        </execution>
    </executions>
    <configuration>
        <threshold>5000</threshold>
    </configuration>
</plugin>
```

### Fail on Warnings

Catch misconfigurations like missing `allow-bundled` (which defaults to loading all ~59 skills):

```xml
<configuration>
    <threshold>5000</threshold>
    <failOnWarning>true</failOnWarning>
</configuration>
```

### Skip Analysis

Disable the plugin for a specific module:

```xml
<configuration>
    <skip>true</skip>
</configuration>
```

Or via command line: `-Djaiclaw.analyze.skip=true`

## Parameters

| Parameter | Type | Default | Property | Description |
|-----------|------|---------|----------|-------------|
| `threshold` | int | `0` | `jaiclaw.analyze.threshold` | Fail if tokens exceed this (0 = disabled) |
| `failOnWarning` | boolean | `false` | `jaiclaw.analyze.failOnWarning` | Fail if warnings are present |
| `skip` | boolean | `false` | `jaiclaw.analyze.skip` | Skip analysis entirely |

## Command-Line Usage

Run on-demand against any module without adding the plugin to its POM:

```bash
# Analyze a specific module
./mvnw io.jaiclaw:jaiclaw-maven-plugin:analyze -pl :my-agent-app

# With threshold
./mvnw io.jaiclaw:jaiclaw-maven-plugin:analyze -pl :my-agent-app -Djaiclaw.analyze.threshold=5000

# Fail on warnings
./mvnw io.jaiclaw:jaiclaw-maven-plugin:analyze -pl :my-agent-app -Djaiclaw.analyze.failOnWarning=true
```

## Sample Output

```
Prompt Token Analysis: my-agent-app
====================================

Component                  Tokens    Details
---------                  ------    -------
System prompt                  208    (configured)
Skills (2 loaded)            4,312    allow-bundled: [coding, web-research]
Built-in tools (5)           1,560    profile: coding
                             ------
Estimated total              6,080    (excludes conversation history)

Warnings:
  (none)
```

## What It Analyzes

- **System prompt** — identity name/description, date context, inline content, and external prompt files
- **Skills** — resolved from `jaiclaw.skills.allow-bundled` configuration
- **Built-in tools** — filtered by the agent's tool profile (`none`, `minimal`, `coding`, `messaging`, `full`)
- **Project tools** — custom `ToolCallback` implementations and plugin tools detected via source scanning

## Multi-Module Builds

The plugin gracefully skips modules that don't have `src/main/resources/application.yml`, so it's safe to add to a parent POM in a multi-module project:

```xml
<!-- parent pom.xml -->
<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>io.jaiclaw</groupId>
                <artifactId>jaiclaw-maven-plugin</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <configuration>
                    <threshold>8000</threshold>
                    <failOnWarning>true</failOnWarning>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

Then in each agent module that should be analyzed:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.jaiclaw</groupId>
            <artifactId>jaiclaw-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals><goal>analyze</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
