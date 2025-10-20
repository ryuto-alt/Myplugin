# BGM システムの使い方

このプラグインには、ロビーとゲーム中に自動でBGMを再生する機能が実装されています。

## フォルダ構成

プラグインフォルダ内に以下の2つのフォルダを作成してください:

```
plugins/
└── PvPGame/
    ├── lobby/        # ロビーBGM用のフォルダ
    └── gamebgm/      # ゲームBGM用のフォルダ
```

## 音楽ファイルの配置

### ロビーBGM (`lobby/` フォルダ)
- **ファイル形式**: `.ogg` (Ogg Vorbis形式)
- **動作**: フォルダ内の全てのOGGファイルを順番に再生し、最後まで行ったら最初に戻ります
- **再生タイミング**:
  - プレイヤーがサーバーに参加したとき
  - ゲーム終了後、ロビーに戻ったとき
  - ロビーワールドに移動したとき

### ゲームBGM (`gamebgm/` フォルダ)
- **ファイル形式**: `.ogg` (Ogg Vorbis形式)
- **動作**: フォルダ内の全てのOGGファイルを順番に再生し、ループします
- **再生タイミング**: `/start` コマンドでゲームが開始されたとき

## 重要な注意事項

### 1. 音楽ファイルをMinecraftのリソースパックに含める必要があります

OGGファイルは、**サーバー側プラグインだけでは再生できません**。以下の手順でリソースパックを作成してください:

#### リソースパックの作成手順:

1. リソースパックフォルダを作成:
```
resourcepack/
├── pack.mcmeta
└── assets/
    └── minecraft/
        └── sounds/
            ├── lobby/
            │   ├── song1.ogg
            │   ├── song2.ogg
            │   └── song3.ogg
            └── gamebgm/
                ├── battle1.ogg
                ├── battle2.ogg
                └── battle3.ogg
```

2. `pack.mcmeta` ファイルを作成:
```json
{
  "pack": {
    "pack_format": 15,
    "description": "PvPGame BGM Pack"
  }
}
```

3. `sounds.json` ファイルを作成 (`assets/minecraft/` フォルダ内):
```json
{
  "lobby.song1": {
    "sounds": [
      {
        "name": "lobby/song1",
        "stream": true
      }
    ]
  },
  "lobby.song2": {
    "sounds": [
      {
        "name": "lobby/song2",
        "stream": true
      }
    ]
  },
  "gamebgm.battle1": {
    "sounds": [
      {
        "name": "gamebgm/battle1",
        "stream": true
      }
    ]
  }
}
```

**注意**: 各OGGファイルに対して、`sounds.json` にエントリを追加する必要があります。

### 2. OGGファイルの命名規則

- ファイル名に**スペースや特殊文字を含めない**でください
- 推奨: `song1.ogg`, `battle_theme.ogg` など
- 非推奨: `my song.ogg`, `戦闘テーマ.ogg` など

### 3. 音楽の長さ設定

現在、MusicManager.javaでは全ての曲の長さを**3分(3600ティック)**と仮定しています。

曲の長さが異なる場合は、`MusicManager.java` の以下の部分を修正してください:

```java
// 例: 2分の曲の場合
long songDuration = 2400L; // 2分 = 2400ティック

// 例: 5分の曲の場合
long songDuration = 6000L; // 5分 = 6000ティック
```

**ティック計算**: 1秒 = 20ティック、1分 = 1200ティック

## 音楽の動作フロー

### ロビーでの動作:
1. プレイヤーがサーバーに参加 → ロビーBGM開始
2. 1曲目が終わる → 2曲目が自動再生
3. 全ての曲が終わる → 1曲目に戻ってループ
4. プレイヤーがロビーを離れる → BGM停止

### ゲーム中の動作:
1. `/start` コマンド実行 → ロビーBGM停止、ゲームBGM開始
2. ゲームBGMが全プレイヤーに再生される
3. `/end` コマンド実行 → ゲームBGM停止、ロビーBGM開始

## トラブルシューティング

### 音楽が再生されない場合:

1. **OGGファイルが正しいフォルダに配置されているか確認**
   - `plugins/PvPGame/lobby/` と `plugins/PvPGame/gamebgm/`

2. **リソースパックが正しく適用されているか確認**
   - サーバーのリソースパック設定を確認
   - クライアント側でリソースパックが有効になっているか確認

3. **sounds.json に全てのOGGファイルが登録されているか確認**

4. **ログを確認**
   - サーバーログで "Loaded X lobby songs and Y game songs" のメッセージを確認
   - 0曲ロードされている場合は、ファイル配置を確認

5. **OGGファイルが正しい形式か確認**
   - Minecraft 1.20では Ogg Vorbis 形式のみサポート
   - MP3やWAVは使用できません

## 推奨ツール

- **OGG変換**: Audacity (無料) - MP3/WAVからOGGに変換可能
- **リソースパック配布**: GitHub Releases, Google Driveなどでzipファイルとして配布

## カスタマイズ

音楽の再生時間や動作をカスタマイズしたい場合は、`MusicManager.java` を編集してください。

### 例: 各曲の長さを個別に設定したい場合

`MusicManager.java` に曲ごとの長さを設定するマップを追加:

```java
private Map<String, Long> songDurations = new HashMap<>();

// 初期化時に設定
songDurations.put("song1", 2400L);  // 2分
songDurations.put("song2", 3600L);  // 3分
songDurations.put("battle1", 4800L); // 4分

// 再生時に使用
long songDuration = songDurations.getOrDefault(songName, 3600L);
```
