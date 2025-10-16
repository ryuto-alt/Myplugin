package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;

public class GeneratorSelectionListener implements Listener {
    private final PvPGame plugin;

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

        event.setCancelled(true);

        // Handle corner selection
        plugin.getGUIClickListener().handleCornerSelection(player, event.getClickedBlock().getLocation());
    }
}
