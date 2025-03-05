package net.web1337.borhani.crystalCore.listeners.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import net.web1337.borhani.crystalCore.CrystalCore;

public class PacketEventManager {
    private final CrystalCore plugin;
    private final ProtocolManager protocolManager;

    public PacketEventManager(CrystalCore plugin, ProtocolManager protocolManager) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
    }

    public void registerListeners() {
        // Register packet listeners
        protocolManager.addPacketListener(new EntityAttackListener(plugin));
        protocolManager.addPacketListener(new InteractPacketListener(plugin));
    }

    public void unregisterListeners() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(plugin);
        }
    }
}