package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for when players pick up items or move items in inventory
 * and automatically applies team upgrade enchantments to weapons and armor
 */
public class UpgradeEnchantmentListener implements Listener {
    private final PvPGame plugin;

    public UpgradeEnchantmentListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (item == null) {
            return;
        }

        // Schedule enchantment application after the click event completes
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            applyTeamEnchantments(player, item);
        });
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();

        // Schedule enchantment application after pickup completes
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            applyTeamEnchantments(player, item);
        });
    }

    private void applyTeamEnchantments(Player player, ItemStack item) {
        String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        if (teamName == null) {
            return;
        }

        // Apply weapon enchantment
        if (item.getType().toString().contains("SWORD")) {
            if (plugin.getWeaponUpgradeManager().hasWeaponUpgrade(teamName)) {
                // Remove existing sharpness and add Sharpness I
                item.removeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS);
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 1);
            }
        }

        // Apply armor enchantment
        if (item.getType().toString().contains("HELMET") ||
            item.getType().toString().contains("CHESTPLATE") ||
            item.getType().toString().contains("LEGGINGS") ||
            item.getType().toString().contains("BOOTS")) {

            int armorLevel = plugin.getArmorUpgradeManager().getArmorLevel(teamName);
            if (armorLevel > 0) {
                // Remove existing protection and add current level
                item.removeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION);
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, armorLevel);
            }
        }
    }
}
