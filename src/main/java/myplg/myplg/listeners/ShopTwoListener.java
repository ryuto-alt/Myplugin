package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.gui.ShopConfigGUI;
import myplg.myplg.gui.ShopTeamSelectGUI;
import myplg.myplg.gui.ShopTwoGUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
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

public class ShopTwoListener implements Listener {
    private final PvPGame plugin;
    private final ShopTwoGUI shopTwoGUI;
    private final ShopTeamSelectGUI teamSelectGUI;
    private final ShopConfigGUI configGUI;
    private final NamespacedKey shopTypeKey;
    private final NamespacedKey shopTeamKey;
    private final Map<UUID, UUID> pendingSkeletons; // Player UUID -> Skeleton UUID

    public ShopTwoListener(PvPGame plugin) {
        this.plugin = plugin;
        this.shopTwoGUI = new ShopTwoGUI(plugin);
        this.teamSelectGUI = new ShopTeamSelectGUI(plugin);
        this.configGUI = new ShopConfigGUI(plugin);
        this.shopTypeKey = new NamespacedKey(plugin, "shop_type");
        this.shopTeamKey = new NamespacedKey(plugin, "shop_team");
        this.pendingSkeletons = new HashMap<>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.SKELETON_SPAWN_EGG) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();

                // Check if it's an upgrade skeleton spawn egg
                if (displayName.contains("アップグレード")) {
                    // Get player's yaw at the time of spawning
                    final float playerYaw = player.getLocation().getYaw();

                    // Schedule skeleton customization after spawn
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        // Find the newly spawned skeleton near the player
                        player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5).forEach(entity -> {
                            if (entity.getType() == EntityType.SKELETON) {
                                Skeleton skeleton = (Skeleton) entity;

                                // Check if this skeleton doesn't have a shop type yet
                                if (!skeleton.getPersistentDataContainer().has(shopTypeKey, PersistentDataType.STRING)) {
                                    // Set basic properties
                                    skeleton.setCustomName("§6§lアップグレード");
                                    skeleton.setCustomNameVisible(true);
                                    skeleton.setAI(false);
                                    skeleton.setInvulnerable(true);
                                    skeleton.setSilent(true);
                                    skeleton.setRemoveWhenFarAway(false); // Prevent despawning

                                    // Set skeleton to face the same direction as the player
                                    Location loc = skeleton.getLocation();
                                    loc.setYaw(playerYaw + 180f);  // Add 180 degrees to face the same direction
                                    loc.setPitch(0f); // Look straight ahead
                                    skeleton.teleport(loc);

                                    // Store temporary shop type (team not set yet)
                                    skeleton.getPersistentDataContainer().set(shopTypeKey, PersistentDataType.STRING, "shop2");

                                    // Store pending skeleton and open team selection GUI
                                    pendingSkeletons.put(player.getUniqueId(), skeleton.getUniqueId());
                                    teamSelectGUI.openTeamSelectGUI(player, skeleton);

                                    plugin.getLogger().info("Shop 2 skeleton created, awaiting team selection: " + skeleton.getUniqueId());
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
        if (event.getRightClicked().getType() != EntityType.SKELETON) {
            return;
        }

        Skeleton skeleton = (Skeleton) event.getRightClicked();
        Player player = event.getPlayer();

        // Check if this skeleton is a shop 2 skeleton
        if (skeleton.getPersistentDataContainer().has(shopTypeKey, PersistentDataType.STRING)) {
            String shopType = skeleton.getPersistentDataContainer().get(shopTypeKey, PersistentDataType.STRING);

            // Only handle shop2 type
            if (!"shop2".equals(shopType)) {
                return;
            }

            event.setCancelled(true);

            String shopTeam = skeleton.getPersistentDataContainer().get(shopTeamKey, PersistentDataType.STRING);

            // Check if player is sneaking (Shift + Right Click) -> Open config GUI
            if (player.isSneaking()) {
                // Open config GUI
                configGUI.openConfigGUI(player, skeleton, shopTeam);
            } else {
                // Normal right click -> Open shop GUI
                if (shopTeam == null) {
                    player.sendMessage("§c§lエラー: このアップグレードショップはまだチームが設定されていません！");
                    player.sendMessage("§7Shift + 右クリックで設定してください。");
                    return;
                }

                shopTwoGUI.openMainShop(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Prevent shop 2 skeletons from being damaged
        if (event.getEntity().getType() == EntityType.SKELETON) {
            Skeleton skeleton = (Skeleton) event.getEntity();
            if (skeleton.getPersistentDataContainer().has(shopTypeKey, PersistentDataType.STRING)) {
                String shopType = skeleton.getPersistentDataContainer().get(shopTypeKey, PersistentDataType.STRING);
                if ("shop2".equals(shopType)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public void setSkeletonTeam(Skeleton skeleton, String teamName) {
        skeleton.getPersistentDataContainer().set(shopTeamKey, PersistentDataType.STRING, teamName);

        // Save to config
        plugin.getShopDataManager().saveShopVillager(
            skeleton.getPersistentDataContainer().get(shopTypeKey, PersistentDataType.STRING),
            skeleton.getUniqueId(),
            skeleton.getLocation(),
            teamName
        );

        // Update custom name with team
        skeleton.setCustomName("§6§lアップグレード §7(" + teamName + ")");
    }

    public ShopTeamSelectGUI getTeamSelectGUI() {
        return teamSelectGUI;
    }

    public ShopConfigGUI getConfigGUI() {
        return configGUI;
    }
}
