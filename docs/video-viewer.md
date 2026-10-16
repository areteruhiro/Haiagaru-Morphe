# 動画専用ビューア（1.8.6プレリリース）

Haiagaru設定の「動画を専用ビューアで開く」で切り替える。初期値オン。
オフならChMate従来の起動先を変更しない。

- 対象: 191 dev / 226 dev / 241 / 242 dev。
- 添付対応済みのImgur・X・Instagram配信元のHTTPS MP4を対象とする。
  アルバムURL自体ではなく、解析後の直接動画URLを開く。
- AndroidのVideoViewを使用し、再生・一時停止・シークを提供する。
  動画タップ後にストリーミング通信を行う。ログインCookieは渡さない。
- ︙: 外部アプリで開く、URLコピー、再読み込み、画面回転。
- タイトル文字は表示しない。「戻る」「︙」はシークバーと同時に表示・非表示にする。
  画面タップで切り替え、自動非表示タイマーは使用しない（シーク・再生操作後も同じ）。
  動画の表示位置・サイズは変えない。ボタン背景は透明で、白文字に黒い影を付ける。
- 他アプリへ移動した場合は一時停止し、戻っても勝手に再生しない。
- エラー時はエラー番号と再読み込み／外部アプリへの案内を表示。
  自動リトライやバックグラウンド再生は行わない。
- ステータスバー・ナビゲーションバーのInsetsを反映する。
- Activityはexported=false、外部アプリからの直接起動を許可しない。
- ネイティブのstartActivity/startActivityForResult境界だけに接続する。
  明示的な別画面指定・対象外URLは維持し、専用ビューア自身の外部起動は再介入しない。

## 検証

`VerifyVideoViewer`で4APKの専用Activity、設定、外部メニュー、
startActivityForResultを含む接続を確認した。
Manifestには非公開Activityと回転時のconfigChanges登録を確認した。
ランタイム検証は`scripts/frida-video-viewer-fixture.js`で行う。

191のエミュレータで、通常のサムネイルタップと同じネイティブヘルパーから
内部ビューアが開くこと、指定されたImgur動画の再生（約9.8秒）、
オン／オフと対象外URLの振り分け、︙の4項目、外部動画アプリへの起動を確認した。
再生中のホーム移動後に`playing=false / foreground=false`となることも確認した。
既存アプリはデータ保持で上書き。アップロード・削除・ログイン操作は行っていない。
226/241/242の実再生は未検証（APK適用と静的検証のみ）。
検証ログ: `video-viewer191-runtime.log` / `video-playback191.log` / `video-viewer-static.log`
（いずれもbuild/verification配下）。
