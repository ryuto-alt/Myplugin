# PvPGame BGMシステム セットアップガイド

## 🎵 概要

このプラグインには、ロビーとゲーム中に自動でBGMを再生するシステムが実装されています。

**音楽ファイル**:
- **ロビーBGM**: 5曲 (lobbyBGM1 ~ lobbyBGM5)
- **ゲームBGM**: 1曲 (gameBGM)

**重要**: 音楽ファイルは**リソースパック（クライアント側）のみ**に必要です。サーバー側にOGGファイルを配置する必要はありません！

## 📦 セットアップ手順

### ステップ1: リソースパックの準備

#### 1-1. リソースパックをZIPに圧縮

1. `resourcepack` フォルダを開く
2. フォルダの**中身**（`pack.mcmeta`, `assets` フォルダなど）を全て選択
3. 右クリック → 「送る」→ 「圧縮（zip形式）フォルダー」
4. ファイル名を `PvPGame-BGM.zip` に変更

**重要**: ZIPファイルを開いたときに、`pack.mcmeta` が直下に見えるようにしてください!

❌ 間違い:
```
PvPGame-BGM.zip
└── resourcepack/
    ├── pack.mcmeta
    └── assets/
```

✅ 正しい:
```
PvPGame-BGM.zip
├── pack.mcmeta
└── assets/
    └── minecraft/
        ├── sounds.json
        └── sounds/
```

#### 1-2. 音楽ファイルが含まれているか確認

ZIPファイル内の以下のパスに音楽ファイルがあることを確認:

```
assets/minecraft/sounds/lobby/
├── lobbyBGM1.ogg
├── lobbyBGM2.ogg
├── lobbyBGM3.ogg
├── lobbyBGM4.ogg
└── lobbyBGM5.ogg

assets/minecraft/sounds/gamebgm/
└── gameBGM.ogg
```

### ステップ2: サーバー側のセットアップ

#### 2-1. プラグインをビルド

```bash
cd C:\Users\Unoryuto\Documents\MyPlg\myplg
gradlew build
```

#### 2-2. プラグインファイルをサーバーにコピー

`build/libs/PvPGame-1.0-SNAPSHOT.jar` を サーバーの `plugins/` フォルダにコピー

**注意**: サーバー側にOGGファイルをコピーする必要はありません！

### ステップ3: リソースパックの配布

#### 方法A: 手動配布（開発・テスト用）

1. `PvPGame-BGM.zip` をプレイヤーに配布
2. プレイヤーは以下の手順で適用:
   - ZIPファイルを `.minecraft/resourcepacks/` にコピー
   - Minecraft起動 → Options → Resource Packs
   - 「PvPGame BGM Pack」を有効化
   - Done → サーバーに接続

#### 方法B: サーバーから自動配布（本番環境推奨）

1. **リソースパックをWebサーバーにアップロード**
   - GitHub Releases (推奨)
   - Google Drive (公開リンク)
   - 自前のWebサーバー
   - Dropbox など

2. **server.properties を編集**:

```properties
resource-pack=https://example.com/PvPGame-BGM.zip
resource-pack-sha1=<SHA-1ハッシュ>
require-resource-pack=true
```

3. **SHA-1ハッシュの生成**:

**Windows (PowerShell)**:
```powershell
Get-FileHash PvPGame-BGM.zip -Algorithm SHA1
```

**Mac/Linux**:
```bash
shasum PvPGame-BGM.zip
```

出力例:
```
SHA1ハッシュ: a1b2c3d4e5f6...
```

このハッシュ値を `server.properties` の `resource-pack-sha1` に設定

4. **サーバーを再起動**

これで、プレイヤーがサーバーに接続すると自動的にリソースパックがダウンロードされます。

### ステップ4: 動作確認

#### 1. サーバーログを確認

サーバー起動時に以下のようなログが出力されるはずです:

```
[PvPGame] === Music System Loaded ===
[PvPGame] Lobby songs: lobbyBGM1, lobbyBGM2, lobbyBGM3, lobbyBGM4, lobbyBGM5
[PvPGame] Game songs: gameBGM
[PvPGame] Note: Music files must be in the client-side resourcepack!
[PvPGame] ===========================
```

