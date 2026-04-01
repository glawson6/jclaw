package io.jaiclaw.promptanalyzer;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.nio.file.Path;

/**
 * Spring Shell commands for analyzing JaiClaw project prompt token usage.
 */
@ShellComponent
public class PromptAnalyzerCommands {

    private final ProjectScanner scanner = new ProjectScanner();

    @ShellMethod(value = "Analyze a JaiClaw project and estimate input token usage",
                 key = {"prompt-analyze", "prompt analyze"})
    public String analyze(
            @ShellOption(value = "--path", defaultValue = ".") String projectPath
    ) {
        try {
            AnalysisReport report = scanner.analyze(Path.of(projectPath).toAbsolutePath());
            return report.format();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Check if estimated token usage is below a threshold",
                 key = {"prompt-check", "prompt check"})
    public String check(
            @ShellOption(value = "--path", defaultValue = ".") String projectPath,
            @ShellOption(value = "--threshold", defaultValue = "5000") int threshold
    ) {
        try {
            AnalysisReport report = scanner.analyze(Path.of(projectPath).toAbsolutePath());
            if (report.estimatedTotalTokens() <= threshold) {
                return String.format("PASS: estimated %,d tokens (threshold: %,d)",
                        report.estimatedTotalTokens(), threshold);
            } else {
                return String.format("FAIL: estimated %,d tokens exceeds threshold of %,d\n\n%s",
                        report.estimatedTotalTokens(), threshold, report.format());
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
