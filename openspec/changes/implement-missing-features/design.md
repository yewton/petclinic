## Context

現在 `petclinic-fullstack` では、`spring-petclinic`（参照実装）にあるペット情報の更新（edit）および、ペットの診療や訪問記録を管理する `Visit` 機能が未実装です（エンティティの定義の一部のみ存在）。これを参照実装と同等の機能を持つように追加実装します。アプリケーションは Spring Boot 上で動作し、データベースアクセスには jOOQ を、View 層には Thymeleaf を使用しています。

## Goals / Non-Goals

**Goals:**
- `/owners/{ownerId}/pets/{petId}/edit` への GET/POST リクエストを処理し、既存のペット情報（名前、生年月日、種類）を更新する。
- `/owners/{ownerId}/pets/{petId}/visits/new` への GET/POST リクエストを処理し、指定のペットへの新しい `Visit` （診療記録）を保存する。
- 既存の Thymeleaf テンプレート（`createOrUpdatePetForm.html`, `createOrUpdateVisitForm.html`）へ必要なデータを渡し、適切に画面が表示されるようにする。
- `VisitRepository` を新規作成し、jOOQ を使った DB 操作（追加、参照）を実装する。
- `PetRepository` にある未実装の UPDATE 操作（更新処理）を jOOQ を用いて実装する。
- 飼い主詳細画面などで、ペットに対する Visit 一覧を表示させるために、取得系のクエリを修正する。

**Non-Goals:**
- jOOQ 以外の DB アクセス実装（例えば、JPA や Spring Data JDBC 版への対応）を新たに追加すること。
- UI テンプレートのデザイン自体を変更すること（あくまで提供されている HTML/CSS の要件を満たすバインディングを行う）。

## Decisions

- **VisitRepository の新設**: 
  - 既存のパターンに則り、`VisitRepository` を追加して jOOQ の `DSLContext` を使ったデータ操作クラスを実装します。これにより Pet と Visit の責務を明確に分離します。
- **Pet 更新 (UPDATE) 処理**: 
  - `PetRepository#save()` 内で、`pet.isNew()` が false の場合は `PETS` テーブルに対する UPDATE ク文を実行するように実装を追加します。
- **Visit と Pet のエンティティ拡張**:
  - `Visit.kt` に `id` フィールドを追加し、永続化時の識別を可能にします。
- **Coroutine ベースへの統一**:
  - コントローラやリポジトリのメソッドはすべて `suspend fun` とし、ノンブロッキングな Coroutine で実装・処理を揃えます。

## Risks / Trade-offs

- **Risk: Visit の N+1 やネストされたデータ取得の複雑化**
  - Owner → Pet → Visit とネストされたデータを UI で表示する場合、単純に分割クエリを発行すると N+1 問題を引き起こします。
  - **Mitigation**: jOOQ の `MULTISet` や既存の `findAll` 系のクエリ実装を参考に、必要な情報を1回ないしは最小限のクエリで効率的にフェッチしてマッピングするようなクエリを構築します。
- **Risk: PetType との不整合**
  - Pet 更新時に意図しない種別（PetType）が指定された場合などにエラーとなる可能性があります。
  - **Mitigation**: コントローラ層で参照実装同様に入力バリデーションや例外処理を実装します。
