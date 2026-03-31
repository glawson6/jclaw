package io.jaiclaw.voicecall.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jaiclaw.voicecall.model.CallRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * JSONL append-only file store for call records. Each line is a JSON-serialized CallRecord.
 * Writes happen asynchronously via a single-threaded executor.
 */
public class JsonlCallStore implements CallStore {

    private static final Logger log = LoggerFactory.getLogger(JsonlCallStore.class);

    private final Path storePath;
    private final ObjectMapper objectMapper;
    private final ExecutorService writeExecutor;

    // In-memory index for fast lookups (populated on load)
    private final Map<String, CallRecord> index = new LinkedHashMap<>();

    public JsonlCallStore(Path storePath) {
        this.storePath = storePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.writeExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "jsonl-call-store-writer");
            t.setDaemon(true);
            return t;
        });
        loadFromDisk();
    }

    @Override
    public void persist(CallRecord record) {
        synchronized (index) {
            index.put(record.getCallId(), record);
        }
        writeExecutor.submit(() -> appendToDisk(record));
    }

    @Override
    public Map<String, CallRecord> loadActiveCalls() {
        synchronized (index) {
            return index.entrySet().stream()
                    .filter(e -> !e.getValue().getState().isTerminal())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    @Override
    public List<CallRecord> getHistory(int limit) {
        synchronized (index) {
            List<CallRecord> all = new ArrayList<>(index.values());
            all.sort(Comparator.comparing(CallRecord::getStartedAt).reversed());
            return all.stream().limit(limit).collect(Collectors.toList());
        }
    }

    private void loadFromDisk() {
        if (!Files.exists(storePath)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(storePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    CallRecord record = objectMapper.readValue(line, CallRecord.class);
                    index.put(record.getCallId(), record);
                } catch (Exception e) {
                    log.warn("Skipping malformed JSONL line: {}", e.getMessage());
                }
            }
            log.info("Loaded {} call records from {}", index.size(), storePath);
        } catch (IOException e) {
            log.error("Failed to load call store from {}: {}", storePath, e.getMessage());
        }
    }

    private void appendToDisk(CallRecord record) {
        try {
            Files.createDirectories(storePath.getParent());
            String json = objectMapper.writeValueAsString(record);
            Files.writeString(storePath, json + "\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to persist call record {}: {}", record.getCallId(), e.getMessage());
        }
    }

    public void shutdown() {
        writeExecutor.shutdown();
    }
}