#### 2. ロビーBGMの確認

1. リソースパックを有効化してサーバーに接続
2. ロビーワールドにスポーン
3. 約1秒後にロビーBGMが再生開始
4. ログで再生状況を確認:
   ```
   [PvPGame] Playing lobby song for PlayerName: lobbyBGM1 (sound: lobby.lobbybgm1)
   ```

#### 3. ゲームBGMの確認

1. `/start` コマンドでゲーム開始
2. ゲームBGMが全プレイヤーに再生される
3. ログで確認:
   ```
   [PvPGame] Playing game song: gameBGM (sound: gamebgm.gamebgm)
   ```

#### 4. BGM切り替えの確認

1. `/end` コマンドでゲーム終了
2. ゲームBGMが停止
3. ロビーに戻り、ロビーBGMが再開

## 🎮 BGMシステムの動作

### ロビーBGM
- **再生開始**: プレイヤー参加時、ゲーム終了時
- **再生方式**: 5曲を順番に再生し、最後まで行ったら最初に戻る
- **個別再生**: 各プレイヤーごとに独立して再生

### ゲームBGM
- **再生開始**: `/start` コマンド実行時
- **再生方式**: 全プレイヤーに同時再生
- **停止**: `/end` コマンド実行時

### 自動切り替え
- ロビー → ゲームワールド: ロビーBGM停止
- ゲームワールド → ロビー: ロビーBGM開始
- ゲーム開始: ロビーBGM停止 → ゲームBGM開始
- ゲーム終了: ゲームBGM停止 → ロビーBGM開始

## 🔧 トラブルシューティング

### 音楽が再生されない

#### 1. リソースパックが適用されているか確認
- F3 + T でリソースパックをリロード
- Options > Resource Packs で有効化されているか確認
- サーバー自動配布の場合、接続時にダウンロードされたか確認

#### 2. サーバーログを確認
```
[PvPGame] === Music System Loaded ===
[PvPGame] Lobby songs: lobbyBGM1, lobbyBGM2, lobbyBGM3, lobbyBGM4, lobbyBGM5
```
→ 正常に読み込まれています

#### 3. sounds.json を確認
リソースパック内の `assets/minecraft/sounds.json` が正しいか確認:

```json
{
  "lobby.lobbybgm1": {
    "sounds": [
      {
        "name": "lobby/lobbyBGM1",
        "stream": true
      }
    ]
  }
}
```

**注意点**:
- サウンドID（`lobby.lobbybgm1`）は**小文字**
- ファイルパス（`lobby/lobbyBGM1`）は**大文字小文字を保持**
- `"stream": true` が必須

#### 4. ファイル名の一致確認

| resourcepack | sounds.json | プラグイン (MusicManager.java) |
|--------------|------------|-------------------------------|
| `lobbyBGM1.ogg` | `"name": "lobby/lobbyBGM1"` | `lobbySongs.add("lobbyBGM1")` |

**重要**: 全ての場所で**完全に同じファイル名**を使用してください！

### リソースパックが読み込まれない

#### 1. ZIPファイルの構造確認
ZIPファイルを開いて、直下に `pack.mcmeta` があることを確認

#### 2. pack.mcmeta の確認
```json
{
  "pack": {
    "pack_format": 15,
    "description": "§6PvPGame BGM Pack\n§7Lobby & Game Music"
  }
}
```

#### 3. Minecraftバージョンの確認
| Minecraft | pack_format |
|-----------|-------------|
| 1.20.0-1.20.1 | 15 |
| 1.20.2-1.20.4 | 18 |

### 音楽が途切れる・ループしない

現在、全ての曲の長さを**3分（3600ティック）**と仮定しています。

実際の曲の長さに合わせて `MusicManager.java` の `playNextLobbySong()` と `playNextGameSong()` を修正:

