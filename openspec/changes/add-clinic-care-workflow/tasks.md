## 1. Workflow foundations

- [ ] 1.1 Inspect the existing Core repositories and staff-facing application patterns, then define migrations for appointments, encounters, medication instructions, follow-up contacts, and payment statuses without changing legacy visits.
- [ ] 1.2 Add domain models, status enums, validation, and persistence constraints for scheduled appointments and actual care encounters.
- [ ] 1.3 Add repository operations that verify owner, pet, appointment, and encounter relationships before every workflow update.
- [ ] 1.4 Preserve legacy visits and add tests proving that new workflow operations neither delete nor rewrite them.

## 2. Booking requests and the daily board

- [ ] 2.1 Add the reception flow for creating a booking request for an existing owner and pet.
- [ ] 2.2 Add scheduling, cancellation, and no-show updates with validation and repository tests.
- [ ] 2.3 Add the selected-day appointment board with owner, pet, requested reason, scheduled time, and status.
- [ ] 2.4 Add check-in, waiting, and consultation handoffs that create or link one encounter, with integration tests for the complete handoff.

## 3. Care outcome and follow-up

- [ ] 3.1 Add the clinical encounter form for a veterinarian to record the care outcome, owner-facing summary, and follow-up due date.
- [ ] 3.2 Add medication instruction persistence and form fields for name, instruction, frequency, start date, and end date without clinical recommendations.
- [ ] 3.3 Add confirmation and additive-correction behavior for owner-facing summaries, including tests that preserve the original summary.
- [ ] 3.4 Advance completed consultations to payment-pending and verify state transitions through repository and controller tests.

## 4. Reception follow-up and owner summary

- [ ] 4.1 Add a reception follow-up list for encounters due within the configured review window or overdue.
- [ ] 4.2 Add a workflow to record that reception contacted the owner about a due follow-up.
- [ ] 4.3 Add a printable encounter summary containing only permitted owner-facing fields.
- [ ] 4.4 Add integration tests proving that the printable summary excludes internal clinical outcome text.

## 5. Payment-status handoff

- [ ] 5.1 Add a non-negative billed amount and payment status to completed encounters.
- [ ] 5.2 Add reception-facing payment work lists for payment-pending, unpaid, and partially-paid encounters.
- [ ] 5.3 Add integration tests for payment recording, appointment completion, and the prohibition on payment-card or payment-processing data.

## 6. Quality and workflow review

- [ ] 6.1 Add WebTestClient coverage for normal and invalid cross-owner, cross-pet, and cross-encounter workflow updates.
- [ ] 6.2 Apply formatting and run the full Gradle check suite.
- [ ] 6.3 Review the booking, arrival, next-action, follow-up, printed-summary, and payment workflows with a receptionist, veterinarian, and pet owner; capture resulting backlog changes in a follow-up proposal.
