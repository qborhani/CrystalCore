package net.web1337.borhani.crystalCore.version.impl.v1_19_R3;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.web1337.borhani.crystalCore.version.VersionAdapter;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;

public class VersionAdapter_1_19_R3 implements VersionAdapter {
    @Override
    public void updateCrystalForPlayer(EnderCrystal crystal, Player player) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        
        // Set entity ID
        packet.getIntegers().write(0, crystal.getEntityId());
        
        // Create metadata watcher
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        
        // Send the packet
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getVersion() {
        return "v1_19_R3";
    }

    @Override
    public boolean isSupported() {
        try {
            Class.forName("org.bukkit.craftbukkit.v1_19_R3.CraftServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}