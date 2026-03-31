package io.jaiclaw.voicecall.store;

import io.jaiclaw.voicecall.model.CallRecord;

import java.util.List;
import java.util.Map;

/**
 * Persistence interface for call records.
 */
public interface CallStore {

    /**
     * Persist a call record (create or update).
     */
    void persist(CallRecord record);

    /**
     * Load all non-terminal call records. Used for recovery on startup.
     */
    Map<String, CallRecord> loadActiveCalls();

    /**
     * Return the most recent call records, up to the given limit.
     */
    List<CallRecord> getHistory(int limit);
}