```java
// 曲ごとに長さを設定する例
private long getSongDuration(String songName) {
    switch (songName.toLowerCase()) {
        case "lobbybgm1": return 2400L;   // 2分 (2 * 60 * 20 ticks)
        case "lobbybgm2": return 6000L;   // 5分
        case "lobbybgm3": return 5400L;   // 4分30秒
        case "lobbybgm4": return 6000L;   // 5分
        case "lobbybgm5": return 18000L;  // 15分
        case "gamebgm": return 18000L;    // 15分
        default: return 3600L;            // デフォルト3分
    }
}

// playNextLobbySongメソッド内で使用
long songDuration = getSongDuration(songName);
```

**ティック計算**:
- 1秒 = 20ティック
- 1分 = 1200ティック
- 例: 2分30秒 = (2 × 1200) + (30 × 20) = 3000ティック

## 📁 最終的なファイル構成

### サーバー側
```
server/
├── plugins/
│   └── PvPGame-1.0-SNAPSHOT.jar
└── server.properties (リソースパック設定 - オプション)
```

**サーバー側にOGGファイルは不要です！**

### リソースパック (PvPGame-BGM.zip)
```
├── pack.mcmeta
└── assets/
    └── minecraft/
        ├── sounds.json
        └── sounds/
            ├── lobby/
            │   ├── lobbyBGM1.ogg
            │   ├── lobbyBGM2.ogg
            │   ├── lobbyBGM3.ogg
            │   ├── lobbyBGM4.ogg
            │   └── lobbyBGM5.ogg
            └── gamebgm/
                └── gameBGM.ogg
```

## 💡 カスタマイズ

### 音楽を追加する場合

1. **新しいOGGファイルをリソースパックに追加**:
   - `resourcepack/assets/minecraft/sounds/lobby/newSong.ogg`

2. **sounds.json に登録**:
```json
{
  "lobby.newsong": {
    "sounds": [
      {
        "name": "lobby/newSong",
        "stream": true
      }
    ]
  }
}
```

3. **MusicManager.java に追加**:
```java
private void loadSongs() {
    // ...
    lobbySongs.add("newSong");
    // ...
}
```

4. **プラグインを再ビルド & リソースパックを再圧縮**

5. **サーバー再起動**

## 🌐 GitHub Releasesでリソースパックを配布する方法

### 1. GitHub Releasesにアップロード

1. GitHubリポジトリページを開く
2. 右側の「Releases」をクリック
3. 「Create a new release」をクリック
4. Tag version: `v1.0` (例)
5. Release title: `PvPGame BGM Pack v1.0`
6. `PvPGame-BGM.zip` をドラッグ&ドロップでアップロード
7. 「Publish release」をクリック

### 2. ダウンロードURLを取得

アップロードされたZIPファイルを右クリック → 「リンクのアドレスをコピー」

例: `https://github.com/username/repo/releases/download/v1.0/PvPGame-BGM.zip`

### 3. server.propertiesに設定

```properties
resource-pack=https://github.com/username/repo/releases/download/v1.0/PvPGame-BGM.zip
resource-pack-sha1=<SHA-1ハッシュ>
require-resource-pack=true
```

## 📞 サポート

問題が解決しない場合は、以下の情報を含めて報告してください:

1. サーバーログ（`[PvPGame]` を含む部分）
2. リソースパックのZIP構造（ZIPを開いたスクリーンショット）
3. リソースパックが適用されているか（F3画面のスクリーンショット）
4. Minecraftバージョン
5. 問題の詳細な説明

---

**作成**: 2025
**プラグイン**: PvPGame v1.0
**Minecraft**: 1.20.x

## 📝 重要な注意事項まとめ

✅ **必要なもの**:
- リソースパック（クライアント側） - 音楽ファイルを含む
- プラグインJARファイル（サーバー側） - 音楽ファイルは不要

❌ **不要なもの**:
- サーバー側のOGGファイル
- plugins/PvPGame/lobby/ フォルダ
- plugins/PvPGame/gamebgm/ フォルダ

🎵 **仕組み**:
1. プラグインが `playSound()` でサウンド名を送信
2. クライアントがリソースパックからOGGファイルを再生
3. サーバーはタイミング制御のみを行う
