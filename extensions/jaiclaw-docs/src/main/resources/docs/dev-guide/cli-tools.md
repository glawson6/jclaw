# JaiClaw CLI Tool Modules Reference

[Back to Developer Guide](../JAICLAW-DEVELOPER-GUIDE.md)

---

## Table of Contents

1. [jaiclaw-perplexity](#jaiclaw-perplexity)
2. [jaiclaw-rest-cli-architect](#jaiclaw-rest-cli-architect)
3. [jaiclaw-skill-creator](#jaiclaw-skill-creator)
4. [jaiclaw-prompt-analyzer](#jaiclaw-prompt-analyzer)
5. [jaiclaw-maven-plugin](#jaiclaw-maven-plugin)
6. [Dual-Mode Build Pattern](#dual-mode-build-pattern)

---

## jaiclaw-perplexity

**Purpose**: Perplexity AI integration providing search with citations, raw web search, and deep multi-step research via Sonar and Agent APIs.

**Package**: `io.jaiclaw.tools.perplexity`

### Class Reference

| Class | Type | Description |
|---|---|---|
| PerplexityApplication | class | Standalone Spring Boot entry point |
| PerplexityProperties | record | Configuration properties for Perplexity API |
| PerplexityClient | class | HTTP client for all Perplexity API endpoints |
| PerplexityCommands | class | Spring Shell commands (ask, search, research, chat, models, config) |
| PerplexityAutoConfiguration | class | Auto-configuration registering 3 tools |
| PerplexityApiException | class | Runtime exception for API errors |
| TerminalImageRenderer | class | Renders images in terminal (iTerm2, Chafa, text fallback); built-in SSRF protection |
| TerminalImageRenderer.Protocol | enum | Image rendering: ITERM2, CHAFA, TEXT_ONLY |
| SonarRequest | record | Request payload for Sonar chat/completions API |
| SonarRequest.Builder | class | Builder for SonarRequest |
| SonarResponse | record | Response from Sonar with choices, citations, search results |
| AgentRequest | record | Request for Perplexity Agent API with preset and messages |
| AgentRequest.Builder | class | Builder for AgentRequest |
| AgentResponse | record | Response from Agent API with content, citations, steps |
| AgentStep | record | Single research step in agent response |
| SearchApiRequest | record | Request for raw web search API |
| SearchApiResponse | record | Response from raw web search |
| SearchResult | record | Single search result with title, URL, snippet |
| Choice | record | Message choice in Sonar response |
| Citation | record | Source citation with URL, title, snippet |
| Message | record | Chat message with role and content |
| Usage | record | Token usage metrics |

### Class Relationships

```
PerplexityApplication (standalone entry)
  │
  ├── PerplexityCommands (@ShellComponent)
  │     └── PerplexityClient (HTTP client)
  │           ├── SonarRequest/SonarResponse
  │           ├── AgentRequest/AgentResponse
  │           └── SearchApiRequest/SearchApiResponse
  │
  └── PerplexityAutoConfiguration (embeddable mode)
        ├── PerplexitySearchTool ────┐
        ├── PerplexityWebSearchTool ─┤── inner classes, impl ToolCallback
        └── PerplexityResearchTool ──┘
              └── PerplexityClient
```

### Shell Commands

| Command | Alias | Description |
|---|---|---|
| `pplx ask` | `pplx-ask` | Ask with Sonar (citations + synthesis) |
| `pplx search` | `pplx-search` | Raw web search (no LLM synthesis) |
| `pplx research` | `pplx-research` | Deep multi-step research via Agent API |
| `pplx chat` | `pplx-chat` | Interactive chat session |
| `pplx models` | `pplx-models` | List available Sonar models |
| `pplx config` | `pplx-config` | Show current configuration |

### Registered Tools (Embeddable)

| Tool | API | Description |
|---|---|---|
| `perplexity-search` | Sonar | Search with citations and synthesis |
| `perplexity-web-search` | Web Search | Raw results without LLM |
| `perplexity-research` | Agent | Deep multi-step research |

---

## jaiclaw-rest-cli-architect

**Purpose**: Scaffolds Spring Shell CLI projects from OpenAPI/Swagger specifications or JSON project specs, generating complete Maven projects with API clients and shell commands.

**Package**: `io.jaiclaw.tools.restcli`

### Class Reference

| Class | Type | Description |
|---|---|---|
| JRestCliApplication | class | Standalone Spring Boot entry point |
| CliArchitectAutoConfiguration | class | Auto-configuration registering 4 tools |
| JRestCliCommands | class | Spring Shell commands (scaffold, validate, from-openapi, interactive) |
| OpenApiParser | class | Parses OpenAPI 3.x and Swagger 2.x specs |
| OpenApiParser.ParseResult | record | Parse result with title, baseUrl, endpoints, auth |
| ProjectSpec | record | Full project specification |
| ProjectSpec.ApiSpec | record | API metadata with title, baseUrl, spec path |
| ProjectGenerator | class | Generates project files from validated spec |
| SpecValidator | class | Validates ProjectSpec and reports issues |
| AuthConfig | record | Authentication config (none/header/basic/oauth2) |
| EndpointSpec | record | Single API endpoint spec |
| EndpointSpec.ParamSpec | record | Parameter spec with name, type, location |
| ProjectMode | enum | JAICLAW_SUBMODULE, EXTERNAL_SUBMODULE, STANDALONE, JBANG |
| ConfigTemplates | class | Templates for application.yml, .env.example, test stubs |
| JavaTemplates | class | Templates for generated Java source |
| PomTemplates | class | Templates for pom.xml variants |

### Class Relationships

```
JRestCliApplication (standalone entry)
  │
  ├── JRestCliCommands (@ShellComponent)
  │     ├── OpenApiParser ──→ OpenApiParser.ParseResult
  │     ├── SpecValidator ──→ ProjectSpec
  │     └── ProjectGenerator
  │           ├── JavaTemplates
  │           ├── PomTemplates
  │           └── ConfigTemplates
  │
  └── CliArchitectAutoConfiguration (embeddable mode)
        ├── ScaffoldProjectTool ─────┐
        ├── ParseOpenApiTool ────────┤── inner classes, impl ToolCallback
        ├── ValidateSpecTool ────────┤
        └── FromOpenApiTool ─────────┘
```

### Shell Commands

| Command | Alias | Description |
|---|---|---|
| `jrestcli scaffold` | `jrestcli-scaffold` | Generate project from JSON spec |
| `jrestcli validate` | `jrestcli-validate` | Validate project spec |
| `jrestcli from-openapi` | `jrestcli-from-openapi` | Generate spec from OpenAPI URL/file |
| `jrestcli interactive` | `jrestcli-interactive` | Interactive project builder |

### Project Modes

| Mode | Description |
|---|---|
| `JAICLAW_SUBMODULE` | Module inside JaiClaw's `tools/` directory |
| `EXTERNAL_SUBMODULE` | Module in external Maven project |
| `STANDALONE` | Independent Spring Boot project |
| `JBANG` | Single-file JBang script |

---

## jaiclaw-skill-creator

**Purpose**: Interactive CLI for creating JaiClaw skill definitions (SKILL.md files) with YAML frontmatter.

**Package**: `io.jaiclaw.tools.skillcreator`

### Class Reference

| Class | Type | Description |
|---|---|---|
| SkillCreatorApplication | class | Standalone Spring Boot entry point |
| SkillCreatorCommands | class | Spring Shell command for interactive skill creation |
| SkillSpec | record | Parsed YAML skill spec (name, description, purpose) |

### Class Relationships

```
SkillCreatorApplication (standalone entry)
  │
  └── SkillCreatorCommands (@ShellComponent)
        │
        └── SkillSpec (parsed user input)
              │
              └── writes → SKILL.md (YAML frontmatter + markdown body)
```

### Shell Commands

| Command | Description |
|---|---|
| `skill create` | Interactive wizard for creating a new SKILL.md |

---

## jaiclaw-prompt-analyzer

**Purpose**: Estimates input token usage for a JaiClaw project by scanning its `application.yml`, resolving configured skills and tools, and producing a detailed token breakdown. Catches skill misconfiguration (e.g., loading all 59 bundled skills) before burning API credits.

**Package**: `io.jaiclaw.promptanalyzer`

### Class Reference

| Class | Type | Description |
|---|---|---|
| PromptAnalyzerApplication | class | Standalone Spring Boot entry point |
| ProjectScanner | class | Core analysis engine — parses YAML, resolves skills/tools, scans source |
| AnalysisReport | record | Token usage report with per-component breakdown and warnings |
| PromptAnalyzerCommands | class | Spring Shell commands (prompt-analyze, prompt-check) |
| PromptAnalyzerAutoConfiguration | class | Auto-configuration registering `prompt_analyze` tool |

### Class Relationships

```
PromptAnalyzerApplication (standalone entry)
  │
  ├── PromptAnalyzerCommands (@ShellComponent)
  │     └── ProjectScanner
  │           ├── SkillLoader (resolves bundled skills)
  │           ├── BuiltinTools.all() (filtered by tool profile)
  │           └── Source scanner (detects ToolDefinition in .java files)
  │                 └── AnalysisReport (output record)
  │
  └── PromptAnalyzerAutoConfiguration (embeddable mode)
        └── PromptAnalyzeTool ── inner class, impl ToolCallback
              └── ProjectScanner
```

### Shell Commands

| Command | Alias | Description |
|---|---|---|
| `prompt analyze` | `prompt-analyze` | Full token analysis report for a JaiClaw project |
| `prompt check` | `prompt-check` | CI pass/fail check against a token threshold |

### Registered Tools (Embeddable)

| Tool | Description |
|---|---|
| `prompt_analyze` | Analyze a project directory and return token usage report |

### How It Works

1. **Parses `application.yml`** — Finds `src/main/resources/application.yml` under the target project. Extracts `jaiclaw.skills.allow-bundled`, `jaiclaw.agent.agents.{id}.tools.profile`, identity name/description, and system prompt configuration. Supports both `jaiclaw:` and `jclaw:` config prefixes.

2. **Simulates system prompt** — Mirrors `SystemPromptBuilder.build()` output: `"You are {name}, {description}.\n\nToday's date is {date}.\n\n"` plus any inline `system-prompt.content`, `system-prompt.source`, or `system-prompt-path` file content.

3. **Resolves skills** — Uses `SkillLoader.loadConfigured(allowBundled, null)` to load the same skills the runtime would. Sums `content().length()` for token estimation.

4. **Resolves built-in tools** — Calls `BuiltinTools.all()` filtered by the agent's tool profile (e.g., `none`=0, `minimal`=1, `coding`=5, `full`=6 tools). Measures `name + description + inputSchema` plus ~80 chars structural overhead per tool.

5. **Scans source for custom tools** — Walks `src/main/java/` for `new ToolDefinition(` constructor calls, extracts string literal lengths, and estimates token contribution.

6. **Token estimation** — `(charCount + 2) / 4` (same formula as `LlmTraceLogger`).

### Usage Examples

```bash
# Build standalone
./mvnw package -pl :jaiclaw-prompt-analyzer -Pstandalone -DskipTests

# Analyze a project
java -jar tools/jaiclaw-prompt-analyzer/target/jaiclaw-prompt-analyzer-*.jar \
  prompt-analyze --path /path/to/my-app

# CI check (fail if > 5000 tokens)
java -jar tools/jaiclaw-prompt-analyzer/target/jaiclaw-prompt-analyzer-*.jar \
  prompt-check --path /path/to/my-app --threshold 5000
```

### Sample Output

```
Prompt Token Analysis: travel-planner
======================================

Component                  Tokens    Details
---------                  ------    -------
System prompt                 148    (configured)
Skills (0 loaded)               0    allow-bundled: []
Built-in tools (6)          1,872    profile: full
Project tools (4)             205    4 tools, ~205 tokens from source scan
                           ------
Estimated total             2,225    (excludes conversation history)

Warnings:
  (none)
```

---

## jaiclaw-maven-plugin

**Purpose**: Maven plugin that runs the same `ProjectScanner` analysis as `jaiclaw-prompt-analyzer` during the Maven build lifecycle. Enforces token budgets in CI/CD pipelines by failing the build if a threshold is exceeded or warnings are present.

**Package**: `io.jaiclaw.maven`

### Class Reference

| Class | Type | Description |
|---|---|---|
| AnalyzeMojo | class | `jaiclaw:analyze` Mojo — runs `ProjectScanner.analyze()` and checks thresholds |

### Class Relationships

```
AnalyzeMojo (@Mojo name="analyze", phase=VERIFY)
  └── ProjectScanner (from jaiclaw-prompt-analyzer)
        ├── SkillLoader (resolves bundled skills)
        ├── BuiltinTools.all() (filtered by tool profile)
        └── Source scanner (detects ToolDefinition in .java files)
              └── AnalysisReport → printed to Maven log
```

### Mojo Parameters

| Parameter | Type | Default | Property | Description |
|---|---|---|---|---|
| `baseDir` | File | `${project.basedir}` | *(readonly)* | Project directory to analyze |
| `threshold` | int | `0` | `jaiclaw.analyze.threshold` | Fail if tokens exceed this (0=disabled) |
| `skip` | boolean | `false` | `jaiclaw.analyze.skip` | Skip analysis entirely |
| `failOnWarning` | boolean | `false` | `jaiclaw.analyze.failOnWarning` | Fail if warnings present |

### POM Configuration

```xml
<plugin>
    <groupId>io.jaiclaw</groupId>
    <artifactId>jaiclaw-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <goals><goal>analyze</goal></goals>
        </execution>
    </executions>
    <configuration>
        <threshold>5000</threshold>
        <failOnWarning>true</failOnWarning>
    </configuration>
</plugin>
```

### Command-Line Usage

```bash
# On-demand (no POM changes needed — fully qualified plugin coordinates)
./mvnw io.jaiclaw:jaiclaw-maven-plugin:analyze -pl :my-agent-app

# With threshold
./mvnw io.jaiclaw:jaiclaw-maven-plugin:analyze -pl :my-agent-app -Djaiclaw.analyze.threshold=5000

# During lifecycle (requires plugin in POM)
./mvnw verify -pl :my-agent-app -DskipTests
```

### Behavior

- Runs during the `verify` phase (after `package`, before `install`)
- **Graceful skip**: Modules without `src/main/resources/application.yml` are silently skipped — safe for multi-module parent POMs
- **Tool profile filtering**: Respects the agent's configured tool profile (`none`, `minimal`, `coding`, `messaging`, `full`)
- All JaiClaw examples include this plugin by default

### Multi-Module Parent POM Pattern

```xml
<!-- parent pom.xml — pluginManagement for shared config -->
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>io.jaiclaw</groupId>
            <artifactId>jaiclaw-maven-plugin</artifactId>
            <version>${project.version}</version>
            <configuration>
                <threshold>8000</threshold>
                <failOnWarning>true</failOnWarning>
            </configuration>
        </plugin>
    </plugins>
</pluginManagement>
```

Then in each agent module:

```xml
<plugin>
    <groupId>io.jaiclaw</groupId>
    <artifactId>jaiclaw-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>analyze</goal></goals>
        </execution>
    </executions>
</plugin>
```

---

## Dual-Mode Build Pattern

All CLI tool modules support two build modes:

| Mode | Profile | Output | Use Case |
|---|---|---|---|
| Library | (default) | Regular JAR | Dependency in other modules (auto-configuration registers tools) |
| Standalone | `-Pstandalone` | Executable fat JAR | Direct CLI usage |

### Build Commands

```bash
# Build as library (default — tools register into ToolRegistry)
./mvnw package -pl :jaiclaw-perplexity -DskipTests

# Build as standalone CLI
./mvnw package -pl :jaiclaw-perplexity -Pstandalone -DskipTests

# Run standalone
java -jar tools/jaiclaw-perplexity/target/jaiclaw-perplexity-*.jar pplx-search "query"
```

### Key Rules

1. **Do NOT add `spring-ai-starter-model-anthropic`** to standalone profile unless the module uses Spring AI directly
2. **Always add hyphenated CLI aliases** for multi-word commands:
   ```java
   @ShellMethod(key = {"pplx search", "pplx-search"})
   ```
3. **Set `spring.main.web-application-type: none`** in `application.yml`
4. **Embeddable mode**: When on classpath as a library, the `*AutoConfiguration` class registers tools into the gateway's `ToolRegistry` — no shell commands, just tools

### Auto-Configuration Pattern

Each CLI tool module includes an auto-configuration class that conditionally registers its tools:

```java
@AutoConfiguration
@ConditionalOnBean(ToolRegistry.class)
public class PerplexityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PerplexityToolsRegistrar.class)
    public PerplexityToolsRegistrar perplexityTools(ToolRegistry registry, ...) {
        // Register PerplexitySearchTool, PerplexityWebSearchTool, PerplexityResearchTool
        return new PerplexityToolsRegistrar();
    }
}
```

This means adding `jaiclaw-perplexity` as a dependency to the gateway automatically makes Perplexity tools available to all agents — no configuration needed.
