package march.dev.utils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance tool result cache. Agents should create their own instance
 * so caches are isolated per-agent. This replaces the previous static
 * global cache.
 */
public class ToolResultCache {

    private static class Entry {
        final String value;
        final long timestamp;

        Entry(String value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();
    private volatile long ttlMs;

    public ToolResultCache(long ttlMs) {
        this.ttlMs = ttlMs > 0 ? ttlMs : 5 * 60 * 1000;
    }

    public String get(String key) {
        Entry e = cache.get(key);
        if (e == null) return null;
        if (System.currentTimeMillis() - e.timestamp > ttlMs) {
            cache.remove(key);
            return null;
        }
        return e.value;
    }

    public void put(String key, String value) {
        cache.put(key, new Entry(value, System.currentTimeMillis()));
    }

    public void invalidate(String key) {
        cache.remove(key);
    }

    public void setTtlMs(long ttlMs) {
        if (ttlMs > 0) this.ttlMs = ttlMs;
    }

}
