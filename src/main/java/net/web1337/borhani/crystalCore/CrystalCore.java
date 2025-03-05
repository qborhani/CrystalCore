package net.web1337.borhani.crystalCore;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.web1337.borhani.crystalCore.listeners.packet.PacketEventManager;
import net.web1337.borhani.crystalCore.version.UniversalVersionAdapter;
import net.web1337.borhani.crystalCore.version.VersionAdapter;
import net.web1337.borhani.crystalCore.version.VersionAdapterFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class CrystalCore extends JavaPlugin implements Listener {
    private ProtocolManager protocolManager;
    private static final String MODRINTH_PROJECT_ID = "elmXFvzt";
    private String currentVersion;
    private static CrystalCore INSTANCE;
    private static final Map<Integer, EnderCrystal> CRYSTALS = new ConcurrentHashMap<>();
    private static final Set<Location> CRYSTAL_LOCATIONS = ConcurrentHashMap.newKeySet();

    public static Map<Integer, EnderCrystal> getCrystals() {
        return Collections.unmodifiableMap(CRYSTALS);
    }

    public static Set<Location> getCrystalLocations() {
        return Collections.unmodifiableSet(CRYSTAL_LOCATIONS);
    }

    public static void addCrystal(EnderCrystal crystal) {
        CRYSTALS.put(crystal.getEntityId(), crystal);
        CRYSTAL_LOCATIONS.add(crystal.getLocation());
    }

    public static void removeCrystal(Entity crystal) {
        CRYSTALS.remove(crystal.getEntityId());
        CRYSTAL_LOCATIONS.remove(crystal.getLocation());
    }

    public static CrystalCore getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.currentVersion = this.getDescription().getVersion();
        
        // Check for ProtocolLib dependency
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("ProtocolLib is required for CrystalCore to function! Please install ProtocolLib and restart the server.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        // Register version adapters
        registerVersionAdapters();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Setup packet listeners
        setupPacketListeners();

        // Check for updates
        checkForUpdates();

        getLogger().info("CrystalCore has been enabled! Version: " + currentVersion);
    }

    private void setupPacketListeners() {
        // Initialize and register packet listeners through PacketEventManager
        PacketEventManager packetEventManager = new PacketEventManager(this, protocolManager);
        packetEventManager.registerListeners();
    }

    private void registerVersionAdapters() {
        // Initialize the universal version adapter
        UniversalVersionAdapter adapter = new UniversalVersionAdapter();
        if (!adapter.isSupported()) {
            getLogger().severe("This server version is not supported! The plugin supports versions 1.13 - 1.21.4");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Successfully loaded version adapter for " + adapter.getVersion());
        VersionAdapterFactory.setAdapter(adapter);
    }

    @EventHandler
    public void onCrystalSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal) {
            // Add crystal to tracking collections
            addCrystal(crystal);
            
            // Ensure crystal spawn handling is done on the main thread
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(this, () -> handleCrystalSpawn(crystal));
                return;
            }
            handleCrystalSpawn(crystal);
        }
    }

    private void handleCrystalSpawn(EnderCrystal crystal) {
        // Get the version adapter to handle version-specific metadata
        VersionAdapter adapter = VersionAdapterFactory.getAdapter();
        if (adapter != null) {
            try {
                // Let the version adapter set the appropriate metadata values first
                adapter.updateCrystalForPlayer(crystal, null);
                
                // Create metadata packet using ProtocolLib
                PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                metadataPacket.getIntegers().write(0, crystal.getEntityId());
                
                // Create a new DataWatcher instance for the crystal
                WrappedDataWatcher watcher = new WrappedDataWatcher();
                
                // Get the crystal's default metadata from the entity
                List<WrappedWatchableObject> defaultMetadata = WrappedDataWatcher.getEntityWatcher(crystal).getWatchableObjects();
                
                // Add all default metadata to our new watcher
                for (WrappedWatchableObject metadata : defaultMetadata) {
                    watcher.setObject(metadata.getIndex(), metadata.getValue());
                }
                
                // Write the metadata to the packet
                metadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
                
                // Only send packets to nearby players to optimize performance
                for (Player player : crystal.getWorld().getNearbyPlayers(crystal.getLocation(), 16)) {
                    try {
                        protocolManager.sendServerPacket(player, metadataPacket);
                        adapter.updateCrystalForPlayer(crystal, player);
                    } catch (Exception e) {
                        getLogger().log(Level.WARNING, "Error sending crystal metadata packet to player " + player.getName(), e);
                    }
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error handling crystal metadata", e);
            }
        }
    }

    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_ID + "/version");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject latestVersion = JsonParser.parseString(response.toString())
                        .getAsJsonArray().get(0).getAsJsonObject();
                String latestVersionNumber = latestVersion.get("version_number").getAsString();

                if (!currentVersion.equals(latestVersionNumber)) {
                    getLogger().info("A new version of CrystalCore is available: " + latestVersionNumber);
                    getLogger().info("Download it from: https://modrinth.com/plugin/crystalcore");
                }
            } catch (Exception e) {
                if (e instanceof java.io.FileNotFoundException) {
                    getLogger().warning("Failed to check for updates: Project not found on Modrinth. Please verify the project ID.");
                } else {
                    getLogger().warning("Failed to check for updates: " + e.getMessage());
                }
            }
        });
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    @Override
    public void onDisable() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(this);
        }
        getLogger().info("CrystalCore has been disabled!");
    }
}
