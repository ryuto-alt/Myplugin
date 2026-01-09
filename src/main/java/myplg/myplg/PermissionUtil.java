package myplg.myplg;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * プレイヤーの権限をチェックするユーティリティクラス
 */
public class PermissionUtil {

    /**
     * プレイヤーがOP レベル4以上かどうかをチェック
     * @param player チェック対象のプレイヤー
     * @return OP レベル4以上の場合true
     */
    public static boolean isOpLevel4(Player player) {
        if (!player.isOp()) {
            return false;
        }

        // ops.jsonファイルを読み込む
        File opsFile = new File(Bukkit.getServer().getWorldContainer(), "ops.json");
        if (!opsFile.exists()) {
            // ops.jsonが存在しない場合、OPであればtrueとする（デフォルト動作）
            return true;
        }

        try (FileReader reader = new FileReader(opsFile)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonArray()) {
                return true; // フォーマットが不正な場合はデフォルトでtrue
            }

            JsonArray opsArray = element.getAsJsonArray();
            String playerUuid = player.getUniqueId().toString();
            String playerName = player.getName();

            for (JsonElement opElement : opsArray) {
                if (!opElement.isJsonObject()) {
                    continue;
                }

                JsonObject opObject = opElement.getAsJsonObject();

                // UUIDまたは名前で一致するか確認
                boolean isMatch = false;
                if (opObject.has("uuid") && opObject.get("uuid").getAsString().equals(playerUuid)) {
                    isMatch = true;
                } else if (opObject.has("name") && opObject.get("name").getAsString().equalsIgnoreCase(playerName)) {
                    isMatch = true;
                }

                if (isMatch) {
                    // levelフィールドをチェック
                    if (opObject.has("level")) {
                        int level = opObject.get("level").getAsInt();
                        return level >= 4;
                    } else {
                        // levelが指定されていない場合はデフォルトで4とみなす
                        return true;
                    }
                }
            }

            // ops.jsonに記載がない場合、OPであれば念のためtrue
            return true;

        } catch (IOException e) {
            Bukkit.getLogger().warning("ops.jsonの読み込みに失敗しました: " + e.getMessage());
            // エラーの場合、OPであればtrueとする
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("ops.jsonの解析に失敗しました: " + e.getMessage());
            return true;
        }
    }
}
