## ADDED Requirements

### Requirement: Reception can record a payment status for completed care
The system SHALL allow reception to record a non-negative billed amount and a payment status of `UNPAID`, `PARTIALLY_PAID`, `PAID`, or `WAIVED` for an encounter whose consultation is complete. The system MUST NOT process a payment or store payment-card data.

#### Scenario: Record payment after care
- **WHEN** reception records a billed amount and `PAID` status for a completed encounter
- **THEN** the system SHALL retain the amount and payment status and mark the related appointment `COMPLETED`

### Requirement: Reception can identify payment work
The system SHALL present encounters in `PAYMENT_PENDING` and encounters with an unpaid or partially paid status in a reception-facing list.

#### Scenario: View payments awaiting action
- **WHEN** reception opens the payment work list
- **THEN** the system SHALL show encounters awaiting payment handling or still unpaid
