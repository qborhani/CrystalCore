package net.web1337.borhani.crystalCore.version;

import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;

public interface VersionAdapter {
    /**
     * Updates the crystal entity for a specific player
     *
     * @param crystal The crystal to update
     * @param player The player to update the crystal for
     */
    void updateCrystalForPlayer(EnderCrystal crystal, Player player);

    /**
     * Gets the server's NMS version
     *
     * @return The NMS version string (e.g., "v1_8_R3")
     */
    String getVersion();

    /**
     * Checks if this adapter supports the current server version
     *
     * @return true if this adapter supports the current server version
     */
    boolean isSupported();
}