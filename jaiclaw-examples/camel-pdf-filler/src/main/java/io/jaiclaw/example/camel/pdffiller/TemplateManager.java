package io.jaiclaw.example.camel.pdffiller;

import io.jaiclaw.documents.PdfFormField;
import io.jaiclaw.documents.PdfFormReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads and caches the PDF template and its form field metadata.
 *
 * <p>The template path is configured via {@code APP_TEMPLATE} env var (or
 * {@code app.template} property). Supports Spring resource prefixes:
 * <ul>
 *   <li>{@code file:/path/to/form.pdf} — local filesystem (default)</li>
 *   <li>{@code classpath:templates/form.pdf} — classpath resource</li>
 * </ul>
 *
 * <p>If the template is a {@code file:} resource and does not exist,
 * {@link SampleFormGenerator} creates a sample PDF form with 8 fields
 * (fullName, email, phone, address, city, state, zipCode, agreedToTerms).
 */
@Configuration
public class TemplateManager {

    private static final Logger log = LoggerFactory.getLogger(TemplateManager.class);

    private final PdfFormReader pdfFormReader;
    private final SampleFormGenerator sampleFormGenerator;
    private final Resource templateResource;
    private byte[] templateBytes;
    private List<PdfFormField> fields;
    private String templatePath;

    public TemplateManager(
            PdfFormReader pdfFormReader,
            SampleFormGenerator sampleFormGenerator,
            @Value("${app.template:file:target/data/templates/sample-form.pdf}") Resource templateResource) {
        this.pdfFormReader = pdfFormReader;
        this.sampleFormGenerator = sampleFormGenerator;
        this.templateResource = templateResource;
    }

    @PostConstruct
    void loadTemplate() throws IOException {
        // Ensure the sample template exists before trying to load it
        sampleFormGenerator.ensureTemplateExists();

        this.templateBytes = templateResource.getInputStream().readAllBytes();
        this.fields = pdfFormReader.readFields(templateBytes);

        // Resolve the absolute filesystem path for the agent's tool calls
        try {
            this.templatePath = templateResource.getFile().getAbsolutePath();
        } catch (IOException e) {
            // Classpath resources can't be resolved to a File — extract to temp
            Path tempFile = Path.of(System.getProperty("java.io.tmpdir"), "jaiclaw-pdf-template.pdf");
            java.nio.file.Files.write(tempFile, templateBytes);
            this.templatePath = tempFile.toAbsolutePath().toString();
            log.info("Classpath template extracted to: {}", templatePath);
        }

        log.info("Loaded PDF template ({} bytes, {} form fields): {}",
                templateBytes.length, fields.size(),
                fields.stream().map(PdfFormField::name).toList());
    }

    public byte[] getTemplateBytes() {
        return templateBytes;
    }

    public List<PdfFormField> getFields() {
        return fields;
    }

    /**
     * Returns the absolute filesystem path to the template PDF, suitable for
     * passing to the agent so it can use {@code pdf_read_fields} and
     * {@code pdf_fill_form} tools.
     */
    public String getTemplatePath() {
        return templatePath;
    }
}
