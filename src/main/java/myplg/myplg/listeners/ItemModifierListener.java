package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Modifies items to:
 * - Set pickaxe and axe attack damage to 0
 * - Make weapons and armor unbreakable
 */
public class ItemModifierListener implements Listener {
    private final PvPGame plugin;

    public ItemModifierListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        modifyItem(item, event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        // Schedule modification after the click event completes
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            modifyItem(item, player);

            // Also check the cursor item (item being moved)
            ItemStack cursor = event.getCursor();
            if (cursor != null) {
                modifyItem(cursor, player);
            }
        });
    }

    private void modifyItem(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        Material type = item.getType();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        boolean modified = false;

        // Apply team upgrade enchantments first
        String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        if (teamName != null) {
            // Apply weapon enchantment
            if (type.toString().contains("SWORD")) {
                if (plugin.getWeaponUpgradeManager().hasWeaponUpgrade(teamName)) {
                    item.removeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS);
                    item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 1);
                    modified = true;
                }
            }

            // Apply armor enchantment
            if (type.toString().contains("HELMET") ||
                type.toString().contains("CHESTPLATE") ||
                type.toString().contains("LEGGINGS") ||
                type.toString().contains("BOOTS")) {

                int armorLevel = plugin.getArmorUpgradeManager().getArmorLevel(teamName);
                if (armorLevel > 0) {
                    item.removeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION);
                    item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, armorLevel);
                    modified = true;
                }
            }
        }

        // Make weapons and armor unbreakable
        if (isWeaponOrArmor(type)) {
            meta.setUnbreakable(true);
            modified = true;
        }

        // Set pickaxe and axe attack damage to 1 and slow attack speed
        if (isPickaxe(type) || isAxe(type)) {
            try {
                Attribute attackDamageAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.attack_damage"));
                Attribute attackSpeedAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.attack_speed"));

                // Remove existing modifiers
                meta.removeAttributeModifier(attackDamageAttr);
                meta.removeAttributeModifier(attackSpeedAttr);

                // Add new attack damage modifier with 1 damage (0.5 hearts)
                AttributeModifier damageModifier = new AttributeModifier(
                    NamespacedKey.fromString("myplg:attack_damage_modifier"),
                    -100.0,  // Large negative to ensure minimum damage
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.HAND
                );
                meta.addAttributeModifier(attackDamageAttr, damageModifier);

                // Add very slow attack speed (much slower than default)
                AttributeModifier speedModifier = new AttributeModifier(
                    NamespacedKey.fromString("myplg:attack_speed_modifier"),
                    -3.5,  // Very slow attack speed
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.HAND
                );
                meta.addAttributeModifier(attackSpeedAttr, speedModifier);

                modified = true;
            } catch (IllegalArgumentException e) {
                // Attribute not available in this version, ignore
            }
        }

        if (modified) {
            item.setItemMeta(meta);
        }
    }

    private boolean isWeaponOrArmor(Material material) {
        String name = material.toString();
        return name.contains("SWORD") || name.contains("BOW") ||
               name.contains("HELMET") || name.contains("CHESTPLATE") ||
               name.contains("LEGGINGS") || name.contains("BOOTS");
    }

    private boolean isPickaxe(Material material) {
        return material.toString().endsWith("_PICKAXE");
    }

    private boolean isAxe(Material material) {
        return material.toString().endsWith("_AXE");
    }
}
