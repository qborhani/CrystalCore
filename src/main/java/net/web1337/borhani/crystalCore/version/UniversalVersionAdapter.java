package net.web1337.borhani.crystalCore.version;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UniversalVersionAdapter implements VersionAdapter {
    private final String serverVersion;
    private final int majorVersion;
    private final int minorVersion;

    public UniversalVersionAdapter() {
        String bukkitVersion = Bukkit.getBukkitVersion();
        this.serverVersion = Bukkit.getServer().getClass().getPackage().getName()
                .substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf('.') + 1);

        // First try modern version format (e.g., "1.21.4-R0.1-SNAPSHOT")
        Pattern modernPattern = Pattern.compile("^(\\d+)\\.(\\d+)");
        Matcher modernMatcher = modernPattern.matcher(bukkitVersion);

        if (modernMatcher.find()) {
            this.majorVersion = Integer.parseInt(modernMatcher.group(1));
            this.minorVersion = Integer.parseInt(modernMatcher.group(2));
        } else {
            // Fallback to legacy format (e.g., "v1_16_R3")
            Pattern legacyPattern = Pattern.compile("v1_(\\d+)_R\\d+");
            Matcher legacyMatcher = legacyPattern.matcher(serverVersion);

            if (legacyMatcher.find()) {
                this.majorVersion = 1;
                this.minorVersion = Integer.parseInt(legacyMatcher.group(1));
            } else {
                throw new IllegalStateException("Unable to determine server version from '" + bukkitVersion + "' or '" + serverVersion + "'");
            }
        }
    }

    @Override
    public void updateCrystalForPlayer(EnderCrystal crystal, Player player) {
        if (minorVersion >= 13) {
            // Hide the base of the crystal using the Bukkit API
            crystal.setShowingBottom(false);

            // Create a metadata packet using ProtocolLib
            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, crystal.getEntityId());

            // Create and initialize the WrappedDataWatcher from the crystal entity
            WrappedDataWatcher watcher = new WrappedDataWatcher(crystal);
            // Set the 'no baseplate' flag (index 15 for modern versions)
            watcher.setObject(15, WrappedDataWatcher.Registry.get(Boolean.class), true);

            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

            try {
                // Send the packet to the player
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getVersion() {
        return serverVersion;
    }

    @Override
    public boolean isSupported() {
        return minorVersion >= 13 && minorVersion <= 21;
    }

    /**
     * Gets the major version number of the server
     * @return the major version number
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Gets the minor version number of the server
     * @return the minor version number
     */
    public int getMinorVersion() {
        return minorVersion;
    }
}