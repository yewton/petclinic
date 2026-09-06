## ADDED Requirements

### Requirement: Declared Daemon JVM
The build MUST declare the JVM version required to run Gradle, so that the build does not depend on which Java version happens to be installed in the environment.

#### Scenario: Environment has no matching daemon JVM
- **WHEN** a build is started with a launcher JVM that satisfies Gradle's runtime requirement but does not match the declared daemon JVM version
- **AND** toolchain auto-provisioning is available
- **THEN** Gradle acquires a compatible JVM using the declared criteria
- **AND** the build proceeds without manual JDK installation

#### Scenario: Auto-provisioning is unavailable
- **WHEN** a build is started with a launcher JVM that satisfies Gradle's runtime requirement but without the declared daemon JVM version
- **AND** toolchain auto-provisioning is unavailable
- **THEN** Gradle fails before evaluating the build settings
- **AND** the failure identifies the missing daemon JVM requirement

#### Scenario: Environment already has the matching daemon JVM
- **WHEN** a build is started in an environment that already provides the declared daemon JVM version
- **THEN** Gradle uses the existing installation
- **AND** no JDK is downloaded

### Requirement: Separation From Project Toolchain
The declared daemon JVM MUST remain independent of the Java toolchain used to compile the project, so that changing one does not force a change to the other.

#### Scenario: Project toolchain is newer than the daemon JVM
- **WHEN** the project declares a Java toolchain newer than the JVM running the daemon
- **THEN** the project is compiled with the declared toolchain
- **AND** the daemon continues to run on its own declared JVM
