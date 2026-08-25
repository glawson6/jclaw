# JaiClaw 1.2.0 Release Notes

**Release Date:** _pending_
**Distribution:** Maven Central + TapTech Nexus (`tooling.taptech.net`)

> 1.2.0 is the **security-hardening release.** It closes the CRITICAL and HIGH findings from the 2026-08-24 security scan (`security-report-2026-08-24.md`) — MCP cross-tenant escapes, PDF path-traversal, missing method-level authorization, and CI/CD CVE-scan wiring.

## Highlights

- **MCP cross-tenant defense** — `CalendarMcpToolProvider`, `PipelineMcpToolProvider`, and `KanbanMcpToolProvider` now reject caller-supplied `tenantId` arguments that don't match the caller's `TenantContext`. Shared helper `io.jaiclaw.core.tenant.TenantArgResolver` centralizes the check; `TenantContextHolder.withTenant(...)` provides a save-restore wrap that no longer clobbers an outer caller's context.
- **PDF tool workspace boundary** — `PdfReadFieldsTool` and `PdfFillFormTool` in `jaiclaw-documents` now route every path through `WorkspaceBoundary.resolve(...)` (mirrors `FileEditTool`). Absolute paths escaping the workspace are rejected with a `ToolResult.Error`. Opt out with `jaiclaw.tools.documents.workspace-boundary=false`.
- **Method-level authorization is now honest** — `@EnableMethodSecurity` is registered on all three security modes (`api-key`, `jwt`, `none`). Prior to this release the `@PreAuthorize` annotations on `PipelineDeploymentController` and `GdprController` were silent no-ops. New `PipelineStudioControllerAuthzSpec` is the first-of-its-kind regression guard proving the annotations actually fire.
- **Admin controller now gated** — `AdminController` picked up a class-level `@PreAuthorize("@adminAuthzExpressions.admin()")` backed by the new `AdminAuthzProperties` / `AdminAuthzExpressions` beans (`jaiclaw.gateway.admin.roles.admin`).
- **Pipeline Studio controller now gated** — every endpoint on `PipelineStudioController` picked up per-endpoint `@PreAuthorize` — `viewer()` for read paths, `author()` for mutating paths.
- **OWASP dependency-check wired for NVD** — the plugin now consumes an NVD API key via `-Dnvd.api.key=... / $NVD_API_KEY`. Without a key the plugin aborted on the first artifact; with one, the scan runs end-to-end.

## Breaking changes

### Method-security defaults changed

`@EnableMethodSecurity` is now on. Any `@PreAuthorize` annotation in downstream code (including third-party code that JaiClaw brings in) that was previously silently ignored will now be enforced.

**Backward-compat mitigation shipped with this release:** `Roles.DEFAULT` on `PipelineAuthoringProperties`, `GdprAuthzProperties`, and the new `AdminAuthzProperties` were flipped from concrete role names (e.g., `ROLE_PIPELINE_DEPLOYER`, `GDPR_OPERATOR`) to **blank strings**. The existing `hasAuthorityOrBlank(...)` helpers already treat blank as "allow any authenticated principal", so default deployments behave identically to before enabling method security.

Adopters who **want** role-based gating must set the property explicitly in `application.yml` AND ensure their auth layer grants a matching authority. Example:

```yaml
jaiclaw:
  pipeline:
    authoring:
      roles:
        deployer: ROLE_PIPELINE_DEPLOYER   # opt-in — requires the auth layer to grant this authority
  compliance:
    gdpr:
      roles:
        operator: GDPR_OPERATOR
  gateway:
    admin:
      roles:
        admin: JAICLAW_ADMIN
```

**Caution — api-key deployments:** `ApiKeyAuthenticationFilter` creates the principal with **zero granted authorities**. If you turn on any non-blank role config without also modifying the filter (or providing a custom `ApiKeyProvider` that grants authorities), every api-key call to the annotated endpoint will return `403 Forbidden`.

### Naming convention for admin role

`AdminAuthzProperties.Roles.admin` uses the `JAICLAW_ADMIN` naming style (matching `GDPR_OPERATOR`, no `ROLE_` prefix). This differs from the `PipelineAuthoringProperties.Roles.*` defaults (which used the `ROLE_PIPELINE_*` prefix pre-1.2.0). Both work — `hasAuthorityOrBlank` does an exact string match, no `ROLE_` magic. Adopters pick whichever convention suits their auth layer.

## New APIs

- **`io.jaiclaw.core.tenant.TenantArgResolver`** — static helper `resolveOrValidate(TenantContext caller, Map<String,Object> args, String argName, String defaultTenant)`. Returns the caller tenant on match; throws `CrossTenantAccessException` on mismatch. Use in any MCP tool provider that accepts a caller-supplied `tenantId` arg.
- **`io.jaiclaw.core.tenant.CrossTenantAccessException`** — thrown by `TenantArgResolver` when caller and requested tenants diverge.
- **`io.jaiclaw.core.tenant.TenantContextHolder.withTenant(TenantContext, Supplier)`** and `.withTenant(TenantContext, Runnable)` — save/restore-safe wrappers. Prefer over manual `set/clear` pairs — the naive pattern clobbers an outer caller's tenant on `finally`.
- **`io.jaiclaw.gateway.admin.AdminAuthzProperties`** / **`AdminAuthzExpressions`** — role config + SpEL holder for the `AdminController`. Bean name: `adminAuthzExpressions`.

## Security fixes

Closes findings from `security-report-2026-08-24.md`:

- **SEV-001** (HIGH) — OWASP `dependency-check-maven` now consumes `-Dnvd.api.key=...`; scan runs end-to-end.
- **SEV-002** (CRITICAL) — `CalendarMcpToolProvider` cross-tenant write via `tenantId` arg rejected.
- **SEV-003** (CRITICAL) — `PipelineMcpToolProvider` cross-tenant trigger via `tenantId` arg rejected.
- **SEV-004** (CRITICAL) — `PdfReadFieldsTool` / `PdfFillFormTool` reject paths escaping the workspace.
- **SEV-005** (HIGH) — `MessagingMcpToolProvider` switched from naive `set/clear` to save-restore `withTenant`; regression tests added. `handleBroadcastMessage` recipient tenant validation deferred as SEV-005b — tracked as a `@PendingFeature` in `MessagingMcpToolProviderSpec`.
- **SEV-006** (HIGH) — `AdminController` gated with `@PreAuthorize("@adminAuthzExpressions.admin()")`.
- **SEV-007** (HIGH) — `PipelineStudioController` gated per-endpoint with `viewer()` / `author()` roles.
- **SEV-008** (HIGH) — `KanbanMcpToolProvider` now wraps `execute()` with `TenantContextHolder.withTenant(...)`.

## Deferred (backlog)

- **SEV-005b** — `handleBroadcastMessage` recipient tenant validation. Needs a design pass on whether `ChannelRegistry` tracks per-peer tenant.
- **SEV-009 through SEV-019** — Medium/Low/Info findings from the same report.

## Version alignment

_No dependency version bumps in 1.2.0 — this release is scoped to the security fixes above._
