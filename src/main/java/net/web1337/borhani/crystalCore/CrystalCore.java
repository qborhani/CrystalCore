package net.web1337.borhani.crystalCore;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
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
import java.util.logging.Level;

public final class CrystalCore extends JavaPlugin implements Listener {
    private ProtocolManager protocolManager;
    private static final String MODRINTH_PROJECT_ID = "elmXFvzt";
    private String currentVersion;

    @Override
    public void onEnable() {

        this.currentVersion = getDescription().getVersion();
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Setup packet listeners
        setupPacketListeners();

        // Check for updates
        checkForUpdates();

        getLogger().info("CrystalCore has been enabled! Version: " + currentVersion);
    }

    private void setupPacketListeners() {
        // Crystal placement optimization
        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Client.USE_ITEM) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!event.isCancelled() && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.END_CRYSTAL) {
                    // Process crystal placement with proper synchronization
                    event.setReadOnly(false);
                    // Run synchronously to maintain proper packet order
                    Bukkit.getScheduler().runTask(getPlugin(), () -> {
                        try {
                            PacketContainer packet = event.getPacket();
                            // Validate packet data and block position before processing
                            if (packet != null && event.getPlayer().isOnline()) {
                                // Get block position from packet
                                org.bukkit.block.Block targetBlock = event.getPlayer().getTargetBlock(null, 5);
                                if (targetBlock != null && (targetBlock.getType() == Material.OBSIDIAN || targetBlock.getType() == Material.BEDROCK)) {
                                    // Process the placement packet synchronously with validation
                                    org.bukkit.Location placementLoc = targetBlock.getLocation().add(0, 1, 0);
                                    // Check if there's space for the crystal
                                    if (placementLoc.getBlock().getType() == Material.AIR) {
                                        event.getPlayer().updateInventory();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            getLogger().log(Level.WARNING, "Error handling crystal placement", e);
                        }
                    });
                }
            }
        });

        // Crystal attack optimization
        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!event.isCancelled()) {
                    try {
                        Entity target = event.getPacket().getEntityModifier(event).read(0);
                        if (target instanceof EnderCrystal) {
                            // Process crystal attack synchronously to prevent race conditions
                            event.setReadOnly(false);
                            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                                try {
                                    if (target.isValid() && !target.isDead()) {
                                        // Ensure crystal still exists before removing
                                        ((EnderCrystal) target).remove();
                                        // Update nearby players
                                        for (Player player : target.getWorld().getNearbyPlayers(target.getLocation(), 32)) {
                                            player.updateInventory();
                                        }
                                    }
                                } catch (Exception e) {
                                    getLogger().log(Level.WARNING, "Error handling crystal attack", e);
                                }
                            });
                        }
                    } catch (Exception e) {
                        getLogger().log(Level.WARNING, "Error handling crystal attack packet", e);
                    }
                }
            }
        });
    }

    @EventHandler
    public void onCrystalSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EnderCrystal) {
            // Optimize crystal spawn synchronization
            for (Player player : Bukkit.getOnlinePlayers()) {
                protocolManager.updateEntity(event.getEntity(), Collections.singletonList(player));
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

    @Override
    public void onDisable() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(this);
        }
        getLogger().info("CrystalCore has been disabled!");
    }
}
