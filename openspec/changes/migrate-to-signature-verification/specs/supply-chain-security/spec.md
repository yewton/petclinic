## Requirements

### Requirement: Signed dependencies can be verified by trusted keys

The build SHALL be able to verify signed dependencies using explicitly trusted PGP keys and SHALL NOT retrieve keys from key servers at verification time.

#### Scenario: A dependency is signed by a trusted key

- **WHEN** a dependency is signed by a key trusted for its group
- **THEN** Gradle verifies the signature using the local armored keyring

### Requirement: Unsigned artifacts retain checksum verification

The build SHALL continue checksum verification for artifacts that do not provide a usable signature, including plugin marker artifacts where applicable.

#### Scenario: An unsigned plugin marker is resolved

- **WHEN** a plugin marker has no usable PGP signature
- **THEN** Gradle verifies it using an approved checksum rule
