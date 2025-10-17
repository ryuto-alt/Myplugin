package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;

public class WaterBucketListener implements Listener {
    private final PvPGame plugin;

    public WaterBucketListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();

        // Only process during game
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // Check if the bucket being emptied is a water bucket
        if (event.getBucket() == Material.WATER_BUCKET) {
            // Schedule removal of the empty bucket after the event completes
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Remove the empty bucket from player's inventory
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (mainHand != null && mainHand.getType() == Material.BUCKET) {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            });
        }
    }
}
