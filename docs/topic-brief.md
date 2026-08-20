# 題材企画: `URLDecoder`がプラス記号を空白へ変換する

## 対象

| 項目 | 内容 |
| --- | --- |
| 対象言語 | Java 21 |
| 対象読者 | URIのクエリ文字列・フォームエンコード・不透明トークンの境界を区別しないまま`URLDecoder`を使いやすい中級者 |
| 難易度プロファイル | 実践・上級 |
| 選定理由 | `URLDecoder`は`application/x-www-form-urlencoded`向けであり、プラス記号を空白へ変換する。クエリの不透明なトークン値に適用すると`AB+CD`が`AB CD`となり、トークン照合が失敗する。直接の検証結果、最後に受理したトークン、受理件数を分けて観測し、入力のエンコード形式・プラス記号・標準API規則を比較できる。 |
| 実行基盤 | Maven、Java 21、JUnit Jupiter 5.11.4 |
| フレームワーク非依存性 | 原因は`java.net.URLDecoder`の標準ライブラリ契約である。HTTP、Servlet、DI、DB、外部ネットワークには依存しない。 |

## 学習する契約

> トークン`BASE42`を先に受理済みの状態で、クエリ文字列`token=AB+CD`を検証する場合、`AB+CD`を文字どおりトークン値として受理し、最後に受理したトークンを`AB+CD`、受理件数を二件にすべきだが、バグ状態では`AB CD`へ変換されて拒否され、旧トークン`BASE42`と受理件数一件が残る。

### 対象の直接原因

クエリ値`AB+CD`を`URLDecoder.decode(value, UTF_8)`へ渡している。`URLDecoder`はフォームデコードの規則により`+`を空白へ変換するため、プラス記号を含む不透明トークンが変更される。

### 対象外

このラボはHTTPリクエスト解析、URL構築、パーセントエンコード、署名アルゴリズム、Base64、トークン発行、認証・認可、外部のキー管理を扱わない。すでに分離済みの`token=...`形式の固定文字列から、プラス記号を含むトークンをそのまま比較する狭い規則だけを扱う。

## 再現設計

| 要素 | 決定 |
| --- | --- |
| 公開境界 | `CallbackTokenVerifier#verifyQuery(String)`、`lastAcceptedToken()`、`acceptedTokenCount()`。 |
| 入力・初期状態 | 最初に`token=BASE42`を受理し、その後`token=AB+CD`を検証する。 |
| Redの観測 | `TokenOutcome.ACCEPTED`を期待するが、バグ状態では`TokenOutcome.REJECTED`となる。 |
| 最終観測 | `lastAcceptedToken()`が`"AB+CD"`となり、`acceptedTokenCount()`が`2`であることを別々に検証する。 |
| 決定性 | 時刻、乱数、並行実行、`sleep`、外部I/Oを使わず、固定の文字列とインメモリ状態だけを使う。 |
| 固定状態の検証コマンド | `mvn --batch-mode clean test` |
| バグ状態の確認コマンド | `git checkout <bug-commit>`後に`mvn --batch-mode test -Dtest=CallbackTokenVerifierTest` |

## 仮説

| 仮説 | どう検証または除外するか |
| --- | --- |
| A: トークン許可リストに`AB+CD`がない | 固定許可トークンを直接確認する。 |
| B: クエリからtoken値を分離する処理がプラス記号を失っている | 分離直後の値が`AB+CD`であることを確認する。 |
| C: `URLDecoder`がプラス記号を空白へ変換する | 同じ`AB+CD`に`URLDecoder.decode`を適用し、`AB CD`になることを直接観測する。 |

## 予定する履歴

| 順序 | コミットの目的 | 期待する状態 |
| --- | --- | --- |
| 1 | プラス記号を含むトークンを拒否する失敗を再現する | 対象テストが`ACCEPTED`期待・`REJECTED`実際のアサーション差分で失敗する。 |
| 2 | 不透明トークンのプラス記号を保持する | 同じ検証が成功し、全体も成功する。 |
