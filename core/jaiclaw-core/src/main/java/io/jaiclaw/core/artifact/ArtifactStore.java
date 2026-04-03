package io.jaiclaw.core.artifact;

import java.util.Optional;

/**
 * SPI for storing binary artifacts (filled PDFs, generated files, etc.).
 * Implementations range from in-memory (testing/demos) to S3/database (production).
 */
public interface ArtifactStore {

    void save(StoredArtifact artifact);

    Optional<StoredArtifact> findById(String id);

    void updateStatus(String id, ArtifactStatus status, String message);

    void delete(String id);
}
