# 🔧 音楽が流れない場合のトラブルシューティング

## ✅ 修正完了した内容

### 1. **pack_format を 1.21.1 対応に更新**
- ❌ 旧: `"pack_format": 15` (Minecraft 1.20.0-1.20.1用)
- ✅ 新: `"pack_format": 34` (Minecraft 1.21.x用)

### 2. **sounds.json に category を追加**
- 各サウンドに `"category": "music"` を追加
- これにより音量スライダー（音楽）が正しく適用されます

## 📋 チェックリスト

### Step 1: リソースパックの確認

1. **BEDWARSTEXフォルダの構造を確認**:
```
BEDWARSTEX/
├── pack.mcmeta (pack_format: 34)
└── assets/
    └── minecraft/
        ├── sounds.json
        └── sounds/
            ├── lobby/
            │   ├── lobbyBGM1.ogg ✓
            │   ├── lobbyBGM2.ogg ✓
            │   ├── lobbyBGM3.ogg ✓
            │   ├── lobbyBGM4.ogg ✓
            │   └── lobbyBGM5.ogg ✓
            └── gamebgm/
                └── gameBGM.ogg ✓
```

2. **pack.mcmeta の内容確認**:
```json
{
  "pack": {
    "pack_format": 34,
    "description": "§6PvPGame BGM Pack\n§7Lobby & Game Music"
  }
}
```

3. **sounds.json の内容確認**:
```json
{
  "lobby.lobbybgm1": {
    "category": "music",
    "sounds": [
      {
        "name": "lobby/lobbyBGM1",
        "stream": true
      }
    ]
  }
}
```

### Step 2: リソースパックのZIP化

**重要**: ZIPの構造が正しいか確認してください！

❌ **間違い**:
```
BEDWARSTEX.zip
└── BEDWARSTEX/
    ├── pack.mcmeta
    └── assets/
```

✅ **正しい**:
```
BEDWARSTEX.zip
├── pack.mcmeta
└── assets/
    └── minecraft/
```

**正しいZIPの作り方**:
1. `BEDWARSTEX`フォルダを開く
2. **フォルダの中身**（pack.mcmeta, assetsなど）を全て選択
3. 右クリック → 送る → 圧縮(zip形式)フォルダー
4. `BEDWARSTEX.zip` にリネーム

### Step 3: Minecraftでの適用

#### 方法A: 手動適用
1. `BEDWARSTEX.zip` を `.minecraft/resourcepacks/` フォルダにコピー
2. Minecraftを起動
3. Options → Resource Packs
4. BEDWARSTEXパックを「Selected」側に移動
5. Done をクリック

#### 方法B: サーバー自動配布
`server.properties`:
```properties
resource-pack=https://example.com/BEDWARSTEX.zip
resource-pack-sha1=<SHA-1ハッシュ>
require-resource-pack=true
```

### Step 4: 動作確認

1. **リソースパックが読み込まれているか確認**:
   - F3 キーを押す
   - 右側に `Resource Packs: [BEDWARSTEX]` が表示されるか確認

2. **音楽が再生されるかテスト**:
   ```
   /playsound lobby.lobbybgm1 music @s
   ```
   このコマンドで音楽が流れたら成功！

3. **ログを確認**:
   ```
   [PvPGame] Playing lobby song for PlayerName: lobbyBGM1 (sound: lobby.lobbybgm1)
   ```

## 🐛 よくある問題と解決方法

### 問題1: リソースパックが認識されない

**症状**: Minecraftのリソースパック選択画面に表示されない、または赤い×マーク

**原因**: pack_formatが間違っている

**解決方法**:
1. `pack.mcmeta` を開く
2. `"pack_format": 34` になっているか確認
3. Minecraft 1.21.x を使用しているか確認

| Minecraftバージョン | pack_format |
|-------------------|-------------|
| 1.19.3 - 1.19.4   | 12          |
| 1.20.0 - 1.20.1   | 15          |
| 1.20.2 - 1.20.4   | 18          |
| 1.20.5 - 1.20.6   | 32          |
| 1.21.x            | 34          |

### 問題2: リソースパックは認識されるが音が出ない

**症状**: リソースパックは読み込まれているが、音楽が再生されない

**チェック項目**:

1. **音量設定を確認**:
   - Options → Music & Sounds
   - 「Music」スライダーが0%になっていないか確認

2. **サウンド名が正しいか確認**:
   プラグインのログで:
   ```
   Playing lobby song: lobbyBGM1 (sound: lobby.lobbybgm1)
   ```
   サウンド名が小文字 `lobby.lobbybgm1` になっているか確認

3. **OGGファイルが正しい場所にあるか確認**:
   ```
   assets/minecraft/sounds/lobby/lobbyBGM1.ogg
   ```
   ファイル名の大文字小文字が sounds.json と一致しているか確認

