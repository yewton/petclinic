## 1. Entity Updates

- [x] 1.1 `net.yewton.petclinic.visit.Visit` クラスに `id: Int?` フィールドを追加して、DBにおける主キーとして扱えるようにする。

## 2. Repositories Layer

- [x] 2.1 `PetRepository.kt` の `save` メソッドに、`pet.isNew() == false` の場合の jOOQ ベースの UPDATE クエリ実装を追加する。
- [x] 2.2 `VisitRepository.kt` を新規作成し、jOOQ を使った `Visit` の保存処理（insert）と、取得処理を実装する（すべて `suspend fun` で実装）。
- [x] 2.3 `OwnerRepository` または関連リポジトリのデータ取得クエリを改修し、Owner 取得時に Pet の Visit リストも正しくフェッチ・マッピングされるよう修正する。

## 3. Controllers Layer

- [x] 3.1 `PetController.kt` に `/pets/{petId}/edit` エンドポイントに対する `GET` (フォーム初期化用) と `POST` (更新処理用バリデーション付き) メソッドを追加する。
- [x] 3.2 `VisitController.kt` を新規作成し、`/pets/{petId}/visits/new` に対する `GET` と `POST` 処理を実装する。

## 4. UI Bindings / Verification

- [x] 4.1 Thymeleaf テンプレートから渡されるフォームデータが新設した機能と正しくバインディングされ、エラー時のメッセージ出力が期待通りか確認する。
- [x] 4.2 バックエンドのテストコマンドを実行し、ビルドエラー並びに既存のエラーがないか確認する。
