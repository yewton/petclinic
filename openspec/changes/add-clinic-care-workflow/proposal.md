## Why

The current application is a technical demonstration of a pet and owner register with a free-text visit history. It cannot support the minimal outpatient flow of a small companion-animal clinic: booking a visit, receiving the patient, recording the next clinical action, and following up with the owner.

This change establishes a first, reviewable clinic-care workflow. It is the first delivery increment toward the complete product To-Be and product backlog defined in [`product-backlog.md`](product-backlog.md); it prioritizes an observable staff-and-owner outcome over building generic platform capabilities in isolation.

## What Changes

- Add an internal appointment register that lets reception record a booking request and see it on the clinic's daily list.
- Add arrival and waiting-state handling so reception and clinical staff share the same view of who is ready to be seen.
- Add a minimal clinical follow-up record for the responsible veterinarian to capture the next action, follow-up due date, and an owner-facing take-home summary.
- Add a reception follow-up list for patients whose follow-up is due or overdue.
- Add a printed take-home summary for an owner; an authenticated owner portal, automated reminders, and online self-booking remain out of scope for this change.
- Add a minimal invoice amount and payment-status record after the care workflow is established, without payment processing or external accounting integration.
- Preserve existing historical visits while introducing scheduled and actual-care concepts only where a delivered workflow needs them.
- Define the complete product To-Be as a separate LikeC4 project in `to-be-architecture/`, without modifying the repository's existing architecture model.

## Capabilities

### New Capabilities

- `clinic-appointment-arrival`: Reception can register a booking request, maintain the daily list, and record arrival and waiting state.
- `care-follow-up`: Clinical staff can record a care outcome and follow-up due date, while reception and owners receive the information appropriate to their work.
- `care-payment-status`: Reception can record and review the amount and payment state associated with completed care.

### Modified Capabilities

None.

## Impact

- Affects the shared Core domain, database schema, and the staff-facing application; it introduces appointment and care workflow data alongside the existing owner, pet, and legacy visit data.
- Adds staff-facing views and a printable owner-facing summary.
- Requires validation of parent-child ownership on workflow updates and preservation of historical care data; it does not introduce a complete authentication, portal, payment, or notification platform.
- Adds a future-state architecture reference and a complete product backlog; neither changes the current runtime container set nor the current composite-build structure.
