## ADDED Requirements

### Requirement: Pet Update
The system MUST allow users to update existing pet information including name, birth date, and type.

#### Scenario: Successful pet update
- **WHEN** user submits the pet update form with valid data
- **THEN** the system updates the pet's information in the database
- **AND** redirects back to the owner's details page

#### Scenario: Duplicate pet name
- **WHEN** user submits the pet update form with a name that already belongs to another pet owned by the same owner
- **THEN** the system shows a validation error for the duplicate name
- **AND** the pet information is not saved

#### Scenario: Invalid birth date
- **WHEN** user submits the pet update form with a birth date in the future
- **THEN** the system shows a validation error for the invalid date
- **AND** the pet information is not saved
