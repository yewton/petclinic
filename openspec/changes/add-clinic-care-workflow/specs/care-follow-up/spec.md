## ADDED Requirements

### Requirement: Veterinarians can record a minimal care outcome
The system SHALL allow a veterinarian to record the responsible veterinarian, care outcome, owner-facing summary, medication instructions, and follow-up due date on a care encounter. The system MUST keep internal outcome text distinct from the owner-facing summary.

#### Scenario: Record next action after consultation
- **WHEN** a veterinarian records an owner-facing summary and a follow-up due date for an encounter in consultation
- **THEN** the system SHALL retain the information on that encounter and make the appointment ready for payment handling

### Requirement: Medication instructions have an explicit duration
The system SHALL allow a veterinarian to record each medication instruction with its name, dose or free-text instruction, frequency, start date, and end date. The system MUST NOT calculate a dose or recommend a medication.

#### Scenario: Record a time-bounded medication instruction
- **WHEN** a veterinarian enters a medication name, instruction, frequency, start date, and end date
- **THEN** the system SHALL include that instruction in the encounter and its owner-facing summary

### Requirement: Reception can act on due follow-ups
The system SHALL present reception with encounters whose follow-up due date is within the configured review window or overdue, including the owner, pet, due date, and latest owner-facing next action.

#### Scenario: Review an overdue follow-up
- **WHEN** reception opens the follow-up list after an encounter's due date has passed
- **THEN** the system SHALL show the encounter as overdue and allow reception to record that follow-up was contacted

### Requirement: Staff can print a confirmed owner summary
The system SHALL provide a printable summary for a completed encounter containing only the pet identity, visit date, responsible veterinarian, owner-facing summary, medication instructions, and follow-up due date. The system MUST NOT include internal outcome text in the printed summary.

#### Scenario: Print a take-home summary
- **WHEN** staff select print for an encounter with a confirmed owner summary
- **THEN** the system SHALL render a printable document containing the permitted owner-facing information

### Requirement: Historical care data is preserved
The system MUST retain existing legacy visit records and MUST prevent deletion of a care encounter that has a recorded outcome or owner-facing summary. Corrections to a confirmed owner-facing summary SHALL be recorded as an additive correction with its creation time.

#### Scenario: Correct a confirmed summary
- **WHEN** staff correct a confirmed owner-facing summary
- **THEN** the system SHALL retain the original summary and show the later correction with its creation time
