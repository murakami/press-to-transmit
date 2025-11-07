# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

PressToTransmit は VoicePing SDK を使用した Android プッシュトゥートーク (PTT) アプリケーションです。Firebase Cloud Messaging (FCM) を通じて受信したメッセージをトリガーに音声通信を開始します。

## ビルドコマンド

```bash
# デバッグビルド (ptt flavor)
./gradlew assemblePttDebug

# デバッグビルド (example flavor)
./gradlew assembleExampleDebug

# リリースビルド
./gradlew assemblePttRelease

# クリーンビルド
./gradlew clean

# アプリをインストール
./gradlew installPttDebug
```

## アーキテクチャ

### コア構成
- **MainActivity**: Jetpack Compose による UI とアプリケーションのエントリーポイント
  - VoicePing 接続設定 (サーバー URL、会社名、ユーザー ID など) の入力フォーム
  - FCM トークンの取得・表示機能
  - PTT ボタンによる手動での音声送信機能

- **VoicePingWorker**: CoroutineWorker を使用したバックグラウンド音声通信処理
  - Foreground Service として実行され、通知を表示
  - VoicePing SDK のラッパー関数を companion object で提供
  - すべての VoicePing 操作は `GlobalScope.launch(Dispatchers.Main)` で実行される必要がある

- **MyFirebaseMessagingService**: FCM メッセージ受信時に VoicePingWorker を起動
  - `handleIntent()` で WorkManager による VoicePingWorker のエンキュー処理

- **MyBroadcastReceiver**: PTT 関連のブロードキャストを受信 (現在はログ出力のみ)
  - Phonemax デバイスの物理 PTT ボタンなど、複数のアクションをリッスン

### VoicePing SDK の重要な制約
- VoicePing SDK のすべての操作は**必ずメインスレッド**で実行する必要がある
- `VoicePingWorker.kt` の companion object にある各ラッパー関数を使用すること
- 初期化順序: `VoicePing.init()` → `VoicePing.connect()` → その他の操作 (startTalking など)
- 再初期化前には必ず `VoicePing.dispose()` を呼び出す (VoicePingThread の適切な終了のため)

### AudioSource の最適化
- `AudioSourceConfig.kt` でデバイス製造元ごとに最適な AudioSource を選択
- 対応メーカー: LG, TCL, Motorola, Samsung, ALPS, ASUS
- デフォルトは `VOICE_COMMUNICATION`

## Product Flavors

プロジェクトには2つの product flavor が定義されています:
- **ptt**: `com.example.presstotransmit` (メイン)
- **example**: `com.example.Example` (開発・テスト用)

## 技術スタック

- **UI**: Jetpack Compose
- **非同期処理**: Kotlin Coroutines, WorkManager
- **Firebase**: FCM (Cloud Messaging)
- **音声通信**: VoicePing SDK (version 1.0 from JitPack)
- **最小 SDK**: 28 (Android 9.0)
- **ターゲット SDK**: 36

## 開発時の注意点

1. VoicePing SDK の操作は必ず `VoicePingWorker` の companion object メソッドを経由する
2. Foreground Service は Android O 以降で通知チャンネルの作成が必要
3. SharedPreferences で接続設定を永続化している (ServerUrl, Company, UserId など)
4. FCM トークンは起動時とトークン更新時に取得される
