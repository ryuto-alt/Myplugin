package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GeneReloadCommand implements CommandExecutor {
    private final PvPGame plugin;

    public GeneReloadCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cこのコマンドはOP権限が必要です。");
            return true;
        }

        // Check if game is running
        boolean wasRunning = plugin.getGameManager().isGameRunning();

        // Stop all generators if running
        if (wasRunning) {
            plugin.getGeneratorManager().stopAllGenerators();
            sender.sendMessage("§e全てのジェネレーターを停止しました...");
        }

        // Clear existing generators from memory
        plugin.getGeneratorManager().getGenerators().clear();
        sender.sendMessage("§eメモリ上のジェネレーターをクリアしました...");

        // Reload generators from generators.yml
        plugin.getGeneratorDataManager().loadGenerators();
        sender.sendMessage("§agenerators.ymlから " + plugin.getGeneratorManager().getGenerators().size() + " 個のジェネレーターを再読み込みしました！");

        // Restart generators if game was running
        if (wasRunning) {
            plugin.getGeneratorManager().startAllGenerators();
            sender.sendMessage("§a全てのジェネレーターを再起動しました！");
        }

        // Log the intervals for verification
        plugin.getLogger().info("===== Generator Intervals After Reload =====");
        plugin.getGeneratorManager().getGenerators().forEach((id, gen) -> {
            plugin.getLogger().info(id + ": " + gen.getSpawnInterval() + " ticks (" + gen.getMaterial().name() + ")");
        });

        return true;
    }
}
