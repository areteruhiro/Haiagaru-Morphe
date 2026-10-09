# ワッチョイ下4桁でNGName登録

対応対象は ChMate 0.8.10.191 dev／226 dev／241／242 dev。
各版の名前・ワッチョイ長押しメニューに同じ登録操作を追加する。
191の通常メニューと226以降のネイティブリストメニューは外観が異なるが、登録条件は共通。

- 選択したレスの名前・ワッチョイだけを参照し、他のレスや引用本文から取得しない。
- 前半4文字は英数字として扱う。`ﾜｯﾁｮｲ 8f01-uDul` に加え、`ｽｯﾌﾟ Sd03-uDul`、`ｵｯﾍﾟｹ Sr01-uDul` なども対象。
- 下4文字を正規表現リテラルとして登録し、大文字・小文字と記号を区別する。
- 前半・接頭辞が異なっても下4文字が一致すれば対象となる。別の利用者と一致する可能性を確認ダイアログで説明する。
- 全板共通のChMate標準NGNameへ登録し、標準NGネーム設定で編集・解除できる。
- NG登録は必死チェッカーの板ルーティング成功に依存しない。

## バージョン別の接続先

| ChMate | 名前長押しメニュー | NG登録後の更新 |
| --- | --- | --- |
| 191 dev | `pa.c(Response, boolean, boolean)` の選択レス | 標準登録と同じ `NgChanged` イベントを通知 |
| 226 dev | `getImgAcceptedHeight` のNGNameリスト生成、5引数ビルダー | 標準NGWord更新経路 |
| 241 | `ResListFragmentViewModel` のNGNameリスト生成、6引数ビルダー | 標準NGWord更新経路 |
| 242 dev | `ResListFragmentViewModel` のNGNameリスト生成、6引数ビルダー | 標準NGWord更新経路 |

226／241では既存のレス全体長押しメニューも残す。
メニュー生成フックはパッチ適用時にNGName文字列と引数型で一意に検証する。

各版の独自ポップアップは `MenuItem.OnMenuItemClickListener` を呼ばず、専用のディスパッチャーへ直接渡す。
`onItemClick(AdapterView, View, int, long)` のサブメニュー処理と標準クリックディスパッチを照合し、
標準ディスパッチ直前で、追加した下4桁NG項目（ID 76）だけを
保存済みのリスナーへ渡す。標準NG登録・コピー・ワッチョイ検索などの他項目は従来どおり処理する。
Mi Note 10 / 191 / Haiagaru 1.7.9で、Fridaによる同等補正後に登録ダイアログと
レス392の「あぼーん [NGName]」への即時反映を確認（2026-10-10）。

## 回帰テスト

- `java scripts/VerifyWacchoiSuffixNg.java`：接頭辞・英字を含む前半・記号・大小文字・境界。
- `java scripts/VerifyWacchoiNameSheet.java`：226と241／242のビルダー引数・コールバック。
- `java scripts/VerifyWacchoiNgPopup.java`：191の独自ポップアップのクリック通知・他項目の非干渉。
- Androidバンドルをビルドした後、各版の元APKへ適用を確認する。
- APK生成成功と、実機上のメニュー表示・登録直後のNG反映確認は区別する。
