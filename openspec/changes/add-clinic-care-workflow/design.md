## Context

The shared Core currently stores owners, pets, veterinarians, and legacy visits. A legacy visit has only a date and free-text description, so it cannot distinguish an upcoming booking from a patient's arrival, care outcome, follow-up, or payment state. This change implements the first product workflow without selecting a lasting presentation technology for the product architecture.

The complete product goal is to let a small clinic carry outpatient care from a booking request through follow-up, while giving the owner an appropriate secure view of confirmed care information. [`product-backlog.md`](product-backlog.md) defines that complete target and [`to-be-architecture/`](to-be-architecture/) visualizes it. The first slices are deliberately staff-operated so they can be reviewed with a receptionist and veterinarian before introducing owner authentication and a public portal.

## Goals / Non-Goals

**Goals:**

- Deliver staff-visible, end-to-end increments rather than standalone platform components.
- Preserve legacy visit history while introducing separate scheduled-care and actual-care records.
- Keep the same case visible to reception and clinical staff as it moves through the day.
- Record an owner-understandable next action and follow-up date, then make it available as a printable summary.
- Record the amount and payment state of completed care without becoming a payment system.

**Non-Goals:**

- Owner accounts, public self-service booking, online payment, automatic notifications, and a separate owner-portal deployment in this initial implementation. These are part of the product To-Be, with their order defined by the product backlog.
- A complete electronic medical record, diagnostic decision support, prescription system, medication master, insurance claims, inventory, or external accounting integration.
- Migration or destructive rewriting of existing `visits` data.

## Decisions

### Define the complete product target before implementation slicing

The To-Be model treats PetClinic as one Software System because a clinic adopts and operates the complete care workflow as one product. Its containers make the security and data boundaries explicit: Care API and Clinic Care Database own clinical and financial records; the owner portal provides the public interaction; Owner Care API exposes only purpose-specific, published information and owns consent, contact preferences, and delivery history through the engagement store. The owner-facing containers do not directly access the clinic database. Presentation technologies are intentionally omitted from this product architecture.

`tasks.md` deliberately covers only the first staff-operated increments of that model. The product backlog remains the ordering mechanism for later increments and is revised when workflow review changes the evidence.

### Build vertical workflow slices

Each implementation increment MUST be reviewable through a receptionist, veterinarian, or owner action. The initial sequence is: record a booking request and view it on the day list; check in and expose the waiting queue; record a care outcome and next action; work a due-follow-up list; print an owner summary; then record payment status.

This avoids making authentication, generic audit infrastructure, or database-only Appointment/Encounter abstractions the first reviewable result. Integrity checks, tests, timestamps, and actor attribution needed by a delivered slice are part of that slice's definition of done.

### Separate scheduled care from actual care while retaining legacy visits

An appointment represents a requested or scheduled future clinic interaction. A care encounter represents the actual outpatient interaction and is linked to one appointment when the patient arrives. The encounter owns the clinical outcome, follow-up, owner summary, and payment status. Existing `Visit` records remain historical data and are not repurposed as appointments or encounters.

The alternative of extending `Visit` was rejected because a date plus free text cannot safely represent both planned and completed work or their distinct state transitions.

### Use a finite staff workflow for the daily board

Appointments progress through `REQUESTED`, `SCHEDULED`, `CHECKED_IN`, `WAITING`, `IN_CONSULTATION`, `PAYMENT_PENDING`, `COMPLETED`, `CANCELED`, or `NO_SHOW`. A receptionist records request, scheduling, arrival, cancellation, and no-show states. Clinical staff advance a waiting patient into and out of consultation. Completing care leaves the appointment payment-pending until a payment state is recorded.

The board is a shared operational projection, not a separate queueing service.

### Start owner value with a printable summary

The encounter holds a veterinarian-confirmed owner summary, medication instructions, and follow-up due date. Staff can print it directly for an owner. This tests whether the information is understandable and useful without prematurely selecting owner identity, public exposure, and notification architecture.

### Keep payment as a recorded state

An encounter can have an amount and one of `UNPAID`, `PARTIALLY_PAID`, `PAID`, or `WAIVED`. No payment method, transaction processing, or accounting export is introduced. This provides the minimum operational handoff from care to reception.

## Risks / Trade-offs

- [No authentication in this change] → The workflow MUST be treated as an internal, non-production-data pilot until an access-control change is delivered. Public or owner-facing endpoints MUST NOT be introduced.
- [Clinical details are intentionally minimal] → Store only clinician-entered narrative instructions and dates; do not infer diagnoses, dosage, urgency, or treatment decisions.
- [A busy clinic can have walk-ins and emergency cases] → Permit reception to create an appointment request on the same day; defer triage rules and automatic prioritization.
- [A printed summary can be lost] → Treat it as a validation step for a future authenticated portal, not a replacement for secure access controls.
- [Legacy data has weaker integrity] → Do not delete or transform legacy visits; validate all new record relationships at both request and persistence boundaries.
- [A status model can become too rigid] → Keep the initial states limited to handoffs used by the daily workflow; refine after staff review.

## Migration Plan

1. Add new tables and foreign keys for appointments, encounters, encounter medication instructions, and payment status without altering legacy visits.
2. Deploy staff-facing routes and views behind the existing internal application boundary.
3. Seed or manually create a small set of representative booking requests for review; do not migrate existing visits into encounters.
4. Run receptionist and veterinarian workflow reviews using the first vertical slices before expanding the model.
5. Roll back by disabling the new staff-facing routes and leaving the new records intact; no existing owner, pet, or visit data is modified.

## Open Questions

- What minimum legal and clinic policy requirements apply to retention, correction history, and actor attribution before real clinical data is entered?
- Which staff roles beyond receptionist and veterinarian need daily-board access, and how will authentication be introduced after the workflow has been validated?
- Is a single follow-up date sufficient for the first review, or must one encounter support multiple independently tracked actions?
- What printed fields are clear enough to owners without exposing internal clinical notes?
