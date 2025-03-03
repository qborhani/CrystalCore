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
                try {
                    if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.END_CRYSTAL) {
                        // Optimize crystal placement latency
                        event.setReadOnly(false);
                        PacketContainer packet = event.getPacket();
                        // Process the packet with reduced delay
                        Bukkit.getScheduler().runTask(getPlugin(), () -> {
                            try {
                                protocolManager.receiveClientPacket(event.getPlayer(), packet);
                            } catch (Exception e) {
                                getLogger().warning("Error processing crystal placement");
                            }
                        });
                    }
                } catch (Exception e) {
                    getLogger().warning("Error handling crystal placement packet");
                }
            }
        });

        // Crystal attack optimization
        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                try {
                    Entity target = event.getPacket().getEntityModifier(event).read(0);
                    if (target instanceof EnderCrystal) {
                        // Optimize crystal attack latency
                        event.setReadOnly(false);
                        PacketContainer packet = event.getPacket();
                        // Process the attack with reduced delay
                        Bukkit.getScheduler().runTask(getPlugin(), () -> {
                            try {
                                protocolManager.receiveClientPacket(event.getPlayer(), packet);
                            } catch (Exception e) {
                                getLogger().warning("Error processing crystal attack");
                            }
                        });
                    }
                } catch (Exception e) {
                    getLogger().warning("Error handling crystal attack packet");
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
