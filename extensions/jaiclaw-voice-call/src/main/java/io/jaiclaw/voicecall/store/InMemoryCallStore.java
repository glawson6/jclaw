package io.jaiclaw.voicecall.store;

import io.jaiclaw.voicecall.model.CallRecord;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory call store backed by ConcurrentHashMap.
 * Suitable for development and testing.
 */
public class InMemoryCallStore implements CallStore {

    private final ConcurrentHashMap<String, CallRecord> records = new ConcurrentHashMap<>();

    @Override
    public void persist(CallRecord record) {
        records.put(record.getCallId(), record);
    }

    @Override
    public Map<String, CallRecord> loadActiveCalls() {
        return records.entrySet().stream()
                .filter(e -> !e.getValue().getState().isTerminal())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public List<CallRecord> getHistory(int limit) {
        return records.values().stream()
                .sorted(Comparator.comparing(CallRecord::getStartedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public int size() {
        return records.size();
    }

    public void clear() {
        records.clear();
    }
}
