package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GeneratorSelectionListener implements Listener {
    private final PvPGame plugin;
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private static final long CLICK_COOLDOWN = 500; // 0.5 seconds in milliseconds

    public GeneratorSelectionListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        // Check if player is in generator selection mode
        if (!plugin.getGUIClickListener().hasGeneratorSelection(player.getUniqueId())) {
            return;
        }

        // Check cooldown to prevent double-click
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastClickTime.get(player.getUniqueId());
        if (lastClick != null && (currentTime - lastClick) < CLICK_COOLDOWN) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        lastClickTime.put(player.getUniqueId(), currentTime);

        // Handle corner selection
        plugin.getGUIClickListener().handleCornerSelection(player, event.getClickedBlock().getLocation());
    }
}
