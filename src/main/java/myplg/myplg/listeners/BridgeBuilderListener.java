package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BridgeBuilderListener implements Listener {
    private final PvPGame plugin;
    private static final int MAX_BRIDGE_LENGTH = 25;
    private final Set<UUID> trackedEggs = new HashSet<>();

    public BridgeBuilderListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player right-clicked with Bridge Builder Egg
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
            item != null && item.getType() == Material.EGG && item.hasItemMeta() &&
            item.getItemMeta().hasDisplayName() &&
            item.getItemMeta().getDisplayName().equals("§bBridge Builder Egg")) {

            event.setCancelled(true);

            // Get player's team wool color
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
            Material woolType = Material.WHITE_WOOL;
            if (teamName != null) {
                woolType = getTeamWool(teamName);
            }

            // Remove one egg from player's inventory
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }

            // Remember player's starting location
            Location startLoc = player.getLocation().clone();

            // Launch egg and track its trajectory
            Egg egg = player.launchProjectile(Egg.class);
            egg.setVelocity(player.getLocation().getDirection().multiply(1.5));

            // Store wool type in metadata
            egg.setMetadata("bridgeWool", new FixedMetadataValue(plugin, woolType.name()));
            trackedEggs.add(egg.getUniqueId());

            // Track egg trajectory and place blocks (starts after 0.7s, 2 blocks away)
            trackEggAndBuild(egg, woolType, startLoc);
        }
    }

    private void trackEggAndBuild(Egg egg, Material woolType, Location startLoc) {
        // プレイヤーの向き（水平方向）
        org.bukkit.util.Vector direction = startLoc.getDirection().setY(0).normalize();

        // 最初のブロック：プレイヤーの2ブロック先に置く
        Location firstBlockLoc = startLoc.clone().add(direction.clone().multiply(2));
        firstBlockLoc.setY(startLoc.getBlockY() - 1);
        Block firstBlock = firstBlockLoc.getBlock();

        if (firstBlock.getType() == Material.AIR ||
            firstBlock.getType() == Material.WATER ||
            firstBlock.getType() == Material.LAVA ||
            firstBlock.getType() == Material.CAVE_AIR ||
            firstBlock.getType() == Material.VOID_AIR) {

            firstBlock.setType(woolType);
            BlockPlaceListener.addPlayerPlacedBlock(firstBlock);
            firstBlock.getWorld().playSound(firstBlock.getLocation(),
                org.bukkit.Sound.BLOCK_WOOL_PLACE, 0.5f, 1.0f);
        }

        // その後は卵の軌道を追跡
        new BukkitRunnable() {
            int blocksPlaced = 1; // 最初の1ブロックはカウント済み
            Location lastBlockLoc = firstBlockLoc.clone();

            @Override
            public void run() {
                // Stop if egg is dead or max blocks reached
                if (egg.isDead() || !egg.isValid() || blocksPlaced >= MAX_BRIDGE_LENGTH) {
                    trackedEggs.remove(egg.getUniqueId());
                    this.cancel();
                    return;
                }

                // 卵の現在位置
                Location eggLoc = egg.getLocation();

                // 卵が開始位置より5ブロック以上下に落ちたら終了
                if (eggLoc.getY() < startLoc.getY() - 5) {
                    trackedEggs.remove(egg.getUniqueId());
                    this.cancel();
                    return;
                }

                // 卵の1ブロック下にブロックを置く（卵の軌道に沿う）
                Block targetBlock = eggLoc.clone().add(0, -1, 0).getBlock();

                // 同じブロックには置かない
                if (targetBlock.getX() == lastBlockLoc.getBlockX() &&
                    targetBlock.getY() == lastBlockLoc.getBlockY() &&
                    targetBlock.getZ() == lastBlockLoc.getBlockZ()) {
                    return;
                }

                // Only place if the block is air or replaceable
                if (targetBlock.getType() == Material.AIR ||
                    targetBlock.getType() == Material.WATER ||
                    targetBlock.getType() == Material.LAVA ||
                    targetBlock.getType() == Material.CAVE_AIR ||
                    targetBlock.getType() == Material.VOID_AIR) {

                    targetBlock.setType(woolType);
                    BlockPlaceListener.addPlayerPlacedBlock(targetBlock);
                    targetBlock.getWorld().playSound(targetBlock.getLocation(),
                        org.bukkit.Sound.BLOCK_WOOL_PLACE, 0.5f, 1.0f);

                    blocksPlaced++;
                    lastBlockLoc = targetBlock.getLocation();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // すぐ開始、毎tick実行
    }

    @EventHandler
    public void onEggHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Egg) {
            Egg egg = (Egg) event.getEntity();
            if (trackedEggs.contains(egg.getUniqueId())) {
                // Remove from tracking
                trackedEggs.remove(egg.getUniqueId());
                // Cancel the egg hit event to prevent chicken spawning
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEggThrow(PlayerEggThrowEvent event) {
        Egg egg = event.getEgg();
        if (trackedEggs.contains(egg.getUniqueId())) {
            // Prevent chicken from spawning
            event.setHatching(false);
        }
    }

    private Material getTeamWool(String teamName) {
        switch (teamName) {
            case "アクア": return Material.CYAN_WOOL;
            case "イエロー": return Material.YELLOW_WOOL;
            case "ブルー": return Material.BLUE_WOOL;
            case "ホワイト": return Material.WHITE_WOOL;
            case "グレー": return Material.GRAY_WOOL;
            case "ピンク": return Material.PINK_WOOL;
            case "グリーン": return Material.GREEN_WOOL;
            case "レッド": return Material.RED_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }
}
