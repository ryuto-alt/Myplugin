package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.gui.ShopConfigGUI;
import myplg.myplg.gui.ShopGUI;
import myplg.myplg.gui.ShopTeamSelectGUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopVillagerListener implements Listener {
    private final PvPGame plugin;
    private final ShopGUI shopGUI;
    private final ShopTeamSelectGUI teamSelectGUI;
    private final ShopConfigGUI configGUI;
    private final NamespacedKey shopTypeKey;
    private final NamespacedKey shopTeamKey;
    private final Map<UUID, UUID> pendingVillagers; // Player UUID -> Villager UUID

    public ShopVillagerListener(PvPGame plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin);
        this.teamSelectGUI = new ShopTeamSelectGUI(plugin);
        this.configGUI = new ShopConfigGUI(plugin);
        this.shopTypeKey = new NamespacedKey(plugin, "shop_type");
        this.shopTeamKey = new NamespacedKey(plugin, "shop_team");
        this.pendingVillagers = new HashMap<>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.VILLAGER_SPAWN_EGG) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();

                // Check if it's a shop villager spawn egg
                if (displayName.contains("ショップ")) {
                    // Get player's yaw at the time of spawning
                    final float playerYaw = player.getLocation().getYaw();

                    // Schedule villager customization after spawn
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        // Find the newly spawned villager near the player
                        player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5).forEach(entity -> {
                            if (entity.getType() == EntityType.VILLAGER) {
                                Villager villager = (Villager) entity;

                                // Check if this villager doesn't have a shop type yet
                                if (!villager.getPersistentDataContainer().has(shopTypeKey, PersistentDataType.STRING)) {
                                    // Set basic properties
                                    villager.setCustomName("§a§lショップ");
                                    villager.setCustomNameVisible(true);
                                    villager.setAI(false);
                                    villager.setInvulnerable(true);
                                    villager.setSilent(true);

                                    // Set villager to face the same direction as the player
                                    Location loc = villager.getLocation();
                                    loc.setYaw(playerYaw);  // Use player's yaw
                                    loc.setPitch(0f); // Look straight ahead
                                    villager.teleport(loc);

                                    // Store temporary shop type (team not set yet)
                                    villager.getPersistentDataContainer().set(shopTypeKey, PersistentDataType.STRING, "shop1");

                                    // Store pending villager and open team selection GUI
                                    pendingVillagers.put(player.getUniqueId(), villager.getUniqueId());
                                    teamSelectGUI.openTeamSelectGUI(player, villager);

                                    plugin.getLogger().info("Shop villager created, awaiting team selection: " + villager.getUniqueId());
                                }
                            }
                        });
                    }, 1L);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() != EntityType.VILLAGER) {
            return;
        }

        Villager villager = (Villager) event.getRightClicked();
        Player player = event.getPlayer();

        // Check if this villager is a shop villager
        if (villager.getPersistentDataContainer().has(shopTypeKey, PersistentDataType.STRING)) {
            event.setCancelled(true);

            String shopType = villager.getPersistentDataContainer().get(shopTypeKey, PersistentDataType.STRING);
            String shopTeam = villager.getPersistentDataContainer().get(shopTeamKey, PersistentDataType.STRING);

            // Check if player is sneaking (Shift + Right Click) -> Open config GUI
            if (player.isSneaking()) {
                // Open config GUI
                configGUI.openConfigGUI(player, villager, shopTeam);
            } else {
                // Normal right click -> Open shop GUI
                if (shopTeam == null) {
                    player.sendMessage("§c§lエラー: このショップはまだチームが設定されていません！");
                    player.sendMessage("§7Shift + 右クリックで設定してください。");
                    return;
                }

                if ("shop1".equals(shopType)) {
                    shopGUI.openMainShop(player);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Prevent shop villagers from being damaged
        if (event.getEntity().getType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getEntity();
            if (villager.getPersistentDataContainer().has(shopTypeKey, PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }

    public void setVillagerTeam(Villager villager, String teamName) {
        villager.getPersistentDataContainer().set(shopTeamKey, PersistentDataType.STRING, teamName);

        // Save to config
        plugin.getShopDataManager().saveShopVillager(
            villager.getPersistentDataContainer().get(shopTypeKey, PersistentDataType.STRING),
            villager.getUniqueId(),
            villager.getLocation(),
            teamName
        );

        // Update custom name with team
        villager.setCustomName("§a§lショップ §7(" + teamName + ")");
    }

    public ShopTeamSelectGUI getTeamSelectGUI() {
        return teamSelectGUI;
    }

    public ShopConfigGUI getConfigGUI() {
        return configGUI;
    }
}
