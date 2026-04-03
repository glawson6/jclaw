package io.jaiclaw.maven;

import io.jaiclaw.promptanalyzer.AnalysisReport;
import io.jaiclaw.promptanalyzer.ProjectScanner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Analyzes a JaiClaw project's application.yml and estimates per-request token overhead.
 * Optionally fails the build if a token threshold is exceeded or warnings are present.
 */
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class AnalyzeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    /**
     * Fail the build if estimated total tokens exceed this threshold.
     * Set to 0 (default) to disable threshold checking.
     */
    @Parameter(property = "jaiclaw.analyze.threshold", defaultValue = "0")
    private int threshold;

    /**
     * Skip the analysis entirely.
     */
    @Parameter(property = "jaiclaw.analyze.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Fail the build if the analysis produces any warnings.
     */
    @Parameter(property = "jaiclaw.analyze.failOnWarning", defaultValue = "false")
    private boolean failOnWarning;

    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            getLog().info("JaiClaw prompt analysis skipped");
            return;
        }

        Path projectPath = baseDir.toPath();
        Path appYml = projectPath.resolve("src/main/resources/application.yml");
        if (!appYml.toFile().isFile()) {
            getLog().debug("No src/main/resources/application.yml found — skipping analysis");
            return;
        }

        AnalysisReport report;
        try {
            report = new ProjectScanner().analyze(projectPath);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to analyze project: " + e.getMessage(), e);
        }

        // Print report line-by-line via Maven logger
        for (String line : report.format().split("\n")) {
            getLog().info(line);
        }

        // Check warnings
        if (failOnWarning && !report.warnings().isEmpty()) {
            throw new MojoFailureException(
                    "Prompt analysis produced " + report.warnings().size()
                            + " warning(s). Set failOnWarning=false to ignore.");
        }

        // Check threshold
        if (threshold > 0 && report.estimatedTotalTokens() > threshold) {
            throw new MojoFailureException(
                    "Estimated token count " + report.estimatedTotalTokens()
                            + " exceeds threshold " + threshold);
        }
    }
}
