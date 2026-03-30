package io.jaiclaw.core.auth;

/**
 * Constants shared across the auth profile system.
 */
public final class AuthProfileConstants {

    private AuthProfileConstants() {}

    // --- Store format ---
    public static final int AUTH_STORE_VERSION = 1;
    public static final String AUTH_PROFILE_FILENAME = "auth-profiles.json";
    public static final String LEGACY_AUTH_FILENAME = "auth.json";

    // --- External CLI profile IDs ---
    public static final String CLAUDE_CLI_PROFILE_ID = "anthropic:claude-cli";
    public static final String CODEX_CLI_PROFILE_ID = "openai-codex:codex-cli";
    public static final String QWEN_CLI_PROFILE_ID = "qwen-portal:qwen-cli";
    public static final String MINIMAX_CLI_PROFILE_ID = "minimax-portal:minimax-cli";

    // --- Sync timing ---
    /** In-process cache TTL for external CLI credential reads (15 minutes). */
    public static final long EXTERNAL_CLI_SYNC_TTL_MS = 15 * 60 * 1000L;
    /** Threshold: skip sync if existing credential expires more than 10 minutes from now. */
    public static final long EXTERNAL_CLI_NEAR_EXPIRY_MS = 10 * 60 * 1000L;

    // --- External CLI file paths (relative to user home) ---
    public static final String CLAUDE_CLI_CREDS_PATH = ".claude/.credentials.json";
    public static final String CODEX_CLI_AUTH_FILENAME = "auth.json";
    public static final String QWEN_CLI_CREDS_PATH = ".qwen/oauth_creds.json";
    public static final String MINIMAX_CLI_CREDS_PATH = ".minimax/oauth_creds.json";

    // --- macOS Keychain entries ---
    public static final String CLAUDE_KEYCHAIN_SERVICE = "Claude Code-credentials";
    public static final String CLAUDE_KEYCHAIN_ACCOUNT = "Claude Code";
    public static final String CODEX_KEYCHAIN_SERVICE = "Codex Auth";

    // --- File lock settings ---
    public static final int LOCK_MAX_RETRIES = 10;
    public static final long LOCK_BASE_DELAY_MS = 100;
    public static final long LOCK_MAX_DELAY_MS = 10_000;
    public static final long LOCK_STALE_TIMEOUT_MS = 30_000;

    // --- Default directory layout ---
    public static final String DEFAULT_STATE_DIR = ".jaiclaw";
    public static final String AGENTS_DIR = "agents";
    public static final String STATE_DIR_ENV = "JAICLAW_STATE_DIR";
    public static final String AGENT_DIR_ENV = "JAICLAW_AGENT_DIR";
    public static final String READ_ONLY_ENV = "JAICLAW_AUTH_STORE_READONLY";
}
