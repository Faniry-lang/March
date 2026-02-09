package march.dev.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility to read framework properties with a preference order:
 * 1) `march.properties` from classpath (preferred)
 * 2) System properties (-D...)
 * 3) Environment variables (uppercased, dots -> underscores)
 */
public final class PropertyUtils {

    private static final Properties CP_PROPS = new Properties();

    static {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("march.properties")) {
            if (is != null) {
                CP_PROPS.load(is);
            }
        } catch (Exception e) {
            // ignore: property file missing is acceptable
        }
    }

    private PropertyUtils() {}

    public static String getPreferred(String key) {
        // 1) classpath properties
        String v = CP_PROPS.getProperty(key);
        if (v != null && !v.isBlank()) return v;

        // 2) system property
        v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v;

        // 3) env var fallback (dots -> underscores, uppercased)
        String envKey = key.replace('.', '_').toUpperCase();
        v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v;

        return null;
    }

    public static String getPreferred(String key, String defaultValue) {
        String v = getPreferred(key);
        return v != null ? v : defaultValue;
    }

    public static boolean getPreferredBoolean(String key, boolean defaultValue) {
        String v = getPreferred(key);
        if (v == null) return defaultValue;
        return v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes");
    }

    public static int getPreferredInt(String key, int defaultValue) {
        String v = getPreferred(key);
        if (v == null) return defaultValue;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return defaultValue; }
    }
}
