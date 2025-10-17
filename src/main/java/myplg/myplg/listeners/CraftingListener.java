package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;

/**
 * Disables all crafting
 */
public class CraftingListener implements Listener {
    private final PvPGame plugin;

    public CraftingListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }
        event.setCancelled(true);
        event.getWhoClicked().sendMessage("§cクラフトは無効化されています！");
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }
        event.getInventory().setResult(null);
    }
}
