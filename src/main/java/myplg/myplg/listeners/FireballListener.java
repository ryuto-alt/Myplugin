package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FireballListener implements Listener {
    private final PvPGame plugin;
    private final Map<UUID, Long> fireballCooldown;
    private final Map<UUID, Long> fireballKnockbackTime; // Track when players were knocked back by fireball
    private static final long COOLDOWN_MS = 700; // 0.7秒のクールダウン
    private static final long FALL_DAMAGE_IMMUNITY_MS = 5000; // 5秒間落下ダメージ無効

    public FireballListener(PvPGame plugin) {
        this.plugin = plugin;
        this.fireballCooldown = new HashMap<>();
        this.fireballKnockbackTime = new HashMap<>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player right-clicked with a fire charge
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
            item != null && item.getType() == Material.FIRE_CHARGE &&
            item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
            item.getItemMeta().getDisplayName().contains("火玉")) {

            event.setCancelled(true);

            // Check cooldown
            long currentTime = System.currentTimeMillis();
            Long lastUseTime = fireballCooldown.get(player.getUniqueId());

            if (lastUseTime != null && (currentTime - lastUseTime) < COOLDOWN_MS) {
                return; // Still in cooldown
            }

            // Remove one fire charge from player's hand
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }

            // Launch fireball from player's eye location
            Vector direction = player.getEyeLocation().getDirection();
            Fireball fireball = player.getWorld().spawn(
                player.getEyeLocation().add(direction.multiply(1.5)),
                Fireball.class
            );

            // Set fireball properties
            fireball.setShooter(player);
            fireball.setVelocity(direction.multiply(0.24)); // 0.16 * 1.5 = 0.24 (1.5x faster deflection)
            fireball.setYield(1.62f); // 1.5 * 1.08 = 1.62 (8% larger explosion radius)
            fireball.setIsIncendiary(false); // Don't set blocks on fire

            // Update cooldown
            fireballCooldown.put(player.getUniqueId(), currentTime);

            // Play sound
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onFireballDamage(EntityDamageByEntityEvent event) {
        // Check if damage is from a fireball
        if (event.getDamager() instanceof Fireball && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Fireball fireball = (Fireball) event.getDamager();

            // Check if player hit themselves
            boolean isSelfDamage = false;
            if (fireball.getShooter() instanceof Player) {
                Player shooter = (Player) fireball.getShooter();
                if (shooter.getUniqueId().equals(player.getUniqueId())) {
                    isSelfDamage = true;
                }
            }

            // Set damage based on whether it's self-damage
            if (isSelfDamage) {
                // Self-damage: 8.0 damage (4 hearts)
                event.setDamage(8.0);
            } else {
                // Enemy damage: 3.0 damage (1.5 hearts)
                event.setDamage(3.0);
            }

            // Calculate knockback direction (away from fireball)
            Vector knockbackDirection = player.getLocation().toVector()
                .subtract(fireball.getLocation().toVector())
                .normalize();

            // Apply knockback (reduced by 30% total: 2.5 -> 2.25 -> 2.025 -> 1.8225)
            knockbackDirection.setY(0.4374); // 0.6 * 0.9 * 0.9 * 0.9 = 0.4374 (30% weaker upward)
            knockbackDirection.multiply(1.8225); // 2.5 * 0.9 * 0.9 * 0.9 = 1.8225 (30% weaker horizontal)

            player.setVelocity(knockbackDirection);

            // Only grant fall damage immunity for self-damage
            if (isSelfDamage) {
                fireballKnockbackTime.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onPlayerFallDamage(EntityDamageEvent event) {
        // Only handle fall damage for players
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Long knockbackTime = fireballKnockbackTime.get(player.getUniqueId());

        // If player was recently hit by fireball, completely cancel fall damage
        if (knockbackTime != null) {
            long timeSinceKnockback = System.currentTimeMillis() - knockbackTime;

            if (timeSinceKnockback < FALL_DAMAGE_IMMUNITY_MS) {
                event.setCancelled(true);

                // Clean up if immunity period has ended
                if (timeSinceKnockback >= FALL_DAMAGE_IMMUNITY_MS - 100) {
                    fireballKnockbackTime.remove(player.getUniqueId());
                }
            } else {
                // Clean up expired entry
                fireballKnockbackTime.remove(player.getUniqueId());
            }
        }
    }
}
