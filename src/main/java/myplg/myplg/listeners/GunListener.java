package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class GunListener implements Listener {
    private final PvPGame plugin;

    // Gun settings
    private static final double HEADSHOT_DAMAGE = 11.0;
    private static final double BODY_DAMAGE = 7.0;
    private static final long COOLDOWN_TICKS = 60L; // 3 seconds
    private static final double MAX_RANGE = 100.0;
    private static final double BULLET_SPEED = 5.0; // blocks per tick

    // Player cooldowns
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> gunHolders = new HashSet<>(); // Players currently holding gun (have slowness)

    public GunListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if an item is a gun
     */
    public static boolean isGun(ItemStack item) {
        if (item == null || item.getType() != Material.CROSSBOW) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().equals("§6§lスナイパーライフル");
    }

    /**
     * Check if an item is ammo
     */
    public static boolean isAmmo(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_NUGGET) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().equals("§e弾薬");
    }

    /**
     * Create a gun item
     */
    public static ItemStack createGun() {
        ItemStack gun = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = gun.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lスナイパーライフル");
            meta.setLore(Arrays.asList(
                "§7高精度の狙撃銃",
                "",
                "§6ダメージ:",
                "§c・ヘッドショット: §f11",
                "§e・胴体: §f7",
                "",
                "§7左クリック: 射撃",
                "§7クールダウン: 3秒",
                "§8※持っている間鈍足"
            ));
            gun.setItemMeta(meta);
        }
        return gun;
    }

    /**
     * Create ammo item
     */
    public static ItemStack createAmmo(int amount) {
        ItemStack ammo = new ItemStack(Material.IRON_NUGGET, amount);
        ItemMeta meta = ammo.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e弾薬");
            meta.setLore(Arrays.asList(
                "§7スナイパーライフル用の弾薬",
                "§71発につき1個消費"
            ));
            ammo.setItemMeta(meta);
        }
        return ammo;
    }

    /**
     * Handle left click to shoot
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isGun(item)) return;

        // Prevent crossbow default behavior
        event.setCancelled(true);

        // Left click - shoot
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            shoot(player);
        }
    }

    /**
     * Cancel crossbow shooting
     */
    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack bow = event.getBow();
        if (isGun(bow)) {
            event.setCancelled(true);
        }
    }

    /**
     * Update slowness state for a player based on what they're holding
     */
    private void updateSlownessState(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (isGun(mainHand)) {
            // Holding gun - apply slowness
            if (!gunHolders.contains(uuid)) {
                gunHolders.add(uuid);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1, false, false, false));
            }
        } else {
            // Not holding gun - remove slowness
            if (gunHolders.contains(uuid)) {
                gunHolders.remove(uuid);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
    }

    /**
     * Apply/remove slowness when switching items
     */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        // Check the NEW slot item
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        UUID uuid = player.getUniqueId();

        if (isGun(newItem)) {
            if (!gunHolders.contains(uuid)) {
                gunHolders.add(uuid);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1, false, false, false));
            }
        } else {
            if (gunHolders.contains(uuid)) {
                gunHolders.remove(uuid);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
    }

    /**
     * Handle inventory clicks (shift-click moves, etc.)
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Delay check to after the inventory action completes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    updateSlownessState(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Handle inventory close - recheck state
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        // Delay check to ensure inventory state is finalized
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    updateSlownessState(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Handle dropping the gun
     */
    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // Delay check to after the drop completes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    updateSlownessState(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Shoot the gun
     */
    private void shoot(Player player) {
        UUID uuid = player.getUniqueId();

        // Check cooldown
        long currentTime = System.currentTimeMillis();
        Long lastShot = cooldowns.get(uuid);
        if (lastShot != null && currentTime - lastShot < COOLDOWN_TICKS * 50) {
            long remaining = (COOLDOWN_TICKS * 50 - (currentTime - lastShot)) / 1000;
            player.sendMessage("§c§lクールダウン中！ §7あと " + (remaining + 1) + " 秒");
            return;
        }

        // Check ammo
        if (!consumeAmmo(player)) {
            player.sendMessage("§c§l弾薬がありません！");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f);
            return;
        }

        // Set cooldown
        cooldowns.put(uuid, currentTime);

        // Play shoot sound
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.5f);

        // Get shooter team
        String shooterTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

        // Start bullet travel
        Location startLocation = player.getEyeLocation().clone();
        Vector direction = startLocation.getDirection().normalize();

        new BukkitRunnable() {
            double traveled = 0;
            Location bulletPos = startLocation.clone();

            @Override
            public void run() {
                for (double step = 0; step < BULLET_SPEED && traveled < MAX_RANGE; step += 0.5) {
                    bulletPos.add(direction.clone().multiply(0.5));
                    traveled += 0.5;

                    // Spawn trail particle
                    bulletPos.getWorld().spawnParticle(Particle.CRIT, bulletPos, 1, 0, 0, 0, 0);

                    // Check for block collision
                    if (bulletPos.getBlock().getType().isSolid()) {
                        bulletPos.getWorld().spawnParticle(Particle.SMOKE, bulletPos, 5, 0.1, 0.1, 0.1, 0.02);
                        player.sendMessage("§7ミス... (壁に命中)");
                        this.cancel();
                        return;
                    }

                    // Check for entity collision
                    Collection<Entity> nearbyEntities = bulletPos.getWorld().getNearbyEntities(bulletPos, 0.5, 0.5, 0.5);
                    for (Entity entity : nearbyEntities) {
                        if (!(entity instanceof LivingEntity)) continue;
                        if (entity == player) continue;
                        if (entity.isDead()) continue;

                        LivingEntity target = (LivingEntity) entity;

                        // Don't hit teammates
                        if (target instanceof Player) {
                            Player targetPlayer = (Player) target;
                            String targetTeam = plugin.getGameManager().getPlayerTeam(targetPlayer.getUniqueId());
                            if (shooterTeam != null && shooterTeam.equals(targetTeam)) {
                                continue;
                            }
                        }

                        // Hit! Calculate damage (no distance falloff)
                        boolean isHeadshot = isHeadshotFromBullet(bulletPos, target);
                        double baseDamage = isHeadshot ? HEADSHOT_DAMAGE : BODY_DAMAGE;

                        // Apply sniper upgrade multiplier
                        double multiplier = plugin.getSniperUpgradeManager().getDamageMultiplier(player.getUniqueId());
                        double damage = baseDamage * multiplier;

                        // Apply damage without knockback
                        applyDamageNoKnockback(target, damage, player);

                        // Feedback
                        if (isHeadshot) {
                            player.sendMessage("§c§l✦ ヘッドショット！ §7(" + damage + " ダメージ)");
                            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.5f);
                            target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 20, 0.2, 0.2, 0.2, 0.1);
                        } else {
                            player.sendMessage("§e§l✦ ヒット！ §7(" + damage + " ダメージ)");
                            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
                        }

                        this.cancel();
                        return;
                    }
                }

                if (traveled >= MAX_RANGE) {
                    player.sendMessage("§7ミス...");
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Check if bullet hit is a headshot
     */
    private boolean isHeadshotFromBullet(Location bulletPos, LivingEntity target) {
        double bulletY = bulletPos.getY();
        double targetY = target.getLocation().getY();
        double eyeHeight = target.getEyeHeight();
        double relativeHitHeight = bulletY - targetY;
        return relativeHitHeight >= eyeHeight - 0.4;
    }

    /**
     * Apply damage without knockback (armor reduces damage)
     */
    private void applyDamageNoKnockback(LivingEntity target, double damage, Player damager) {
        Vector originalVelocity = target.getVelocity().clone();
        target.damage(damage, damager);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isDead()) {
                    target.setVelocity(originalVelocity);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Consume one ammo from player's inventory
     */
    private boolean consumeAmmo(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isAmmo(item)) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Get ammo count for a player
     */
    public int getAmmoCount(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isAmmo(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Clean up when player leaves
     */
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        if (gunHolders.remove(uuid)) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }
}
