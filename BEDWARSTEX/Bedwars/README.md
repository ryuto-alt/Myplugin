# PvPGame BGM リソースパック

このリソースパックは、PvPGameプラグインのBGMシステムに対応しています。

## 📁 フォルダ構成

```
resourcepack/
├── pack.mcmeta                    # リソースパック設定ファイル
├── pack.png                       # リソースパックアイコン (128x128px)
├── PACK_ICON_INFO.txt            # アイコン作成ガイド
└── assets/
    └── minecraft/
        ├── sounds.json            # サウンド定義ファイル
        └── sounds/
            ├── lobby/             # ロビーBGMフォルダ
            │   ├── song1.ogg
            │   ├── song2.ogg
            │   └── song3.ogg
            └── gamebgm/           # ゲームBGMフォルダ
                ├── battle1.ogg
                ├── battle2.ogg
                └── battle3.ogg
```

## 🎵 音楽ファイルの追加方法

### 1. OGGファイルを用意する

- **形式**: Ogg Vorbis (.ogg)
- **推奨設定**:
  - ビットレート: 128-192 kbps
  - サンプルレート: 44.1kHz
  - チャンネル: ステレオ

### 2. ファイルを配置する

#### ロビーBGM:
```
assets/minecraft/sounds/lobby/曲名.ogg
```

#### ゲームBGM:
```
assets/minecraft/sounds/gamebgm/曲名.ogg
```

### 3. sounds.json に登録する

`assets/minecraft/sounds.json` を開いて、新しい曲を追加:

```json
{
  "lobby.新しい曲名": {
    "sounds": [
      {
        "name": "lobby/新しい曲名",
        "stream": true
      }
    ]
  }
}
```

**重要**:
- `"stream": true` を必ず含めてください (メモリ節約のため)
- ファイル名から `.ogg` 拡張子は除いてください

## 🎧 MP3/WAVをOGGに変換する方法

### Audacity を使用 (無料・推奨)

1. [Audacity](https://www.audacityteam.org/) をダウンロード
2. 音楽ファイルを開く (File > Open)
3. 必要に応じて編集 (カット、フェードアウトなど)
4. File > Export > Export as OGG
5. 品質: 5-7 を選択 (ファイルサイズと品質のバランス)
6. Export をクリック

### オンラインツール

- [Online-Convert.com](https://audio.online-convert.com/convert-to-ogg)
- [CloudConvert](https://cloudconvert.com/mp3-to-ogg)

## 📦 リソースパックの配布方法

### 方法1: ZIPファイルとして配布

1. `resourcepack` フォルダ全体を選択
2. ZIPファイルに圧縮
3. 配布 (Google Drive, Dropbox, GitHubなど)

**使用方法 (プレイヤー側)**:
1. ZIPファイルをダウンロード
2. Minecraftの `resourcepacks` フォルダに配置
3. Minecraft > Options > Resource Packs でパックを有効化

### 方法2: サーバーから自動配布

`server.properties` を編集:

```properties
resource-pack=https://example.com/pvpgame-bgm.zip
resource-pack-sha1=<SHA-1ハッシュ>
require-resource-pack=true
```

**SHA-1ハッシュの生成**:
```bash
# Windows (PowerShell)
Get-FileHash resourcepack.zip -Algorithm SHA1

# Mac/Linux
shasum resourcepack.zip
```

## 🎮 プラグイン側の設定

リソースパックを作成したら、プラグイン側にも音楽ファイルを配置してください:

```
plugins/PvPGame/
├── lobby/
│   ├── song1.ogg
│   ├── song2.ogg
│   └── song3.ogg
└── gamebgm/
    ├── battle1.ogg
    ├── battle2.ogg
    └── battle3.ogg
```

**重要**: リソースパックとプラグインフォルダのファイル名は完全に一致させてください！

## ⚙️ 対応バージョン

- **Minecraft**: 1.20.x
- **pack_format**: 15

他のバージョンの場合は、`pack.mcmeta` の `pack_format` を調整してください:

| Minecraft Version | pack_format |
|------------------|-------------|
| 1.19.3 - 1.19.4  | 12          |
| 1.20 - 1.20.1    | 15          |
| 1.20.2 - 1.20.4  | 18          |
| 1.20.5 - 1.20.6  | 32          |
| 1.21             | 34          |

## 🔧 トラブルシューティング

### 音楽が再生されない

1. **リソースパックが有効になっているか確認**
   - F3 + T でリロード試行
   - Options > Resource Packs で有効化確認

2. **sounds.json の構文エラー確認**
   - JSONLint などでバリデーション
   - カンマの位置、括弧の閉じ忘れに注意

3. **ファイル名の一致確認**
   - sounds.json の名前と実際のファイル名が一致しているか
   - 大文字小文字の違いに注意 (song1 ≠ Song1)

4. **OGGファイルの形式確認**
   - Ogg Vorbis 形式か確認
   - VLC Media Player などで再生できるか確認

### リソースパックが読み込まれない

1. **pack.mcmeta の確認**
   - JSON形式が正しいか
   - pack_format が使用中のMinecraftバージョンに対応しているか

2. **フォルダ構成の確認**
   - `assets/minecraft/` の階層が正しいか
   - 余計なフォルダが挟まっていないか

3. **ZIPファイルの確認**
   - ZIPファイルを開いたときに、直下に `pack.mcmeta` があるか
   - `resourcepack/pack.mcmeta` のように余計なフォルダが入っていないか

## 📝 ライセンスについて

音楽ファイルを配布する際は、以下に注意してください:

1. **著作権**: 使用する音楽の著作権を確認
2. **ライセンス**: フリー音源の場合もライセンス条項を確認
3. **クレジット**: 必要に応じて作曲者をクレジット表記

### フリー音源サイト (例)

- [魔王魂](https://maou.audio/) - 日本のフリー音源
- [DOVA-SYNDROME](https://dova-s.jp/) - 日本のフリーBGM
- [Free Music Archive](https://freemusicarchive.org/)
- [Incompetech](https://incompetech.com/music/royalty-free/)

## 💡 カスタマイズのヒント

### 1. 曲ごとに異なる音量を設定

sounds.json で `volume` パラメータを追加:

```json
{
  "lobby.song1": {
    "sounds": [
      {
        "name": "lobby/song1",
        "stream": true,
        "volume": 0.8
      }
    ]
  }
}
```

### 2. ランダム再生

同じサウンドIDに複数の曲を登録:

```json
{
  "lobby.random": {
    "sounds": [
      {
        "name": "lobby/song1",
        "stream": true
      },
      {
        "name": "lobby/song2",
        "stream": true
      }
    ]
  }
}
```

### 3. 複数のバリエーション

pitch パラメータで音程を変更:

```json
{
  "lobby.song1": {
    "sounds": [
      {
        "name": "lobby/song1",
        "stream": true,
        "pitch": 1.0
      }
    ]
  }
}
```

## 🆘 サポート

質問や問題がある場合は、プラグイン開発者に連絡してください。

---

**作成者**: PvPGame Plugin
**最終更新**: 2025
