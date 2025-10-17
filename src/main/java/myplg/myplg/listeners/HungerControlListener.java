package myplg.myplg.listeners;

import myplg.myplg.GameMode;
import myplg.myplg.PvPGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class HungerControlListener implements Listener {
    private final PvPGame plugin;

    public HungerControlListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // ゲーム中は満腹度を常にマックスに保つ
        if (plugin.getGameManager().isGameRunning()) {
            event.setCancelled(true);
            Player player = (Player) event.getEntity();
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
    }
}
