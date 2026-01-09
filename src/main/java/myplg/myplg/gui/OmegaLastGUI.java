package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OmegaLastGUI {
    private final PvPGame plugin;

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

    public OmegaLastGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    public void openTeamSelectionGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c§lΩ-LAST - 敵チーム選択");

        // Fill with red glass panes for dramatic effect
        ItemStack redPane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta redMeta = redPane.getItemMeta();
        if (redMeta != null) {
            redMeta.setDisplayName(" ");
            redPane.setItemMeta(redMeta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, redPane);
        }

        // Get player's team to exclude it
        String playerTeamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

        // Add enemy teams
        int slot = 10;
        for (Map.Entry<String, Team> entry : plugin.getGameManager().getTeams().entrySet()) {
            String teamName = entry.getKey();
            Team team = entry.getValue();

            // Skip player's own team
            if (teamName.equals(playerTeamName)) {
                continue;
            }

            // Skip teams without beds
            if (team.getBedBlock() == null) {
                continue;
            }

            // Skip eliminated teams (no members)
            if (team.getMembers().isEmpty()) {
                continue;
            }

            Material woolType = TEAM_WOOL_MAP.getOrDefault(teamName, Material.WHITE_WOOL);
            ItemStack teamItem = new ItemStack(woolType);
            ItemMeta teamMeta = teamItem.getItemMeta();
            if (teamMeta != null) {
                teamMeta.setDisplayName("§e§l" + teamName);
                teamMeta.setLore(Arrays.asList(
                    "§7このチームのベッドに突撃する",
                    "",
                    "§cクリックして選択"
                ));
                teamItem.setItemMeta(teamMeta);
            }
            inv.setItem(slot, teamItem);
            slot++;

            // Only show up to 7 teams
            if (slot > 16) {
                break;
            }
        }

        // Cancel button
        ItemStack cancelButton = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§c§lキャンセル");
            cancelMeta.setLore(Arrays.asList(
                "§7GUIを閉じる"
            ));
            cancelButton.setItemMeta(cancelMeta);
        }
        inv.setItem(22, cancelButton);

        player.openInventory(inv);
    }
}
