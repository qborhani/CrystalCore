package net.web1337.borhani.crystalCore.version;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class VersionAdapterFactory {
    private static final Map<String, Class<? extends VersionAdapter>> adapters = new HashMap<>();
    private static VersionAdapter currentAdapter;

    public static void setAdapter(VersionAdapter adapter) {
        currentAdapter = adapter;
    }

    public static void registerAdapter(String version, Class<? extends VersionAdapter> adapterClass) {
        adapters.put(version, adapterClass);
    }

    private static String parseServerVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = packageName.split("\\.");
            if (parts.length < 4) {
                throw new IllegalStateException("Invalid package name format: " + packageName);
            }
            String version = parts[3];
            if (!version.startsWith("v") || !version.matches("v\\d+_\\d+_R\\d+")) {
                throw new IllegalStateException("Invalid version format: " + version + ". Expected format: v<major>_<minor>_R<revision>");
            }
            return version;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse server version", e);
        }
    }

    private static VersionAdapter findCompatibleAdapter(String serverVersion, Logger logger) {
        // Parse version components
        String[] versionParts = serverVersion.substring(1).split("_");
        int majorVersion = Integer.parseInt(versionParts[0]);
        int minorVersion = Integer.parseInt(versionParts[1]);

        // First try exact match
        if (adapters.containsKey(serverVersion)) {
            try {
                VersionAdapter adapter = adapters.get(serverVersion).getDeclaredConstructor().newInstance();
                if (adapter.isSupported()) {
                    return adapter;
                }
            } catch (Exception e) {
                logger.warning("Failed to initialize exact version adapter for " + serverVersion + ": " + e.getMessage());
            }
        }

        // Try to find the closest compatible version
        VersionAdapter bestMatch = null;
        int bestMatchDiff = Integer.MAX_VALUE;

        for (Map.Entry<String, Class<? extends VersionAdapter>> entry : adapters.entrySet()) {
            if (entry.getKey().equals(serverVersion)) continue; // Skip already tried version

            String[] adapterVersionParts = entry.getKey().substring(1).split("_");
            int adapterMajor = Integer.parseInt(adapterVersionParts[0]);
            int adapterMinor = Integer.parseInt(adapterVersionParts[1]);

            // Calculate version difference
            int versionDiff = Math.abs((majorVersion * 100 + minorVersion) - (adapterMajor * 100 + adapterMinor));

            try {
                VersionAdapter adapter = entry.getValue().getDeclaredConstructor().newInstance();
                if (adapter.isSupported() && versionDiff < bestMatchDiff) {
                    bestMatch = adapter;
                    bestMatchDiff = versionDiff;
                    logger.info("Found potential compatible adapter " + entry.getKey() + " for server version " + serverVersion);
                }
            } catch (Exception e) {
                logger.warning("Failed to initialize version adapter for " + entry.getKey() + ": " + e.getMessage());
            }
        }

        if (bestMatch != null) {
            logger.info("Selected best compatible adapter: " + bestMatch.getVersion() + " for server version " + serverVersion);
            return bestMatch;
        }
        return null;
    }

    public static VersionAdapter getAdapter() {
        if (currentAdapter != null) {
            return currentAdapter;
        }

        Logger logger = Bukkit.getLogger();
        String serverVersion = parseServerVersion();
        logger.info("Detected server version: " + serverVersion);

        currentAdapter = findCompatibleAdapter(serverVersion, logger);
        return currentAdapter;
    }
}