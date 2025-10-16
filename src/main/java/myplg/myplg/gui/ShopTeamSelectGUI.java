package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopTeamSelectGUI {
    private final PvPGame plugin;
    private final Map<UUID, UUID> pendingVillagers; // Player UUID -> Entity UUID (Villager or Skeleton)

    // Team name to wool color mapping
    private static final Map<String, Material> TEAM_WOOL_MAP = new HashMap<>();

    static {
        TEAM_WOOL_MAP.put("アクア", Material.CYAN_WOOL);
        TEAM_WOOL_MAP.put("イエロー", Material.YELLOW_WOOL);
        TEAM_WOOL_MAP.put("ブルー", Material.BLUE_WOOL);
        TEAM_WOOL_MAP.put("ホワイト", Material.WHITE_WOOL);
        TEAM_WOOL_MAP.put("グレー", Material.GRAY_WOOL);
        TEAM_WOOL_MAP.put("ピンク", Material.PINK_WOOL);
        TEAM_WOOL_MAP.put("グリーン", Material.GREEN_WOOL);
        TEAM_WOOL_MAP.put("レッド", Material.RED_WOOL);
    }

    public ShopTeamSelectGUI(PvPGame plugin) {
        this.plugin = plugin;
        this.pendingVillagers = new HashMap<>();
    }

    public void openTeamSelectGUI(Player player, Entity entity) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lショップのチームを選択");

        Map<String, Team> teams = plugin.getGameManager().getTeams();
        int slot = 10;

        for (Team team : teams.values()) {
            Material woolType = TEAM_WOOL_MAP.getOrDefault(team.getName(), Material.WHITE_WOOL);

            ItemStack teamItem = new ItemStack(woolType);
            ItemMeta meta = teamItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e§l" + team.getName());
                meta.setLore(Arrays.asList(
                    "§7このショップを" + team.getName() + "チームに設定",
                    "",
                    "§aクリックして選択"
                ));
                teamItem.setItemMeta(meta);
            }

            inv.setItem(slot, teamItem);
            slot++;

            // Organize in rows
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        // Store pending entity (villager or skeleton)
        pendingVillagers.put(player.getUniqueId(), entity.getUniqueId());

        player.openInventory(inv);
    }

    public UUID getPendingVillager(UUID playerUUID) {
        return pendingVillagers.get(playerUUID);
    }

    public void removePendingVillager(UUID playerUUID) {
        pendingVillagers.remove(playerUUID);
    }

    public boolean hasPendingVillager(UUID playerUUID) {
        return pendingVillagers.containsKey(playerUUID);
    }
}
