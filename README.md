# TanukiMC

Minecraft 1.21.1向けの、NeoForge MODとBukkit / Spigot / Paperプラグインを併用できるマルチスレッド対応サーバーです。[MohistMC/Youer](https://github.com/MohistMC/Youer)をベースに、領域単位の並列処理とTanukiMCの表示名を追加しています。

- Minecraft: 1.21.1
- NeoForge: 21.1.252
- Java: 21
- 上流ベース: Youer `7bac57ac0`

## 起動

サーバー用のフォルダへjarを置き、Java 21で起動してください。ファイル名の`<commit>`は使用するjarに合わせます。

```powershell
java -Xms2G -Xmx4G -jar TanukiMC-1.21.1-<commit>-server.jar nogui
```

Windowsで`java`が別のバージョンを指している場合は、Java 21の実行ファイルを直接指定できます。

```powershell
& "C:\Program Files\Java\jdk-21\bin\java.exe" -Xms2G -Xmx4G -jar TanukiMC-1.21.1-<commit>-server.jar nogui
```

初回はMinecraft EULAへの同意が必要です。内容を確認して同意する場合は、起動時の案内に`true`を入力してください。MODは`mods/`、プラグインは`plugins/`に配置します。接続するクライアントには、NeoForgeと必要なクライアント側MODを用意してください。停止にはコンソールの`stop`を使います。

## マルチスレッド処理

独立した領域のスケジュールtick、チャンクtick、ブロックエンティティ、対象エンティティ、領域スケジューラのタスクをワーカースレッドへ分配します。近接する領域は所有権をまとめ、越境する変更には所有権チェックとメインスレッドへの橋渡しを使います。プレイヤー処理や一部の共有処理はメインスレッドに残ります。

設定ファイルは互換性のため`youer-config/youer.yml`を引き続き使用します。並列処理は既定で有効です。例えばワーカー数を8にする場合は、既存の`youer`セクション内で以下を設定します。

```yaml
youer:
  threaded:
    enabled: true
    world_ticks: true
    worker_count: 8
    region_chunk_shift: 4
```

設定変更後はサーバーを再起動してください。ワーカー数を増やしても、プレイヤーが同じ領域に集まっている場合などは処理が均等に分散されるとは限りません。Foliaの完全な置き換えではなく、個々のMOD・プラグインのスレッド安全性には動作確認が必要です。

コンソールでは`youer windows`でCPU・メモリ、`tps`でTPS、`mspt`でtick時間を確認できます。既存のコマンド、内部パッケージ名、MOD IDなどのYouer名は互換性のため保持しています。

## ビルド

JDK 21を`JAVA_HOME`に設定し、リポジトリ直下で実行します。初回セットアップと依存関係の取得にはネットワーク接続が必要です。

```powershell
.\gradlew setup
.\gradlew :youer:threadingTopologyTest :youer:youerJar
```

Linuxでは`./gradlew`を使います。成果物は`projects/youer/build/libs/TanukiMC-1.21.1-<commit>-server.jar`です。GitHub Actionsの`TanukiMC Build`でもテストとビルドを実行し、jarをArtifactsへ保存します。

## 検証状況

2026-10-05に実施したローカル試験では、最大8ワーカーの同時実行、所有権と領域越境、MODのtick、プラグイン操作、ワールド保存を確認しました。次の構成では実NeoForgeクライアントの接続と10分間の接続維持が通過しています。

| 種別 | 検証したバージョン |
| --- | --- |
| Create | 6.0.9-216 |
| Mekanism | 10.7.19.85 |
| FerriteCore | 7.0.3 |
| ModernFix | 5.27.24 |
| LuckPerms | 5.5.85 |
| WorldEdit | 7.3.8 |

WorldEditのプレイヤーコマンドによるCreate・Mekanismブロックの編集も確認しています。これは上記構成での検証結果であり、すべてのMOD・プラグインの互換性や大規模運用の性能を保証するものではありません。新規ワールドの負荷試験では初回生成時に10秒のwatchdog警告が発生した試行があり、保存済みワールドでの再試験は警告なしで通過しています。

試験の実行方法は[tests/threading/README.md](tests/threading/README.md)を参照してください。

## 上流とライセンス

TanukiMCはYouer、NeoForge、Bukkit、CraftBukkit、Spigot、Paper、Purpur、Arclightなどの成果に基づいています。上流のコードとライセンス通知を保持しています。ライセンスの詳細は[LICENSE.md](LICENSE.md)と[LICENCE/](LICENCE/)を参照してください。

Youer固有の機能については[上流ドキュメント](https://mohistmc.com/youer/docs)も参考になります。TanukiMCへの問い合わせは[このフォークのIssues](https://github.com/kuretoshi/TanukiMC/issues)へお願いします。
