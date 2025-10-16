package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GeneCommand implements CommandExecutor {
    private final PvPGame plugin;
    private final Map<UUID, GeneratorSelection> selections = new HashMap<>();

    public GeneCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(Component.text("使用方法: /gene <diamond|gold|iron|emerald>", NamedTextColor.RED));
            return true;
        }

        String materialName = args[0].toLowerCase();
        Material material;

        switch (materialName) {
            case "diamond":
                material = Material.DIAMOND;
                break;
            case "gold":
                material = Material.GOLD_INGOT;
                break;
            case "iron":
                material = Material.IRON_INGOT;
                break;
            case "emerald":
                material = Material.EMERALD;
                break;
            default:
                player.sendMessage(Component.text("無効なアイテムタイプです。", NamedTextColor.RED));
                player.sendMessage(Component.text("使用可能: diamond, gold, iron, emerald", NamedTextColor.YELLOW));
                return true;
        }

        // Initialize or get selection
        GeneratorSelection selection = selections.getOrDefault(player.getUniqueId(), new GeneratorSelection());
        selection.material = material;

        if (selection.corner1 == null) {
            selection.corner1 = player.getLocation();
            selections.put(player.getUniqueId(), selection);
            player.sendMessage(Component.text("1つ目の座標を設定しました: ", NamedTextColor.GREEN)
                    .append(Component.text(formatLocation(selection.corner1), NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("もう一度同じコマンドを実行して2つ目の座標を設定してください。", NamedTextColor.YELLOW));
        } else if (selection.corner2 == null) {
            selection.corner2 = player.getLocation();

            // Check if both corners are in the same world
            if (!selection.corner1.getWorld().equals(selection.corner2.getWorld())) {
                player.sendMessage(Component.text("2つの座標は同じワールド内である必要があります。", NamedTextColor.RED));
                selections.remove(player.getUniqueId());
                return true;
            }

            player.sendMessage(Component.text("2つ目の座標を設定しました: ", NamedTextColor.GREEN)
                    .append(Component.text(formatLocation(selection.corner2), NamedTextColor.YELLOW)));

            // Create generator with default interval
            String generatorId = materialName + "_" + System.currentTimeMillis();
            int defaultInterval = getDefaultInterval(material);

            plugin.getGeneratorManager().addGenerator(generatorId, material, selection.corner1, selection.corner2, defaultInterval);

            player.sendMessage(Component.text("ジェネレーターを作成しました！", NamedTextColor.GREEN));
            player.sendMessage(Component.text("アイテム: " + getMaterialDisplayName(material), NamedTextColor.YELLOW));
            player.sendMessage(Component.text("出現間隔: " + (defaultInterval / 20.0) + "秒", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/edit で出現頻度を調整できます。", NamedTextColor.GRAY));

            // Clear selection
            selections.remove(player.getUniqueId());
        }

        return true;
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

    private static class GeneratorSelection {
        Material material;
        org.bukkit.Location corner1;
        org.bukkit.Location corner2;
    }
}
