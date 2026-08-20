# E009: `URLDecoder`がプラス記号を空白へ変換する

## 目的

コールバッククエリに含まれる不透明トークンでは、プラス記号がトークン値の一部です。`BASE42`を受理済みの状態で`token=AB+CD`を検証する場合、`AB+CD`を受理し、最後に受理したトークンを更新し、受理件数を二件にする必要があります。

## 実行環境と再現境界

このラボはJava 21、Maven、JUnit Jupiter 5.11.4だけを使います。HTTPサーバー、Servlet、ネットワーク、署名、データベース、ファイルを使いません。公開境界は`CallbackTokenVerifier#verifyQuery(String)`であり、直接の`TokenOutcome`に加えて、`lastAcceptedToken()`と`acceptedTokenCount()`の最終状態を別々に読みます。

テストは、最初に`token=BASE42`を受理したあと、`token=AB+CD`を検証します。これにより、二つ目の値が拒否されたとき、結果コードだけでなく、最後の受理トークンと受理件数が更新されないことを確認できます。入力・許可トークン・状態は固定であり、時刻、乱数、並行実行、外部I/Oに依存しません。

## 最初に観測した事実

バグ状態はコミット[`bfae7e6`](../commit/bfae7e6)です。次のコマンドで、意図したアサーション差分を確認しました。

```bash
git checkout bfae7e6
mvn --batch-mode test -Dtest=CallbackTokenVerifierTest
```

| 観測項目 | 期待 | 実際 | 根拠 |
| --- | --- | --- | --- |
| 直接の検証結果 | `ACCEPTED` | `REJECTED` | `CallbackTokenVerifierTest` |
| 最後に受理したトークン | `AB+CD` | `BASE42` | `CallbackTokenVerifier#lastAcceptedToken()` |
| 受理トークン件数 | `2` | `1` | `CallbackTokenVerifier#acceptedTokenCount()` |
| フォームデコードの結果 | `AB CD` | `AB CD` | `UrlDecoderPlusObservationTest` |
| 不透明トークンの値 | `AB+CD` | `AB+CD` | `UrlDecoderPlusObservationTest` |

```text
プラス記号を含む許可トークンを受理する
==> expected: <ACCEPTED> but was: <REJECTED>

最後に受理したトークンはプラス記号を保持する
==> expected: <AB+CD> but was: <BASE42>

BASE42とAB+CDの二件を受理する
==> expected: <2> but was: <1>
```

完全な失敗出力は[`evidence/01-bug-service-test-output.txt`](../evidence/01-bug-service-test-output.txt)に保存しています。直接結果だけでなく、最後のトークンと受理件数を最終状態として分けて確認したため、照合エラーが表示だけでなく、二つ目のトークン受理を止めていることを確定できます。

## 競合仮説と検証

| 仮説 | 確認方法 | 結果 |
| --- | --- | --- |
| 許可トークンに`AB+CD`がない | 固定の許可セットを確認する | `AB+CD`が含まれるため棄却。 |
| クエリからtoken値を分離するとプラス記号が失われる | 分離直後の`rawToken`を確認する | `AB+CD`のままなので棄却。 |
| `URLDecoder`がプラス記号を空白へ変換する | 同じ文字列を`URLDecoder.decode`へ渡す | `AB CD`となるため採用。 |

## 確定した原因

バグ状態は、分離済みトークンにフォームデコーダを適用していました。

```java
String token = URLDecoder.decode(rawToken, StandardCharsets.UTF_8);
```

`URLDecoder`は`application/x-www-form-urlencoded`を復号するユーティリティです。[1] この形式ではプラス記号は空白を表すため、`+`は空白に変換されます。[1] `AB+CD`は`AB CD`となり、許可セットの`AB+CD`と一致しなくなります。

問題は文字コード指定ではなく、**入力形式とAPIの契約の不一致**です。今回の値はフォーム値としての空白復元ではなく、不透明トークンとして比較する必要があります。したがってプラス記号を変換するフォームデコーダを適用してはいけません。

## 最小修正

修正コミットは[`826e46b`](../commit/826e46b)です。分離済みの不透明トークンをそのまま照合しました。

```java
String token = rawToken;
```

これにより`AB+CD`は変更されず、許可セットと一致します。修正はトークン照合の一行だけであり、許可リスト、状態更新、テスト入力を変更していません。

全てのプラス記号をパーセントエンコードへ置換する、`URLDecoder`の結果の空白を再びプラス記号へ戻す、テストの期待値を`AB CD`へ下げる修正は採用しませんでした。今回の公開契約は、トークンを文字どおり扱うことだからです。

## 回帰保証

### 再発防止テスト

最初に失敗した`plusSignToken_isAcceptedLiterallyAndUpdatesAcceptedState`はそのまま残しています。このテストは、直接結果、最後のトークン、受理件数を別々に検証します。

| テスト | 回帰として守る契約 |
| --- | --- |
| `plusSignToken_isAcceptedLiterallyAndUpdatesAcceptedState` | `AB+CD`を文字どおり受理し、最後のトークンと受理件数を更新する。 |
| `unknownToken_preservesAcceptedState` | 不明トークンを拒否し、受理済み状態を変更しない。 |
| `formDecoderTurnsPlusIntoSpaceWhileOpaqueTokenMustKeepIt` | フォームデコーダの`+`変換と、不透明トークンの保持要件を並べて示す。 |

修正後の`mvn --batch-mode clean test`では、3テストがすべて成功しました。完全な出力は[`evidence/03-fixed-full-test-output.txt`](../evidence/03-fixed-full-test-output.txt)に保存しています。

## 再現手順

```bash
git checkout bfae7e6
mvn --batch-mode test -Dtest=CallbackTokenVerifierTest
# expected: <ACCEPTED> but was: <REJECTED>

git checkout main
mvn --batch-mode clean test
# Tests run: 3, Failures: 0, Errors: 0
```

## スコープと注意点

この修正は、値がプラス記号を含む**不透明トークン**で、フォームデコードを必要としない場合にだけ有効です。`application/x-www-form-urlencoded`のフォーム値では、`+`を空白として復元する`URLDecoder`の挙動は正しいものです。

本ラボはHTTPリクエスト全体の解析、パーセントエンコード、URL構築、Base64、署名、認証・認可を扱いません。実システムでは入力がフォーム値・クエリ文字列・パス成分・署名済みペイロードのどれかを確認してから、対応するデコード規則を適用してください。

## References

[1] [Oracle: `URLDecoder` — HTML form decoding and plus sign conversion](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URLDecoder.html)
