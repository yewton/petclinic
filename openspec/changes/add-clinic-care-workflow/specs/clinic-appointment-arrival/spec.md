## ADDED Requirements

### Requirement: Reception can record a booking request
The system SHALL allow reception to create a booking request for an existing owner and pet with a requested date, requested time, and reason for visit. The system SHALL retain the request even when the requested slot cannot be scheduled.

#### Scenario: Record a phone booking request
- **WHEN** reception records an existing pet's requested date, requested time, and reason for visit
- **THEN** the system SHALL create an appointment in the `REQUESTED` state and show it in the clinic appointment register

### Requirement: Reception can schedule or close a booking request
The system SHALL allow reception to assign a requested appointment a scheduled date and time or mark it canceled or no-show. The system MUST reject an update whose appointment does not belong to the owner and pet identified by the request context.

#### Scenario: Confirm a requested appointment
- **WHEN** reception assigns a scheduled date and time to a requested appointment
- **THEN** the system SHALL show the appointment as `SCHEDULED` on that day's list

#### Scenario: Reject an unrelated appointment update
- **WHEN** a workflow update identifies an owner or pet that is not linked to the appointment
- **THEN** the system SHALL reject the update without changing the appointment

### Requirement: Staff can share a daily arrival board
The system SHALL present appointments scheduled for a selected clinic day with their owner, pet, scheduled time, reason for visit, and workflow state.

#### Scenario: View today's appointments
- **WHEN** a staff member opens the daily board for today
- **THEN** the system SHALL show all appointments scheduled for today, including their current workflow states

### Requirement: Reception and clinical staff can record the arrival handoff
The system SHALL allow reception to move a scheduled appointment through `CHECKED_IN` and `WAITING`, and allow clinical staff to move a waiting appointment into `IN_CONSULTATION`. The system SHALL create or link one care encounter when the patient is checked in.

#### Scenario: Check in a scheduled patient
- **WHEN** reception checks in a scheduled appointment
- **THEN** the system SHALL mark the appointment `CHECKED_IN`, create or link one care encounter, and make the appointment available to the waiting workflow

#### Scenario: Begin consultation from the waiting workflow
- **WHEN** clinical staff start consultation for a waiting appointment
- **THEN** the system SHALL mark the appointment `IN_CONSULTATION` and show the linked care encounter
