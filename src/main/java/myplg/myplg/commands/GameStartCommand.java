package myplg.myplg.commands;

import myplg.myplg.GameMode;
import myplg.myplg.PermissionUtil;
import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /gamestart <mode> コマンド
 * ゲーム管理本から呼び出される
 */
public class GameStartCommand implements CommandExecutor {

    private final PvPGame plugin;

    public GameStartCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        // Admin権限チェック
        if (!PermissionUtil.isOpLevel4(player)) {
            player.sendMessage("§cこのコマンドはOP レベル4の権限が必要です。");
            return true;
        }

        // 引数チェック
        if (args.length < 1) {
            player.sendMessage("§c使用方法: /gamestart <solo|duo|triple|custom>");
            return true;
        }

        // ゲーム進行中チェック
        if (plugin.getGameManager().isGameRunning()) {
            player.sendMessage("§cゲームは既に進行中です。");
            return true;
        }

        // チーム設定チェック
        if (plugin.getGameManager().getTeams().isEmpty()) {
            player.sendMessage("§cチームが設定されていません。先に /setbed でチームを設定してください。");
            return true;
        }

        // オンラインプレイヤー取得（gameworld内のみ）
        List<Player> gamePlayers = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getWorld().getName().equalsIgnoreCase("world")) {
                gamePlayers.add(online);
            }
        }

        String mode = args[0].toLowerCase();

        // デバッグモードは最低人数チェックをスキップ
        if (!mode.equals("debug")) {
            // 最低人数チェック
            if (gamePlayers.size() < 2) {
                player.sendMessage("§cゲームを開始するには最低2人のプレイヤーが必要です。");
                player.sendMessage("§c現在のプレイヤー数: " + gamePlayers.size() + "人");
                return true;
            }
        }

        switch (mode) {
            case "solo":
                startGameWithMode(player, gamePlayers, GameMode.SOLO, false);
                break;
            case "duo":
                startGameWithMode(player, gamePlayers, GameMode.DUO, false);
                break;
            case "triple":
                startGameWithMode(player, gamePlayers, GameMode.TRIPLE, false);
                break;
            case "custom":
                openCustomModeGUI(player);
                break;
            case "debug":
                startGameWithMode(player, gamePlayers, GameMode.DEBUG, true);
                break;
            default:
                player.sendMessage("§c不明なモード: " + mode);
                player.sendMessage("§c使用可能: solo, duo, triple, custom, debug");
                break;
        }

        return true;
    }

    /**
     * 指定モードでゲームを開始（ランダムチーム割り当て）
     */
    private void startGameWithMode(Player admin, List<Player> players, GameMode mode, boolean isDebug) {
        // ゲームモードを設定
        plugin.getGameManager().setGameMode(mode);

        // デバッグモードを設定
        plugin.getGameManager().setDebugMode(isDebug);

        // 全プレイヤーの本を閉じる
        for (Player p : players) {
            p.closeInventory();
        }

        // チーム割り当て上限を設定
        plugin.getGameSetupManager().setMaxPlayersPerTeam(mode.getMaxPlayersPerTeam());

        // ランダムでチームに割り当て
        assignRandomTeams(players, mode.getMaxPlayersPerTeam());

        // 全プレイヤーに通知
        if (isDebug) {
            Bukkit.broadcastMessage("§4§l[DEBUG] §cデバッグモードでゲームを開始します！");
            Bukkit.broadcastMessage("§7ショップは無料です。");
            // デバッグ情報表示を開始
            plugin.getDebugInfoDisplay().start();
        } else {
            Bukkit.broadcastMessage("§6§l[Bedwars] §e" + mode.getDisplayName() + " §aモードでゲームを開始します！");
            Bukkit.broadcastMessage("§7チームはランダムで振り分けられました。");
        }

        // ゲーム管理本を削除
        plugin.getGameManagerBook().removeAllBooks();

        // カウントダウン開始
        plugin.getGameSetupManager().startCountdown();
    }

    /**
     * プレイヤーをランダムでチームに割り当て
     */
    private void assignRandomTeams(List<Player> players, int maxPlayersPerTeam) {
        // ゲーム設定をリセット
        plugin.getGameSetupManager().reset();

        // プレイヤーをシャッフル
        List<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        // チームリスト取得
        List<Team> teamList = new ArrayList<>(plugin.getGameManager().getTeams().values());

        int teamIndex = 0;
        int teamPlayerCount = 0;

        for (Player player : shuffledPlayers) {
            // チームインデックスがチーム数を超えたらループ（余りのプレイヤー対応）
            if (teamIndex >= teamList.size()) {
                // 全チームが満員の場合、最初のチームに戻ってオーバーフロー
                teamIndex = 0;
                plugin.getLogger().warning("全チームが満員です。プレイヤー " + player.getName() + " は追加できません。");
                player.sendMessage("§c全てのチームが満員です！観戦モードになります。");
                continue;
            }

            Team team = teamList.get(teamIndex);

            // プレイヤーをチームに設定（GameSetupManager経由）
            plugin.getGameSetupManager().setPlayerTeam(player, team.getName());

            // プレイヤーに通知
            player.sendMessage("§aあなたは §e" + team.getName() + " §aチームに配属されました！");

            teamPlayerCount++;

            // チームが満員になったら次のチームへ
            if (teamPlayerCount >= maxPlayersPerTeam) {
                teamIndex++;
                teamPlayerCount = 0;
            }
        }
    }

    /**
     * カスタムモードのGUIを開く
     */
    private void openCustomModeGUI(Player player) {
        // ゲームモードをカスタムに設定
        plugin.getGameManager().setGameMode(GameMode.CUSTOM);

        // ゲーム設定をリセット
        plugin.getGameSetupManager().reset();

        // ゲーム管理本を削除
        plugin.getGameManagerBook().removeAllBooks();

        // 全プレイヤーにチーム選択GUIを開く
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getWorld().getName().equalsIgnoreCase("world")) {
                plugin.getTeamSelectorGUI().openTeamSelector(online, 99); // カスタムは上限なし
            }
        }

        Bukkit.broadcastMessage("§6§l[Bedwars] §eカスタムモード §aでゲームを設定します！");
        Bukkit.broadcastMessage("§7チームを選択してください。");
    }
}
