package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MobSpawnListener implements Listener {
    private final PvPGame plugin;

    public MobSpawnListener(PvPGame plugin) {
        this.plugin = plugin;
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
                break;

            // Cancel all other spawn reasons (natural spawning)
            default:
                event.setCancelled(true);
                break;
        }
    }
}
