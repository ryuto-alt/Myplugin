# Serena MCP セットアップガイド

このプロジェクトでは、コード理解と編集を強化するために **Serena MCP** が統合されています。

## Serena MCP とは？

Serena は、AI コーディングアシスタントに高度なセマンティック検索とコード編集機能を提供する無料のオープンソースツールです。以下の機能を提供します：

- **セマンティックコード検索**: コードベース全体から関連するコードを意味的に検索
- **インテリジェントな編集**: 言語サーバー統合による正確なコード編集
- **プロジェクト理解**: 大規模コードベースの構造を深く理解

## セットアップ状況

このプロジェクトには、以下がすでに設定されています：

✅ Serena MCP サーバーが Claude Code に追加済み
✅ プロジェクト固有の設定ファイル (`.serena/project.yml`) 作成済み
✅ `.gitignore` にSerenaキャッシュファイルの除外設定追加済み

## 設定内容

### Claude Code 設定

Serena MCP は以下のコマンドで追加されています：

```bash
claude mcp add serena -- uvx --from git+https://github.com/oraios/serena serena start-mcp-server --context ide-assistant --project "C:\Users\Unoryuto\Documents\MyPlg\myplg"
```

設定は `~/.claude.json` に保存されています。

### プロジェクト設定

`.serena/project.yml` には以下の設定が含まれています：

- **プロジェクト名**: myplg
- **対応言語**: Java
- **インデックス**: 有効（パフォーマンス向上）
- **除外パス**: target, build, .git, .idea, *.class
- **セマンティック検索**: 有効（深度: 5）
- **自動フォーマット**: 有効

## 使用方法

### 基本的な使い方

Serena MCP は Claude Code と統合されているため、特別な操作は不要です。Claude Code を使用すると、自動的に以下の機能が利用できます：

1. **コード検索**: 「〜の実装を探して」と依頼すると、セマンティック検索で該当コードを見つけます
2. **コード編集**: コード変更を依頼すると、言語サーバーベースの正確な編集が行われます
3. **コード理解**: プロジェクト構造やコード関係の理解が向上します

### 利用可能なツール

Serena MCP が提供する主なツール：

- `semantic_search`: セマンティックコード検索
- `get_definition`: シンボル定義の取得
- `get_references`: シンボル参照の検索
- `apply_edits`: コード編集の適用
- `get_diagnostics`: コード診断情報の取得

### プロジェクトのインデックス化

大規模プロジェクトの場合、初回起動時に自動的にインデックスが作成されます。これにより検索とナビゲーションが高速化されます。

## トラブルシューティング

### MCP サーバーが起動しない

```bash
# MCP サーバーの状態を確認
claude mcp list
```

### 設定の再読み込み

Claude Code を再起動することで、設定が再読み込みされます。

### Serena の更新

```bash
# 最新版に更新（uvx は自動的に最新版を使用）
claude mcp remove serena
claude mcp add serena -- uvx --from git+https://github.com/oraios/serena serena start-mcp-server --context ide-assistant --project "$(pwd)"
```

## 参考リンク

- [Serena GitHub リポジトリ](https://github.com/oraios/serena)
- [Serena ドキュメント](https://deepwiki.com/oraios/serena)
- [Claude Code ドキュメント](https://docs.claude.com/en/docs/claude-code)

## 注意事項

- `.serena/index/` と `.serena/cache/` は Git で追跡されません（キャッシュファイルのため）
- Java 言語サーバーが必要です（通常、JDK とともにインストールされます）
- 初回起動時はインデックス作成に時間がかかる場合があります

## サポートされている言語

このプロジェクトでは Java をサポートしていますが、Serena は以下の言語もサポートしています：

- Python, TypeScript/JavaScript, PHP, Go, Rust, C/C++, C#, Kotlin など

追加の言語サポートが必要な場合は、`.serena/project.yml` の `languages` セクションを編集してください。
