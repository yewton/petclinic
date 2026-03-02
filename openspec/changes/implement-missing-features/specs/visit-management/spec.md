## ADDED Requirements

### Requirement: Add New Visit
The system MUST allow users to add new visit records for a specific pet.

#### Scenario: Successful visit creation
- **WHEN** user submits the new visit form with a valid date and description
- **THEN** the system saves the new visit in the database
- **AND** redirects back to the owner's details page

#### Scenario: View visits list
- **WHEN** user views the owner details page
- **THEN** the system displays a list of visits associated with each of the owner's pets

#### Scenario: Missing description
- **WHEN** user submits the new visit form with an empty description
- **THEN** the system shows a validation error
- **AND** the visit information is not saved
