package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BridgeBuilderListener implements Listener {
    private final PvPGame plugin;
    private final Set<UUID> bridgeEggs;

    public BridgeBuilderListener(PvPGame plugin) {
        this.plugin = plugin;
        this.bridgeEggs = new HashSet<>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player right-clicked with Bridge Builder Egg
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
            item != null && item.getType() == Material.EGG && item.hasItemMeta() &&
            item.getItemMeta().hasDisplayName() &&
            item.getItemMeta().getDisplayName().equals("§bBridge Builder Egg")) {

            event.setCancelled(true);

            // Launch egg projectile
            Egg egg = player.launchProjectile(Egg.class);
            egg.setVelocity(player.getLocation().getDirection().multiply(1.5));

            // Get player's team wool color
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
            Material woolType = Material.WHITE_WOOL;
            if (teamName != null) {
                woolType = getTeamWool(teamName);
            }

            // Store metadata for wool type
            egg.setMetadata("bridgeBuilder", new FixedMetadataValue(plugin, true));
            egg.setMetadata("woolType", new FixedMetadataValue(plugin, woolType.toString()));
            egg.setMetadata("startLocation", new FixedMetadataValue(plugin, player.getLocation().clone()));

            // Track this egg
            bridgeEggs.add(egg.getUniqueId());

            // Remove one egg from player's inventory
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().remove(item);
            }

            // Start bridge building task
            startBridgeBuilding(egg, player.getLocation().clone(), woolType);
        }
    }

    private void startBridgeBuilding(Egg egg, Location startLocation, Material woolType) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            // Check if egg still exists
            if (egg.isDead() || !egg.isValid()) {
                task.cancel();
                bridgeEggs.remove(egg.getUniqueId());
                return;
            }

            Location currentLoc = egg.getLocation();

            // Check distance traveled (max 25 blocks)
            if (startLocation.distance(currentLoc) > 25) {
                egg.remove();
                task.cancel();
                bridgeEggs.remove(egg.getUniqueId());
                return;
            }

            // Place wool block below the egg's position
            Location blockLoc = currentLoc.clone().subtract(0, 1, 0);
            blockLoc.setX(blockLoc.getBlockX());
            blockLoc.setY(blockLoc.getBlockY());
            blockLoc.setZ(blockLoc.getBlockZ());

            // Only place if the block is air or replaceable
            if (blockLoc.getBlock().getType() == Material.AIR ||
                blockLoc.getBlock().getType() == Material.WATER ||
                blockLoc.getBlock().getType() == Material.LAVA) {
                blockLoc.getBlock().setType(woolType);
            }

        }, 0L, 1L); // Run every tick (1/20th of a second)
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        // Clean up when egg hits something
        if (event.getEntity() instanceof Egg) {
            Egg egg = (Egg) event.getEntity();
            if (bridgeEggs.contains(egg.getUniqueId())) {
                bridgeEggs.remove(egg.getUniqueId());
            }
        }
    }

    private Material getTeamWool(String teamName) {
        switch (teamName) {
            case "アクア": return Material.CYAN_WOOL;
            case "イエロー": return Material.YELLOW_WOOL;
            case "ブルー": return Material.BLUE_WOOL;
            case "ホワイト": return Material.WHITE_WOOL;
            case "グレー": return Material.GRAY_WOOL;
            case "ピンク": return Material.PINK_WOOL;
            case "グリーン": return Material.GREEN_WOOL;
            case "レッド": return Material.RED_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }
}
