# Root対応のFrida検証環境

## 現在使用する環境（API 36）

- AVD: `Haiagaru_Root_API36`
- image: `system-images;android-36;google_apis;x86_64`
- ADB: `emulator-5558`
- 起動描画設定: `-gpu software`
- Frida: `17.18.0`、端末内`127.0.0.1:27042`をホストの`27052`へ転送

2026-10-14に起動完了・root・ホームと242アプリのスクリーンショット取得を確認した。
242の`HaiagaruTalk242`へFridaをattachし、`Java bridge OK: Android 36`を確認。
確認時点のcrashバッファは空。TalkのNPEの原因特定や修正確認は別途必要。

```powershell
adb -s emulator-5558 root
adb -s emulator-5558 shell '/data/local/tmp/haiagaru-frida-server -D -l 127.0.0.1:27042'
adb -s emulator-5558 forward tcp:27052 tcp:27042
frida-ps -H 127.0.0.1:27052 -ai
```

## 保存した旧環境（API 37.1・操作検証には使わない）

2026-10-14に作成。既存のGoogle Playエミュレータは変更せず、別AVDを追加した。

- AVD: `Haiagaru_Root_API37`
- image: `system-images;android-37.1;google_apis_ps16k;x86_64`
- ADB: `emulator-5556`（既存は`emulator-5554`）
- Frida client/server: `17.18.0`
- server: `/data/local/tmp/haiagaru-frida-server`
- server listen: `127.0.0.1:27042`（端末内ループバックのみ）
- host forward: `127.0.0.1:27052`

通常の再接続:

```powershell
adb -s emulator-5556 root
adb -s emulator-5556 shell id
adb -s emulator-5556 shell '/data/local/tmp/haiagaru-frida-server -D -l 127.0.0.1:27042'
adb -s emulator-5556 forward tcp:27052 tcp:27042
frida-ps -H 127.0.0.1:27052
```

API 37.1ではRegionSamplingの描画でSurfaceFlingerが
`Assertion failed: !rcEnc->featureInfo()->hasReadColorBufferDma`により再起動した。
この検証AVDだけで`adb shell setprop debug.sf.luma_sampling 0`を設定すると、
一時的に起動が進んだが、その後にも再発し、スクリーンショット取得も同じassertで
失敗した。API 37.1の環境は操作検証に使用せず、停止して保存している。
ChMateの不具合と混同しない。

Fridaのプロセス列挙と`uid=0`は確認済み。これだけではTalk投稿のNPE原因・修正の
確認にはならない。認証情報・投稿本文をログへ出さず、投稿の再試行は重複投稿に注意する。
