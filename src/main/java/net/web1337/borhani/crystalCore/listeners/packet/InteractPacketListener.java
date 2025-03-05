package net.web1337.borhani.crystalCore.listeners.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import net.web1337.borhani.crystalCore.CrystalCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collections;

public class InteractPacketListener extends PacketAdapter {

    public InteractPacketListener(CrystalCore plugin) {
        super(plugin, PacketType.Play.Client.USE_ITEM);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!event.isCancelled() && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.END_CRYSTAL) {
            event.setReadOnly(false);
            try {
                PacketContainer packet = event.getPacket();
                if (packet != null) {
                    Player player = event.getPlayer();
                    org.bukkit.block.Block targetBlock = player.getTargetBlock(null, 5);
                    
                    if ((targetBlock.getType() == Material.OBSIDIAN || targetBlock.getType() == Material.BEDROCK)) {
                        Location placementLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
                        org.bukkit.block.Block aboveBlock = targetBlock.getLocation().add(0, 1, 0).getBlock();
                        
                        if (aboveBlock.getType() == Material.AIR) {
                            // Check for entities in the placement area
                            if (player.getWorld().getNearbyEntities(placementLoc, 0.5, 1, 0.5,
                                    entity -> !(entity instanceof Player p) || p.getGameMode() != org.bukkit.GameMode.SPECTATOR).isEmpty()) {
                                
                                // Spawn crystal with optimized location
                                Entity crystal = player.getWorld().spawnEntity(placementLoc.subtract(0.0, 1.0, 0.0), org.bukkit.entity.EntityType.ENDER_CRYSTAL);
                                if (crystal != null) {
                                    EnderCrystal enderCrystal = (EnderCrystal) crystal;
                                    enderCrystal.setShowingBottom(false);
                                    
                                    // Store crystal in tracking collections
                                    CrystalCore.addCrystal(enderCrystal);
                                    
                                    // Update crystal visibility for nearby players
                                    for (Player nearbyPlayer : player.getWorld().getNearbyPlayers(placementLoc, 32)) {
                                        ((CrystalCore) plugin).getProtocolManager().updateEntity(crystal, Collections.singletonList(nearbyPlayer));
                                    }
                                    
                                    // Handle item consumption for non-creative players
                                    if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && 
                                        player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                                        org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
                                        item.setAmount(item.getAmount() - 1);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error handling crystal placement: " + e.getMessage());
            }
        }
    }
}