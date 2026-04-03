package io.jaiclaw.core.artifact;

import java.time.Instant;
import java.util.Map;

public record StoredArtifact(
    String id,
    byte[] data,
    String mimeType,
    String filename,
    ArtifactStatus status,
    String statusMessage,
    Instant createdAt,
    Map<String, String> metadata
) {
    public StoredArtifact {
        if (status == null) status = ArtifactStatus.PENDING;
        if (createdAt == null) createdAt = Instant.now();
        if (metadata == null) metadata = Map.of();
    }
}
