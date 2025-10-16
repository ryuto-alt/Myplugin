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

    public GUIClickListener(PvPGame plugin) {
        this.plugin = plugin;
        this.managementGUI = new ManagementGUI(plugin);
        this.waitingForRename = new HashMap<>();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        // Main menu
        if (title.equals("ゲーム管理")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (clickedItem.getType() == Material.WHITE_BED) {
                managementGUI.openTeamList(player);
            } else if (clickedItem.getType() == Material.DROPPER) {
                managementGUI.openGeneratorList(player);
            }
        }
        // Generator settings menu
        else if (title.equals("ジェネレーター設定")) {
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

            // Find the generator by material type and slot
            int slot = event.getSlot();
            int currentSlot = 0;

            for (Generator generator : plugin.getGeneratorManager().getGenerators().values()) {
                if (currentSlot == slot && generator.getMaterial() == clickedItem.getType()) {
                    handleGeneratorClick(player, generator, event.getClick());
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

    private void handleGeneratorClick(Player player, Generator generator, ClickType clickType) {
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

        // Refresh GUI
        managementGUI.openGeneratorList(player);
    }

    public ManagementGUI getManagementGUI() {
        return managementGUI;
    }
}
