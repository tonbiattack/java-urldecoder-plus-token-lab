# `URLDecoder`がプラス記号を空白へ変換し、トークン照合を失敗させる

Java標準ライブラリの`URLDecoder`を題材に、**プラス記号を含む不透明トークンをフォームデコードして値を変えてしまう**問題を、失敗するテスト、原因の直接観測、最小修正、回帰テストの順に追うデバッグ教材です。既定ブランチの`main`は成功状態に保ち、意図的に失敗する状態はGit履歴に独立して残します。

## この題材で守る契約

> `BASE42`を先に受理済みの状態で、クエリ文字列`token=AB+CD`を検証する場合、`AB+CD`を文字どおり受理し、最後に受理したトークンを`AB+CD`、受理件数を`2`にする。

| 段階 | 実施内容 | 確認すること |
| --- | --- | --- |
| 再現 | `AB+CD`に`URLDecoder.decode`を適用する | `AB CD`となって許可リスト照合に失敗し、旧トークン・旧件数が残る |
| 観測 | フォームデコードの結果と生トークンを比較する | `URLDecoder`は`+`を空白へ変換する一方、不透明トークンは`+`を保持すべきだと分かる |
| 修正 | 分離済みの不透明トークンにフォームデコードを適用しない | `AB+CD`をそのまま照合できる |
| 回帰防止 | 同じトークン検証テストを再実行する | 検証結果、最後のトークン、受理件数がすべて更新される |

## 必要な環境

| 項目 | バージョン |
| --- | --- |
| JDK | 21 |
| Maven | 3.8以上 |
| テストランナー | JUnit Jupiter 5.11.4 |
| アプリケーションフレームワーク | 不使用 |

## 最短の開始手順

```bash
mvn --batch-mode clean test
```

検証済みの`main`では、3テストがすべて成功します。

## バグを再現する

```bash
git checkout bfae7e6
mvn --batch-mode test -Dtest=CallbackTokenVerifierTest
# expected: <ACCEPTED> but was: <REJECTED>
# expected: <AB+CD> but was: <BASE42>
# expected: <2> but was: <1>

git checkout main
mvn --batch-mode clean test
# Tests run: 3, Failures: 0, Errors: 0
```

バグコミットでは許可リストやクエリ値の分離ではなく、プラス記号を含むトークンを文字どおり受理する契約だけが失敗します。完全な出力は[`evidence/01-bug-service-test-output.txt`](evidence/01-bug-service-test-output.txt)に保存しています。

## 原因の要点

`URLDecoder`は`application/x-www-form-urlencoded`を復号するためのユーティリティです。[1] この形式ではプラス記号`+`が空白を表すため、`URLDecoder.decode("AB+CD", UTF_8)`は`"AB CD"`を返します。[1]

今回の入力は、すでに分離済みの**不透明トークン**です。フォーム値としての空白復元ではなく、プラス記号をトークンの一部として保持する契約なので、フォームデコーダを適用してはいけません。適切なパーサがクエリ値を分離した後なら、トークンをそのまま比較します。

## プロジェクト構成

```text
.
├── docs/
│   ├── debugging-record.md      # 観測・仮説・原因・修正・回帰保証
│   ├── novelty-report.md        # 既存Java記事との四軸比較
│   └── topic-brief.md           # 実装前に固定した契約と再現境界
├── evidence/
│   ├── 01-bug-service-test-output.txt
│   ├── 02-url-decoder-observation-output.txt
│   └── 03-fixed-full-test-output.txt
├── src/main/java/.../token/
│   ├── CallbackTokenVerifier.java
│   └── TokenOutcome.java
└── src/test/java/.../token/
    ├── CallbackTokenVerifierTest.java
    └── UrlDecoderPlusObservationTest.java
```

詳細な調査手順は[デバッグ記録](docs/debugging-record.md)、既存コンテンツとの差分は[題材重複調査レポート](docs/novelty-report.md)を参照してください。

## スコープ

この教材はすでに分離済みの固定クエリ値と、プラス記号を含む不透明トークンの比較を対象にします。HTTPリクエスト解析、パーセントエンコード、URL構築、Base64、署名、認証・認可、外部キー管理は対象外です。

`application/x-www-form-urlencoded`のフォーム値を処理する場合、`URLDecoder`がプラス記号を空白に変換するのは正しい動作です。入力の形式・意味を確認せず、すべてのURL関連文字列からデコード処理を除去してはいけません。

## References

[1] [Oracle: `URLDecoder` — HTML form decoding and plus sign conversion](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URLDecoder.html)
