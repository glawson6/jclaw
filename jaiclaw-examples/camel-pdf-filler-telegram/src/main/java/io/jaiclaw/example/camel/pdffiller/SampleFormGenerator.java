package io.jaiclaw.example.camel.pdffiller;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates a sample PDF form template at startup if the configured template path
 * points to a file that does not yet exist.
 */
@Configuration
public class SampleFormGenerator {

    private static final Logger log = LoggerFactory.getLogger(SampleFormGenerator.class);

    @Bean
    ApplicationRunner sampleFormGeneratorRunner(
            @Value("${app.template:classpath:templates/sample-form.pdf}") Resource templateResource) {
        return args -> {
            if (templateResource.isFile()) {
                Path templatePath = templateResource.getFile().toPath();
                if (!Files.exists(templatePath)) {
                    Files.createDirectories(templatePath.getParent());
                    generateSampleForm(templatePath);
                }
            }
        };
    }

    private void generateSampleForm(Path outputPath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDAcroForm acroForm = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(acroForm);

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            acroForm.setDefaultResources(createDefaultResources(font));

            String[] textFields = {"fullName", "email", "phone", "address", "city", "state", "zipCode"};
            for (String fieldName : textFields) {
                PDTextField tf = new PDTextField(acroForm);
                tf.setPartialName(fieldName);
                acroForm.getFields().add(tf);
            }

            PDCheckBox cb = new PDCheckBox(acroForm);
            cb.setPartialName("agreedToTerms");
            acroForm.getFields().add(cb);

            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(font, 16);
                stream.newLineAtOffset(50, 750);
                stream.showText("Sample Registration Form");
                stream.endText();
            }

            try (OutputStream out = Files.newOutputStream(outputPath)) {
                doc.save(out);
            }
            log.info("Generated sample PDF form template at {}", outputPath);
        }
    }

    private org.apache.pdfbox.pdmodel.PDResources createDefaultResources(PDType1Font font) {
        org.apache.pdfbox.pdmodel.PDResources resources = new org.apache.pdfbox.pdmodel.PDResources();
        resources.put(org.apache.pdfbox.cos.COSName.getPDFName("Helv"), font);
        return resources;
    }
}
