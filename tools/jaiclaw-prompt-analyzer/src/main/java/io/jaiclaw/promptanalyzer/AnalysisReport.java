package io.jaiclaw.promptanalyzer;

import java.util.List;

/**
 * Report produced by {@link ProjectScanner} estimating the input token usage
 * for a JaiClaw project configuration.
 */
public record AnalysisReport(
        String projectName,
        int systemPromptTokens,
        int skillsTokens,
        int skillCount,
        List<String> skillNames,
        int builtinToolsTokens,
        int builtinToolCount,
        int pluginToolCount,
        int customToolCount,
        int projectToolsTokens,
        int estimatedTotalTokens,
        String toolProfile,
        List<String> warnings
) {

    /**
     * Formats the report as a human-readable multi-line string.
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        String title = "Prompt Token Analysis: " + projectName;
        sb.append(title).append('\n');
        sb.append("=".repeat(title.length())).append('\n');
        sb.append('\n');

        sb.append(String.format("%-27s%-10s%s%n", "Component", "Tokens", "Details"));
        sb.append(String.format("%-27s%-10s%s%n", "---------", "------", "-------"));

        // System prompt
        sb.append(String.format("%-27s%,7d    %s%n", "System prompt", systemPromptTokens, "(configured)"));

        // Skills
        String skillsDetail;
        if (skillCount == 0) {
            skillsDetail = "allow-bundled: []";
        } else if (skillNames.contains("*")) {
            skillsDetail = "allow-bundled: [*] (DEFAULT - loads ALL)";
        } else {
            skillsDetail = "allow-bundled: " + skillNames;
        }
        sb.append(String.format("%-27s%,7d    %s%n",
                "Skills (" + skillCount + " loaded)", skillsTokens, skillsDetail));

        // Built-in tools
        sb.append(String.format("%-27s%,7d    profile: %s%n",
                "Built-in tools (" + builtinToolCount + ")", builtinToolsTokens, toolProfile));

        // Project tools (plugin + custom)
        int totalProjectTools = pluginToolCount + customToolCount;
        if (totalProjectTools > 0) {
            String measurability = projectToolsTokens > 0
                    ? String.format("%d tools, ~%,d tokens from source scan", totalProjectTools, projectToolsTokens)
                    : totalProjectTools + " detected (not measurable offline)";
            sb.append(String.format("%-27s%,7d    %s%n",
                    "Project tools (" + totalProjectTools + ")", projectToolsTokens, measurability));
        }

        sb.append(String.format("%-27s%s%n", "", "------"));
        sb.append(String.format("%-27s%,7d    (excludes conversation history)%n",
                "Estimated total", estimatedTotalTokens));

        // Warnings
        sb.append('\n');
        if (warnings.isEmpty()) {
            sb.append("Warnings:\n  (none)\n");
        } else {
            sb.append("Warnings:\n");
            for (String warning : warnings) {
                sb.append("  ! ").append(warning).append('\n');
            }
        }

        return sb.toString();
    }
}
