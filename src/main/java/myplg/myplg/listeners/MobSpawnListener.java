package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Location;

public class MobSpawnListener implements Listener {
    private final PvPGame plugin;
    private String pendingGolemTeam = null; // Store team for next golem spawn

    public MobSpawnListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player is using an Iron Golem spawn egg
        if (item != null && item.getType() == Material.IRON_GOLEM_SPAWN_EGG) {
            // Get player's team
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
            if (teamName != null) {
                // Store team name for the next golem spawn
                pendingGolemTeam = teamName;
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Allow only mobs spawned by player actions
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

        switch (reason) {
            // Allow player-spawned mobs
            case SPAWNER_EGG:
            case DISPENSE_EGG:
            case BREEDING:
            case BUILD_SNOWMAN:
            case BUILD_IRONGOLEM:
            case BUILD_WITHER:
            case CURED:
            case CUSTOM:
                // Allow these spawn reasons

                // Special handling for Iron Golem from spawn egg
                if (event.getEntity() instanceof IronGolem &&
                    (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG ||
                     reason == CreatureSpawnEvent.SpawnReason.DISPENSE_EGG)) {

                    IronGolem golem = (IronGolem) event.getEntity();

                    // Apply pending team metadata
                    if (pendingGolemTeam != null) {
                        // Get team color code
                        String colorCode = getTeamColorCode(pendingGolemTeam);
                        golem.setCustomName(colorCode + pendingGolemTeam + "のゴーレム");
                        golem.setCustomNameVisible(true);
                        golem.setPlayerCreated(false);
                        golem.setPersistent(true);
                        golem.setMetadata("ownerTeam", new FixedMetadataValue(plugin, pendingGolemTeam));
                        
                        // Save spawn location for range limiting
                        Location spawnLoc = golem.getLocation();
                        golem.setMetadata("spawnX", new FixedMetadataValue(plugin, spawnLoc.getX()));
                        golem.setMetadata("spawnY", new FixedMetadataValue(plugin, spawnLoc.getY()));
                        golem.setMetadata("spawnZ", new FixedMetadataValue(plugin, spawnLoc.getZ()));
                        
                        // Set golem attack damage to 8 (4 hearts)
                        org.bukkit.attribute.AttributeInstance attackAttribute = golem.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
                        if (attackAttribute != null) {
                            attackAttribute.setBaseValue(8.0);
                        }
                        
                        // Set golem max health to 83 (default 100, reduced by ~17%)
                        org.bukkit.attribute.AttributeInstance healthAttribute = golem.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        if (healthAttribute != null) {
                            healthAttribute.setBaseValue(83.0);
                            golem.setHealth(83.0);
                        }

                        plugin.getLogger().info("Iron Golem spawned for team: " + pendingGolemTeam);

                        // Clear pending team
                        pendingGolemTeam = null;
                    }
                }
                break;

            // Cancel all other spawn reasons (natural spawning)
            default:
                event.setCancelled(true);
                break;
        }
    }
    
    /**
     * Get team color code for golem name display
     */
    private String getTeamColorCode(String teamName) {
        switch (teamName) {
            case "レッド": return "§c";
            case "ブルー": return "§9";
            case "グリーン": return "§a";
            case "イエロー": return "§e";
            case "アクア": return "§b";
            case "ホワイト": return "§f";
            case "ピンク": return "§d";
            case "グレー": return "§7";
            default: return "§f";
        }
    }
}
