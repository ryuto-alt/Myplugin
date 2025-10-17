package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Removes attack cooldown to enable spam clicking like 1.8
 */
public class AttackCooldownListener implements Listener {
    private final PvPGame plugin;

    public AttackCooldownListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        removeAttackCooldown(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        removeAttackCooldown(event.getPlayer());
    }

    private void removeAttackCooldown(Player player) {
        try {
            Attribute attackSpeedAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.attack_speed"));
            org.bukkit.attribute.AttributeInstance attackSpeed = player.getAttribute(attackSpeedAttr);
            if (attackSpeed != null) {
                attackSpeed.setBaseValue(1024.0);
            }
        } catch (IllegalArgumentException e) {
            // Attribute not available in this version, ignore
            plugin.getLogger().warning("GENERIC_ATTACK_SPEED attribute not available in this Minecraft version");
        }
    }
}
