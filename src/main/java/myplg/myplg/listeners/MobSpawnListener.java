package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class MobSpawnListener implements Listener {
    private final PvPGame plugin;
    private String pendingGolemTeam = null; // Store team for next golem spawn
    private UUID pendingGolemOwner = null; // Store owner UUID for next golem spawn

    // プレイヤーごとのゴーレム数管理
    private static final int MAX_GOLEMS_PER_PLAYER = 5;
    private static final int GOLEM_DESPAWN_SECONDS = 6 * 60; // 6分 = 360秒
    private final Map<UUID, List<UUID>> playerGolems = new HashMap<>(); // Player UUID -> Golem UUIDs

    public MobSpawnListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * プレイヤーがスポーンできるゴーレム数を確認
     */
    public int getPlayerGolemCount(UUID playerUUID) {
        cleanupDeadGolems(playerUUID);
        List<UUID> golems = playerGolems.get(playerUUID);
        return golems == null ? 0 : golems.size();
    }

    /**
     * プレイヤーがさらにゴーレムをスポーンできるか確認
     */
    public boolean canSpawnGolem(UUID playerUUID) {
        return getPlayerGolemCount(playerUUID) < MAX_GOLEMS_PER_PLAYER;
    }

    /**
     * 死んでいるゴーレムをリストから削除
     */
    private void cleanupDeadGolems(UUID playerUUID) {
        List<UUID> golems = playerGolems.get(playerUUID);
        if (golems == null) return;

        golems.removeIf(golemUUID -> {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(golemUUID);
            return entity == null || entity.isDead();
        });
    }

    /**
     * ゲーム終了時にリセット
     */
    public void reset() {
        playerGolems.clear();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player is using an Iron Golem spawn egg
        if (item != null && item.getType() == Material.IRON_GOLEM_SPAWN_EGG) {
            // ゴーレム数制限チェック
            if (!canSpawnGolem(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage("§c§lゴーレム上限！ §7最大" + MAX_GOLEMS_PER_PLAYER + "体までスポーンできます。");
                player.sendMessage("§7現在のゴーレム数: " + getPlayerGolemCount(player.getUniqueId()) + "/" + MAX_GOLEMS_PER_PLAYER);
                return;
            }

            // Get player's team
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
            if (teamName != null) {
                // Store team name and owner for the next golem spawn
                pendingGolemTeam = teamName;
                pendingGolemOwner = player.getUniqueId();
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Allow only mobs spawned by player actions
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

        switch (reason) {
            // Allow player-spawned mobs
            case SPAWNER_EGG:
            case DISPENSE_EGG:
            case BREEDING:
            case BUILD_SNOWMAN:
            case BUILD_IRONGOLEM:
            case BUILD_WITHER:
            case CURED:
            case CUSTOM:
                // Allow these spawn reasons

                // Special handling for Iron Golem from spawn egg
                if (event.getEntity() instanceof IronGolem &&
                    (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG ||
                     reason == CreatureSpawnEvent.SpawnReason.DISPENSE_EGG)) {

                    IronGolem golem = (IronGolem) event.getEntity();

                    // Apply pending team metadata
                    if (pendingGolemTeam != null && pendingGolemOwner != null) {
                        // Get team color code
                        String colorCode = getTeamColorCode(pendingGolemTeam);
                        golem.setCustomName(colorCode + pendingGolemTeam + "のゴーレム");
                        golem.setCustomNameVisible(true);
                        golem.setPlayerCreated(false);
                        golem.setPersistent(true);
                        golem.setMetadata("ownerTeam", new FixedMetadataValue(plugin, pendingGolemTeam));
                        golem.setMetadata("ownerPlayer", new FixedMetadataValue(plugin, pendingGolemOwner.toString()));

                        // Save spawn location for range limiting
                        Location spawnLoc = golem.getLocation();
                        golem.setMetadata("spawnX", new FixedMetadataValue(plugin, spawnLoc.getX()));
                        golem.setMetadata("spawnY", new FixedMetadataValue(plugin, spawnLoc.getY()));
                        golem.setMetadata("spawnZ", new FixedMetadataValue(plugin, spawnLoc.getZ()));

                        // Set golem attack damage to 8 (4 hearts)
                        org.bukkit.attribute.AttributeInstance attackAttribute = golem.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
                        if (attackAttribute != null) {
                            attackAttribute.setBaseValue(8.0);
                        }

                        // Set golem max health to 83 (default 100, reduced by ~17%)
                        org.bukkit.attribute.AttributeInstance healthAttribute = golem.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        if (healthAttribute != null) {
                            healthAttribute.setBaseValue(32.0);
                            golem.setHealth(32.0);
                        }

                        // プレイヤーのゴーレムリストに追加
                        UUID ownerUUID = pendingGolemOwner;
                        UUID golemUUID = golem.getUniqueId();
                        playerGolems.computeIfAbsent(ownerUUID, k -> new ArrayList<>()).add(golemUUID);

                        // 6分後にデスポーンするタイマーを設定
                        final String teamName = pendingGolemTeam;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            org.bukkit.entity.Entity entity = Bukkit.getEntity(golemUUID);
                            if (entity != null && !entity.isDead()) {
                                // パーティクルと音を再生
                                entity.getWorld().spawnParticle(
                                    org.bukkit.Particle.CLOUD,
                                    entity.getLocation().add(0, 1, 0),
                                    30, 0.5, 0.5, 0.5, 0.1
                                );
                                entity.getWorld().playSound(
                                    entity.getLocation(),
                                    org.bukkit.Sound.ENTITY_IRON_GOLEM_DEATH,
                                    1.0f, 1.0f
                                );
                                entity.remove();
                                plugin.getLogger().info("Iron Golem despawned after 6 minutes (team: " + teamName + ")");
                            }
                            // リストからも削除
                            List<UUID> ownerGolems = playerGolems.get(ownerUUID);
                            if (ownerGolems != null) {
                                ownerGolems.remove(golemUUID);
                            }
                        }, GOLEM_DESPAWN_SECONDS * 20L); // 秒 * 20 = ticks

                        plugin.getLogger().info("Iron Golem spawned for team: " + pendingGolemTeam +
                            " (owner: " + Bukkit.getOfflinePlayer(pendingGolemOwner).getName() +
                            ", will despawn in 6 minutes)");

                        // Clear pending data
                        pendingGolemTeam = null;
                        pendingGolemOwner = null;
                    }
                }
                break;

            // Cancel all other spawn reasons (natural spawning)
            default:
                event.setCancelled(true);
                break;
        }
    }
    
    /**
     * Get team color code for golem name display
     */
    private String getTeamColorCode(String teamName) {
        switch (teamName) {
            case "レッド": return "§c";
            case "ブルー": return "§9";
            case "グリーン": return "§a";
            case "イエロー": return "§e";
            case "アクア": return "§b";
            case "ホワイト": return "§f";
            case "ピンク": return "§d";
            case "グレー": return "§7";
            default: return "§f";
        }
    }
}
