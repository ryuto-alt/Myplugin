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
                        golem.setCustomName("§7" + pendingGolemTeam + "のゴーレム");
                        golem.setCustomNameVisible(true);
                        golem.setPlayerCreated(false);
                        golem.setPersistent(true);
                        golem.setMetadata("ownerTeam", new FixedMetadataValue(plugin, pendingGolemTeam));

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
}
