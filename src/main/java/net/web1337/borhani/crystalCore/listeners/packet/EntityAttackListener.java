package net.web1337.borhani.crystalCore.listeners.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import net.web1337.borhani.crystalCore.CrystalCore;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collections;

public class EntityAttackListener extends PacketAdapter {

    public EntityAttackListener(CrystalCore plugin) {
        super(plugin, PacketType.Play.Client.USE_ENTITY);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!event.isCancelled()) {
            try {
                PacketContainer packet = event.getPacket();
                int entityId = packet.getIntegers().read(0);
                Player player = event.getPlayer();
                
                // Schedule the entity lookup and crystal handling on the main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Entity target = player.getWorld().getEntities().stream()
                        .filter(entity -> entity.getEntityId() == entityId && 
                                entity instanceof EnderCrystal && 
                                entity.isValid() && 
                                !entity.isDead())
                        .findFirst()
                        .orElse(null);
                    
                    if (target instanceof EnderCrystal) {
                        try {
                            // Send destroy packet directly to the attacking player for immediate feedback
                            PacketContainer destroyPacket = ((CrystalCore) plugin).getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                            destroyPacket.getIntLists().write(0, Collections.singletonList(target.getEntityId()));
                            ((CrystalCore) plugin).getProtocolManager().sendServerPacket(player, destroyPacket);
                            
                            // Then send to other nearby players
                            for (Player nearbyPlayer : target.getWorld().getNearbyPlayers(target.getLocation(), 32)) {
                                if (nearbyPlayer != player) {
                                    ((CrystalCore) plugin).getProtocolManager().sendServerPacket(nearbyPlayer, destroyPacket);
                                }
                            }
                            
                            // Remove crystal from tracking collections and world
                            CrystalCore.removeCrystal(target);
                            target.remove();
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error sending destroy packets: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Error handling crystal attack packet: " + e.getMessage());
            }
        }
    }
}