4. **sounds.json の構文エラー確認**:
   - https://jsonlint.com/ でバリデーション
   - カンマの位置、括弧の閉じ忘れに注意

### 問題3: 一部の音楽だけ再生されない

**症状**: lobbyBGM1は再生されるが、他の曲が再生されない

**原因**: ファイル名の不一致

**解決方法**:

1. **実際のファイル名を確認**:
   ```bash
   ls assets/minecraft/sounds/lobby/
   ```

2. **sounds.json と一致させる**:

   実際のファイル: `lobbyBGM2.ogg`

   sounds.json:
   ```json
   "lobby.lobbybgm2": {
     "sounds": [
       {
         "name": "lobby/lobbyBGM2",  // ← 大文字小文字を合わせる
         "stream": true
       }
     ]
   }
   ```

3. **プラグインのMusicManager.javaを確認**:
   ```java
   lobbySongs.add("lobbyBGM2");  // ← この名前と一致させる
   ```

### 問題4: 音楽が途切れる・ループしない

**症状**: 音楽が途中で止まる、または次の曲に切り替わらない

**原因**: 曲の長さ設定が実際の長さと合っていない

**解決方法**:

MusicManager.java の `playNextLobbySong()` と `playNextGameSong()` メソッドで曲の長さを調整:

```java
// 現在のデフォルト: 3分 (3600ティック)
long songDuration = 3600L;

// 実際の曲の長さに合わせて変更:
// 例: lobbyBGM5.ogg が15分の場合
private long getSongDuration(String songName) {
    switch (songName.toLowerCase()) {
        case "lobbybgm1": return 2400L;   // 2分
        case "lobbybgm2": return 6000L;   // 5分
        case "lobbybgm3": return 5400L;   // 4分30秒
        case "lobbybgm4": return 6000L;   // 5分
        case "lobbybgm5": return 18000L;  // 15分
        case "gamebgm": return 18000L;    // 15分
        default: return 3600L;
    }
}
```

**ティック計算式**:
- 1秒 = 20ティック
- 1分 = 1200ティック
- 計算例: 2分30秒 = (2 × 1200) + (30 × 20) = 3000ティック

### 問題5: サーバーログにエラーが出る

**症状**: `[PvPGame] No lobby songs found!`

**原因**: これは古いバージョンのエラーです。現在のMusicManagerはファイルを読み込まず、ハードコードしています。

**確認**:
サーバーログに以下が表示されるはずです:
```
[PvPGame] === Music System Loaded ===
[PvPGame] Lobby songs: lobbyBGM1, lobbyBGM2, lobbyBGM3, lobbyBGM4, lobbyBGM5
[PvPGame] Game songs: gameBGM
[PvPGame] Note: Music files must be in the client-side resourcepack!
```

## 🧪 デバッグ手順

### 1. リソースパックの検証

```bash
# ZIPファイルの構造を確認
unzip -l BEDWARSTEX.zip | head -20

# 正しい出力例:
# pack.mcmeta
# assets/
# assets/minecraft/
# assets/minecraft/sounds.json
# assets/minecraft/sounds/lobby/lobbyBGM1.ogg
```

### 2. サウンド再生テスト

Minecraftで以下のコマンドを実行:

```
/playsound lobby.lobbybgm1 music @s
/playsound lobby.lobbybgm2 music @s
/playsound gamebgm.gamebgm music @s
```

各コマンドで音楽が再生されれば、リソースパックは正しく動作しています。

### 3. プラグインの動作確認

1. サーバーに接続
2. ロビーにスポーン
3. 約1秒後に音楽が再生されるはず
4. `/start` コマンドでゲーム開始
5. ゲームBGMに切り替わるはず

## 📞 それでも解決しない場合

以下の情報を含めて報告してください:

1. **Minecraftバージョン**: `F3`キーで確認
2. **リソースパック情報**: F3画面のスクリーンショット
3. **サーバーログ**: `[PvPGame]` を含む部分全て
4. **ZIPファイルの構造**: `unzip -l BEDWARSTEX.zip` の出力
5. **実行したコマンド**: `/playsound` の結果
6. **音量設定**: Options > Music & Sounds のスクリーンショット

---

## 📝 チェックリスト (再確認用)

音が出ない場合、以下を順番に確認:

- [ ] Minecraft 1.21.x を使用している
- [ ] pack.mcmeta の pack_format が 34
- [ ] ZIPファイルの構造が正しい (pack.mcmeta が直下)
- [ ] リソースパックが Minecraft で有効化されている
- [ ] 音量スライダー (Music) が 0% でない
- [ ] `/playsound lobby.lobbybgm1 music @s` で音が出る
- [ ] サーバーログに `Music System Loaded` が表示される
- [ ] sounds.json に `"category": "music"` が含まれている
- [ ] OGGファイル名と sounds.json の name が一致している

全てチェックしても音が出ない場合は、上記の情報を含めて報告してください！
