package myplg.myplg.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import myplg.myplg.Generator;
import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import myplg.myplg.gui.ManagementGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIClickListener implements Listener {
    private final PvPGame plugin;
    private final ManagementGUI managementGUI;
    private final Map<UUID, String> waitingForRename;
    private final Map<UUID, GeneratorSelection> generatorSelections;

    public GUIClickListener(PvPGame plugin) {
        this.plugin = plugin;
        this.managementGUI = new ManagementGUI(plugin);
        this.waitingForRename = new HashMap<>();
        this.generatorSelections = new HashMap<>();
    }

    private static class GeneratorSelection {
        Material material;
        String teamName;
        int interval; // in ticks
        org.bukkit.Location corner1;
        org.bukkit.Location corner2;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        // Generator type selection menu
        if (title.equals("ジェネレーター作成")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            Material selectedMaterial = clickedItem.getType();
            if (selectedMaterial == Material.DIAMOND || selectedMaterial == Material.GOLD_INGOT ||
                selectedMaterial == Material.IRON_INGOT || selectedMaterial == Material.EMERALD) {

                // Store the selected material temporarily
                GeneratorSelection selection = new GeneratorSelection();
                selection.material = selectedMaterial;
                generatorSelections.put(player.getUniqueId(), selection);

                // Diamond and Emerald skip team selection (共通)
                if (selectedMaterial == Material.DIAMOND || selectedMaterial == Material.EMERALD) {
                    selection.teamName = "共通";
                    managementGUI.openIntervalSelection(player, selectedMaterial, "共通");
                } else {
                    // Iron and Gold need team selection
                    managementGUI.openTeamSelectionForGenerator(player, selectedMaterial);
                }
            }
        }
        // Team selection for generator menu
        else if (title.startsWith("チーム選択 - ")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (clickedItem.getType() == Material.ARROW) {
                // Back button
                managementGUI.openGeneratorTypeSelection(player);
                return;
            }

            if (clickedItem.getType() == Material.WHITE_BANNER) {
                // Team selected
                String teamName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());

                GeneratorSelection selection = generatorSelections.get(player.getUniqueId());
                if (selection != null) {
                    selection.teamName = teamName;
                    // Open interval selection GUI
                    managementGUI.openIntervalSelection(player, selection.material, teamName);
                }
            }
        }
        // Interval selection menu
        else if (title.equals("出現間隔選択")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (clickedItem.getType() == Material.ARROW) {
                // Back button
                GeneratorSelection selection = generatorSelections.get(player.getUniqueId());
                if (selection != null) {
                    if (selection.material == Material.DIAMOND || selection.material == Material.EMERALD) {
                        managementGUI.openGeneratorTypeSelection(player);
                    } else {
                        managementGUI.openTeamSelectionForGenerator(player, selection.material);
                    }
                }
                return;
            }

            // Get selected interval from item name
            String itemName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
            int interval = parseInterval(itemName);

            if (interval > 0) {
                GeneratorSelection selection = generatorSelections.get(player.getUniqueId());
                if (selection != null) {
                    selection.interval = interval;

                    player.closeInventory();
                    player.sendMessage(Component.text("出現間隔: " + (interval / 20.0) + "秒", NamedTextColor.GREEN));
                    player.sendMessage(Component.text("範囲の1つ目の角を右クリックしてください。", NamedTextColor.YELLOW));
                }
            }
        }
        // Main menu
        else if (title.equals("ゲーム管理")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (clickedItem.getType() == Material.WHITE_BED) {
                managementGUI.openTeamList(player);
            } else if (clickedItem.getType() == Material.DROPPER) {
                managementGUI.openGeneratorTeamSelection(player);
            }
        }
        // Team selection for generator management
        else if (title.equals("チーム選択 - ジェネレーター管理")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (clickedItem.getType() == Material.ARROW) {
                // Back button
                managementGUI.openMainMenu(player);
                return;
            }

            if (clickedItem.getType() == Material.NETHER_STAR) {
                // 共通 team selected
                managementGUI.openGeneratorListByTeam(player, "共通");
            } else if (clickedItem.getType() == Material.WHITE_BANNER) {
                // Regular team selected
                String teamName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
                managementGUI.openGeneratorListByTeam(player, teamName);
            }
        }
        // Generator list by team
        else if (title.contains(" - ジェネレーター")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (clickedItem.getType() == Material.ARROW) {
                // Back button
                managementGUI.openGeneratorTeamSelection(player);
                return;
            }

            if (clickedItem.getType() == Material.PAPER) {
                return; // Info item, do nothing
            }

            // Extract team name from title
            String teamName = title.split(" - ")[0];

            // Find the generator by material type and slot
            int slot = event.getSlot();
            int currentSlot = 0;

            for (Generator generator : plugin.getGeneratorManager().getGenerators().values()) {
                if (!generator.getTeamName().equals(teamName)) continue;

                if (currentSlot == slot && generator.getMaterial() == clickedItem.getType()) {
                    handleGeneratorClick(player, generator, event.getClick(), teamName);
                    return;
                }
                currentSlot++;
                if (currentSlot >= 45) break;
            }
        }
        // Team list menu
        else if (title.startsWith("チーム一覧")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (clickedItem.getType() == Material.ARROW) {
                // Back button
                managementGUI.openMainMenu(player);
            } else if (clickedItem.getType() == Material.NAME_TAG) {
                // Team name tag clicked
                String teamName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());

                if (plugin.getGameManager().getTeam(teamName) != null) {
                    waitingForRename.put(player.getUniqueId(), teamName);
                    player.closeInventory();
                    player.sendMessage(Component.text("チーム「" + teamName + "」の新しい名前をチャットで入力してください:", NamedTextColor.YELLOW));
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!waitingForRename.containsKey(playerUUID)) {
            return;
        }

        event.setCancelled(true);

        String oldName = waitingForRename.get(playerUUID);
        String newName = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (plugin.getGameManager().getTeam(newName) != null) {
            player.sendMessage(Component.text("チーム「" + newName + "」は既に存在します。別の名前を入力してください:", NamedTextColor.RED));
            return;
        }

        // Rename the team
        plugin.getGameManager().renameTeam(oldName, newName);
        player.sendMessage(Component.text("チーム名を「" + oldName + "」から「" + newName + "」に変更しました。", NamedTextColor.GREEN));
        waitingForRename.remove(playerUUID);

        // Reopen the GUI
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            managementGUI.openTeamList(player);
        });
    }

    private void handleGeneratorClick(Player player, Generator generator, ClickType clickType, String teamName) {
        if (clickType == ClickType.LEFT) {
            // Decrease interval by 0.5 seconds (10 ticks)
            int newInterval = Math.max(10, generator.getSpawnInterval() - 10);
            plugin.getGeneratorManager().updateGeneratorInterval(generator.getId(), newInterval);
            player.sendMessage(Component.text("出現間隔を" + (newInterval / 20.0) + "秒に変更しました。", NamedTextColor.GREEN));
        } else if (clickType == ClickType.RIGHT) {
            // Increase interval by 0.5 seconds (10 ticks)
            int newInterval = generator.getSpawnInterval() + 10;
            plugin.getGeneratorManager().updateGeneratorInterval(generator.getId(), newInterval);
            player.sendMessage(Component.text("出現間隔を" + (newInterval / 20.0) + "秒に変更しました。", NamedTextColor.GREEN));
        } else if (clickType == ClickType.SHIFT_LEFT) {
            // Delete generator
            plugin.getGeneratorManager().removeGenerator(generator.getId());
            player.sendMessage(Component.text("ジェネレーターを削除しました。", NamedTextColor.RED));
        }

        // Refresh GUI with the same team
        managementGUI.openGeneratorListByTeam(player, teamName);
    }

    public boolean hasGeneratorSelection(UUID playerUUID) {
        return generatorSelections.containsKey(playerUUID);
    }

    public void handleCornerSelection(Player player, org.bukkit.Location location) {
        GeneratorSelection selection = generatorSelections.get(player.getUniqueId());
        if (selection == null) {
            return;
        }

        if (selection.corner1 == null) {
            // First corner
            selection.corner1 = location;
            player.sendMessage(Component.text("1つ目の角を設定しました: ", NamedTextColor.GREEN)
                    .append(Component.text(formatLocation(location), NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("2つ目の角を右クリックしてください。", NamedTextColor.YELLOW));
        } else if (selection.corner2 == null) {
            // Second corner
            selection.corner2 = location;
            completeGeneratorSelection(player);
        }
    }

    public void completeGeneratorSelection(Player player) {
        GeneratorSelection selection = generatorSelections.get(player.getUniqueId());
        if (selection == null || selection.corner1 == null || selection.corner2 == null) {
            player.sendMessage(Component.text("エラー: 選択情報が不完全です。", NamedTextColor.RED));
            return;
        }

        // Check if both corners are in the same world
        if (!selection.corner1.getWorld().equals(selection.corner2.getWorld())) {
            player.sendMessage(Component.text("2つの座標は同じワールド内である必要があります。", NamedTextColor.RED));
            generatorSelections.remove(player.getUniqueId());
            return;
        }

        player.sendMessage(Component.text("2つ目の角を設定しました: ", NamedTextColor.GREEN)
                .append(Component.text(formatLocation(selection.corner2), NamedTextColor.YELLOW)));

        // Create generator with selected interval
        String materialName = getMaterialName(selection.material);
        String generatorId = selection.teamName + "_" + materialName + "_" + System.currentTimeMillis();
        int interval = selection.interval > 0 ? selection.interval : getDefaultInterval(selection.material);

        plugin.getGeneratorManager().addGenerator(generatorId, selection.teamName, selection.material, selection.corner1, selection.corner2, interval);

        player.sendMessage(Component.text("ジェネレーターを作成しました！", NamedTextColor.GREEN));
        player.sendMessage(Component.text("チーム: " + selection.teamName, NamedTextColor.AQUA));
        player.sendMessage(Component.text("アイテム: " + getMaterialDisplayName(selection.material), NamedTextColor.YELLOW));
        player.sendMessage(Component.text("出現間隔: " + (interval / 20.0) + "秒", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/edit で出現頻度を調整できます。", NamedTextColor.GRAY));

        // Clear selection
        generatorSelections.remove(player.getUniqueId());
    }

    private int parseInterval(String itemName) {
        // Extract number from item name (e.g., "1秒" -> 20 ticks)
        if (itemName.contains("1秒")) {
            return 20; // 1 second
        } else if (itemName.contains("3秒")) {
            return 60; // 3 seconds
        } else if (itemName.contains("5秒")) {
            return 100; // 5 seconds
        } else if (itemName.contains("10秒")) {
            return 200; // 10 seconds
        } else if (itemName.contains("15秒")) {
            return 300; // 15 seconds
        }
        return 0;
    }

    private String formatLocation(org.bukkit.Location loc) {
        return String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());
    }

    private int getDefaultInterval(Material material) {
        switch (material) {
            case DIAMOND:
                return 200; // 10 seconds
            case GOLD_INGOT:
                return 100; // 5 seconds
            case IRON_INGOT:
                return 60;  // 3 seconds
            case EMERALD:
                return 300; // 15 seconds
            default:
                return 100;
        }
    }

    private String getMaterialName(Material material) {
        switch (material) {
            case DIAMOND:
                return "diamond";
            case GOLD_INGOT:
                return "gold";
            case IRON_INGOT:
                return "iron";
            case EMERALD:
                return "emerald";
            default:
                return material.name().toLowerCase();
        }
    }

    private String getMaterialDisplayName(Material material) {
        switch (material) {
            case DIAMOND:
                return "ダイヤモンド";
            case GOLD_INGOT:
                return "金インゴット";
            case IRON_INGOT:
                return "鉄インゴット";
            case EMERALD:
                return "エメラルド";
            default:
                return material.name();
        }
    }

    public ManagementGUI getManagementGUI() {
        return managementGUI;
    }
}
