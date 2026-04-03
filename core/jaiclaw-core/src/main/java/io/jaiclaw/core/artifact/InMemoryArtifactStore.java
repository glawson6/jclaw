package io.jaiclaw.core.artifact;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryArtifactStore implements ArtifactStore {

    private final ConcurrentHashMap<String, StoredArtifact> store = new ConcurrentHashMap<>();

    @Override
    public void save(StoredArtifact artifact) {
        store.put(artifact.id(), artifact);
    }

    @Override
    public Optional<StoredArtifact> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void updateStatus(String id, ArtifactStatus status, String message) {
        store.computeIfPresent(id, (key, existing) -> new StoredArtifact(
                existing.id(), existing.data(), existing.mimeType(), existing.filename(),
                status, message, existing.createdAt(), existing.metadata()));
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }
}
