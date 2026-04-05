package io.jaiclaw.example.camel.pdffiller;

import io.jaiclaw.camel.CamelMessageConverter;
import io.jaiclaw.core.artifact.ArtifactStatus;
import io.jaiclaw.core.artifact.ArtifactStore;
import io.jaiclaw.core.artifact.StoredArtifact;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.jaiclaw.documents.PdfFormField;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API for submitting JSON data and retrieving filled PDFs.
 *
 * <p>{@code POST /api/fill} — submit JSON for form filling, returns job ID.
 * <p>{@code GET /api/fill/{jobId}} — retrieve the filled PDF or job status.
 */
@RestController
@RequestMapping("/api")
public class PdfFillerRestController {

    private final ProducerTemplate producerTemplate;
    private final ArtifactStore artifactStore;
    private final TemplateManager templateManager;
    private final String outbox;

    public PdfFillerRestController(
            ProducerTemplate producerTemplate,
            ArtifactStore artifactStore,
            TemplateManager templateManager,
            @Value("${app.outbox:target/data/outbox}") String outbox) {
        this.producerTemplate = producerTemplate;
        this.artifactStore = artifactStore;
        this.templateManager = templateManager;
        this.outbox = outbox;
    }

    /**
     * Returns the PDF template's form fields so callers can see what fields are
     * available and what types they expect.
     */
    @GetMapping("/template")
    public ResponseEntity<Map<String, Object>> getTemplate() {
        List<PdfFormField> fields = templateManager.getFields();
        List<Map<String, Object>> fieldList = fields.stream()
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", f.name());
                    m.put("type", f.type().name());
                    if (f.currentValue() != null && !f.currentValue().isEmpty()) {
                        m.put("currentValue", f.currentValue());
                    }
                    if (!f.options().isEmpty()) {
                        m.put("options", f.options());
                    }
                    return m;
                })
                .toList();
        return ResponseEntity.ok(Map.of(
                "fieldCount", fields.size(),
                "fields", fieldList));
    }

    @PostMapping("/fill")
    public ResponseEntity<Map<String, String>> fill(@RequestBody String jsonBody) {
        String jobId = UUID.randomUUID().toString();

        StoredArtifact pending = new StoredArtifact(
                jobId, null, "application/pdf", jobId + ".pdf",
                ArtifactStatus.PENDING, null, Instant.now(), Map.of());
        artifactStore.save(pending);

        String templatePath = templateManager.getTemplatePath();
        String outputPath = outbox + "/" + jobId + ".pdf";
        String message = "Fill the PDF template at " + templatePath
                + " with the following data and write the output to "
                + outputPath + ":\n\n" + jsonBody;
        producerTemplate.sendBodyAndHeader(
                "seda:jaiclaw-pdf-filler-in", message,
                CamelMessageConverter.HEADER_PEER_ID, jobId);

        return ResponseEntity.accepted()
                .body(Map.of("jobId", jobId, "status", "PENDING"));
    }

    @GetMapping("/fill/{jobId}")
    public ResponseEntity<?> getResult(@PathVariable String jobId) {
        Optional<StoredArtifact> opt = artifactStore.findById(jobId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        StoredArtifact artifact = opt.get();
        if (artifact.status() == ArtifactStatus.COMPLETED && artifact.data() != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", artifact.filename());
            return new ResponseEntity<>(artifact.data(), headers, HttpStatus.OK);
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("jobId", jobId);
        status.put("status", artifact.status().name());
        if (artifact.statusMessage() != null) {
            status.put("message", artifact.statusMessage());
        }
        return ResponseEntity.ok(status);
    }
}
