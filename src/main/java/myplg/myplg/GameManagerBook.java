package myplg.myplg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * ゲーム管理本
 * Adminがゲームモードを選択して開始するための本
 */
public class GameManagerBook {

    private final PvPGame plugin;

    // 本の識別子
    public static final String BOOK_IDENTIFIER = "GameManagerBook";

    public GameManagerBook(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * ゲーム管理本を作成
     */
    public ItemStack createBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta != null) {
            meta.setTitle("ゲーム管理");
            meta.setAuthor("Bedwars");

            // ページ1: メインメニュー
            Component page1 = Component.text("")
                .append(Component.text("=== ゲーム管理 ===")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("モードを選択して")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("ゲームを開始します")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("→ 次のページへ")
                    .color(NamedTextColor.YELLOW));

            // ページ2: 1vs1モード
            Component page2 = Component.text("")
                .append(Component.text("【 1vs1 モード 】")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("各チーム1人ずつ")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("ランダム振り分け")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("[クリックで開始]")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/gamestart solo")));

            // ページ3: 2vs2モード
            Component page3 = Component.text("")
                .append(Component.text("【 2vs2 モード 】")
                    .color(NamedTextColor.BLUE)
                    .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("各チーム2人ずつ")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("ランダム振り分け")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("[クリックで開始]")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/gamestart duo")));

            // ページ4: 3vs3モード
            Component page4 = Component.text("")
                .append(Component.text("【 3vs3 モード 】")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("各チーム3人ずつ")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("ランダム振り分け")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("[クリックで開始]")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/gamestart triple")));

            // ページ5: カスタムモード
            Component page5 = Component.text("")
                .append(Component.text("【 カスタム 】")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("チームを手動で選択")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("自由な編成が可能")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("[クリックで設定]")
                    .color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/gamestart custom")));

            // ページ6: デバッグモード
            Component page6 = Component.text("")
                .append(Component.text("【 DEBUG 】")
                    .color(NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("1人でプレイ可能")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("ショップ無料")
                    .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("[クリックで開始]")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/gamestart debug")));

            // ページを追加
            List<Component> pages = new ArrayList<>();
            pages.add(page1);
            pages.add(page2);
            pages.add(page3);
            pages.add(page4);
            pages.add(page5);
            pages.add(page6);

            meta.pages(pages);

            // カスタムモデルデータで識別
            meta.setCustomModelData(9999);

            book.setItemMeta(meta);
        }

        return book;
    }

    /**
     * プレイヤーにゲーム管理本を渡す
     */
    public void giveBook(Player player) {
        // 既に持っているか確認
        if (hasBook(player)) {
            return;
        }

        ItemStack book = createBook();
        player.getInventory().addItem(book);
        player.sendMessage("§6§l[Bedwars] §aゲーム管理本を受け取りました！");
    }

    /**
     * プレイヤーがゲーム管理本を持っているか確認
     */
    public boolean hasBook(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isGameManagerBook(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * アイテムがゲーム管理本かどうか確認
     */
    public static boolean isGameManagerBook(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) {
            return false;
        }

        BookMeta meta = (BookMeta) item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // カスタムモデルデータで識別
        return meta.hasCustomModelData() && meta.getCustomModelData() == 9999;
    }

    /**
     * プレイヤーからゲーム管理本を削除
     */
    public void removeBook(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isGameManagerBook(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    /**
     * 全Adminからゲーム管理本を削除
     */
    public void removeAllBooks() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            removeBook(player);
        }
    }
}
