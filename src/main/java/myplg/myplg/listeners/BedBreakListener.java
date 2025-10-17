package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

public class BedBreakListener implements Listener {
    private final PvPGame plugin;
    private final Random random;

    public BedBreakListener(PvPGame plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBedBreak(BlockBreakEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Block block = event.getBlock();
        Material blockType = block.getType();

        // Check if it's a bed
        if (!isBed(blockType)) {
            return;
        }

        // Mark that this event has been handled by bed listener
        event.setCancelled(false);

        Player player = event.getPlayer();
        String playerTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

        // Find which team's bed this is
        for (Team team : plugin.getGameManager().getTeams().values()) {
            Block teamBedBlock = team.getBedBlock();
            if (teamBedBlock == null) continue;

            // Check if this bed belongs to this team
            if (isSameBed(block, teamBedBlock)) {
                // Check if player is breaking their own team's bed
                if (team.getName().equals(playerTeam)) {
                    // Cancel - player cannot break their own bed
                    event.setCancelled(true);
                    // Restore the bed block immediately (no message)
                    return;
                }

                // Enemy team breaking bed - allow it but prevent drops
                event.setDropItems(false);

                // Mark bed as destroyed
                plugin.getScoreboardManager().setBedStatus(team.getName(), false);

                // Get team colors
                String attackerColor = getTeamColor(playerTeam);
                String victimColor = getTeamColor(team.getName());

                // Check for victory after bed destruction
                plugin.getPlayerDeathListener().checkVictoryCondition();

                // Notify all players with sounds
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    String onlinePlayerTeam = plugin.getGameManager().getPlayerTeam(onlinePlayer.getUniqueId());

                    if (team.getName().equals(onlinePlayerTeam)) {
                        // Show title to destroyed team members
                        Component title = Component.text("§c§lベッドが破壊されました！");
                        Component subtitle = Component.text("§7もうリスポーンできません！");

                        Title titleDisplay = Title.title(
                            title,
                            subtitle,
                            Title.Times.times(
                                Duration.ofMillis(500),  // fade in
                                Duration.ofMillis(3000), // stay
                                Duration.ofMillis(1000)  // fade out
                            )
                        );
                        onlinePlayer.showTitle(titleDisplay);

                        // Also send chat message
                        onlinePlayer.sendMessage("§c§l⚠ あなたのチームのベッドが破壊されました！");

                        // Play wither death sound for destroyed team
                        onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
                    } else {
                        // Send colored chat message to other teams
                        onlinePlayer.sendMessage(
                            attackerColor + playerTeam + "チーム§fの" +
                            attackerColor + player.getName() + "§fが" +
                            victimColor + team.getName() + "チーム§fのベッドを破壊！"
                        );

                        // Play random ender dragon sound for other teams
                        playRandomEnderDragonSound(onlinePlayer);
                    }
                }

                return;
            }
        }
    }

    // Protect beds from explosions
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        event.blockList().removeIf(block -> {
            if (isBed(block.getType())) {
                // Check if this is a team bed
                for (Team team : plugin.getGameManager().getTeams().values()) {
                    Block teamBedBlock = team.getBedBlock();
                    if (teamBedBlock != null && isSameBed(block, teamBedBlock)) {
                        return true; // Remove from explosion list (protect bed)
                    }
                }
            }
            return false;
        });
    }

    // Protect beds from block explosions (TNT, etc.)
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        event.blockList().removeIf(block -> {
            if (isBed(block.getType())) {
                // Check if this is a team bed
                for (Team team : plugin.getGameManager().getTeams().values()) {
                    Block teamBedBlock = team.getBedBlock();
                    if (teamBedBlock != null && isSameBed(block, teamBedBlock)) {
                        return true; // Remove from explosion list (protect bed)
                    }
                }
            }
            return false;
        });
    }

    private boolean isBed(Material material) {
        return material.name().contains("BED") && !material.name().equals("BEDROCK");
    }

    private boolean isSameBed(Block block1, Block block2) {
        // Check if blocks are part of the same bed
        // Compare locations (accounting for head/foot parts)
        if (block1.getLocation().equals(block2.getLocation())) {
            return true;
        }

        // Check adjacent blocks for bed parts
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block adjacent = block1.getRelative(x, 0, z);
                if (adjacent.getLocation().equals(block2.getLocation()) && isBed(adjacent.getType())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void playRandomEnderDragonSound(Player player) {
        // Play ender dragon growl sound
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
    }

    private String getTeamColor(String teamName) {
        if (teamName == null) return "§f";

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
