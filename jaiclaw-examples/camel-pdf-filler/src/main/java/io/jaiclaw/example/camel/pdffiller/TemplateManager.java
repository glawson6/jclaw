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
import java.util.List;

/**
 * Loads and caches the PDF template and its form field metadata.
 */
@Configuration
public class TemplateManager {

    private static final Logger log = LoggerFactory.getLogger(TemplateManager.class);

    private final PdfFormReader pdfFormReader;
    private final Resource templateResource;
    private byte[] templateBytes;
    private List<PdfFormField> fields;

    public TemplateManager(
            PdfFormReader pdfFormReader,
            @Value("${app.template:classpath:templates/sample-form.pdf}") Resource templateResource) {
        this.pdfFormReader = pdfFormReader;
        this.templateResource = templateResource;
    }

    @PostConstruct
    void loadTemplate() throws IOException {
        this.templateBytes = templateResource.getInputStream().readAllBytes();
        this.fields = pdfFormReader.readFields(templateBytes);
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

    public String getFieldDescriptions() {
        StringBuilder sb = new StringBuilder();
        for (PdfFormField field : fields) {
            sb.append("- ").append(field.name()).append(" (").append(field.type());
            if (!field.options().isEmpty()) {
                sb.append(", options: ").append(field.options());
            }
            sb.append(")\n");
        }
        return sb.toString();
    }
}
