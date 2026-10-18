# XIG05の動画ネイティブクラッシュ調査（未公開）

2026-10-18、Android 15 XIG05。`VideoFrameThumbnail.generate` から `MediaMetadataRetriever` のコンストラクタでSIGSEGV。別アプリのMediaCodecと、検証191のVideoView/MediaPlayerコンストラクタでも同じlibmedia.soがクラッシュ。Frida使用との因果関係は未確定。

サムネイル解析を非公開の別プロセスサービスへ移し、異常終了・45秒タイムアウト時は本体へ画像を渡さず再試行停止。再起動で停止状態を解除する。既存poster／placeholderは維持。例外をcatchするだけではSIGSEGVは防げない。

再起動後のFridaなしapp_processテストで標準MediaMetadataRetrieverとMediaPlayerの生成・release成功。検証アプリの起動・動画画面起動後の本体生存を確認。ただし実フレーム抽出app_processテストはKilledで終了し、サムネイル表示成功を確認したものではない。Frida経由テストも未完了。クラッシュ解決済みとは扱わない。

保護フラグunsafe=trueが再起動後も残ってサムネイル生成を止めたため、起動時刻の差で再起動を判別して解除する補正を追加。

検証パッケージは `jp.co.airfront.android.a2chMate.haiagaru.decoderfix191`。最初のCLI呼出で-Oが別パッチへ結びつき、意図せず既存haiagaruパッケージが上書きされた。アンインストール・データ消去なし。ユーザーへ報告済み。以降は-Oの直後に `-e 'Change ChMate package name'` を指定し、導入前にaaptでパッケージ名を照合する。

DAT落ち非表示も共通実装へ追加。Haiagaru表示設定の「未読・履歴・スレのタブでDAT落ちスレを非表示」。初期オフ。標準state & 1だけを判定し、DB・DATは変更しない。HOME 101/102/103のみ。3版の履歴行の状態フィールドを照合し、合成39チェック成功。実画面と他版パッチ適用は未確認。

ビルド成功、191元APKへの適用成功。1.8.16のReleaseは下書き、正式取得先1.7.1・プレ1.8.15へ復旧済み。この追加修正を公開していない。
