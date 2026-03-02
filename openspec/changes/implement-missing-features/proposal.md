## Why
参照実装である `spring-petclinic` と比較して、現在の `petclinic-fullstack` ではペットの登録情報の更新、およびペットの診療履歴（Visits）の追加・管理機能が未実装となっています。Spring Petclinic の完全な移植版とするために、これらの不足している機能を追加実装します。

## What Changes
1. **ペット情報の更新機能の追加**
   - 既存のペット情報を編集するための画面 (`/pets/{petId}/edit`) と、その処理エンドポイントを `PetController` に追加します。
   - `PetRepository` に `update` 処理（jOOQベース）を実装します。
   
2. **訪問履歴 (Visit) の管理機能の追加**
   - 新規の訪問履歴を記録する画面 (`/pets/{petId}/visits/new`) と処置エンドポイントを追加するための `VisitController` を新設します。
   - 飼い主とペットの詳細画面などに訪問履歴を表示するようにします。
   - `Visit` エンティティを適切にデータベースへ保存・取得するための `VisitRepository` を追加します。
   - `Pet` エンティティから `Visit` リストを取得できるようにします。

## Capabilities

### New Capabilities
- `pet-management`: ペットの追加に加え、既存のペット情報を更新できるようになります。
- `visit-management`: ペットの診療訪問履歴 (Visit) を記録し、表示・管理できるようになります。

### Modified Capabilities

## Impact
- `PetController` に編集エンドポイントが追加されます。
- `PetRepository` に未実装の `update` ロジックが追加されます。
- 新機能 `VisitController`, `VisitRepository` が追加されます。
- 既存の Thymeleaf テンプレート (`createOrUpdateVisitForm.html` など) へのパスがバックエンドでハンドリングされるようになります。
- データベースアクセスには既存の仕組み (jOOQ 等) を踏襲した実装が行われます。
