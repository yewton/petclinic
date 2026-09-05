## ADDED Requirements

### Requirement: Declared Daemon JVM
The build MUST declare the JVM version required to run Gradle, so that the build does not depend on which Java version happens to be installed in the environment.

#### Scenario: Environment has no compatible JVM
- **WHEN** a build is started in an environment whose default Java is older than the version Gradle requires
- **THEN** Gradle acquires a compatible JVM using the declared criteria
- **AND** the build proceeds without manual JDK installation

#### Scenario: Environment already has a compatible JVM
- **WHEN** a build is started in an environment that already provides a compatible JVM
- **THEN** Gradle uses the existing installation
- **AND** no JDK is downloaded

### Requirement: Separation From Project Toolchain
The declared daemon JVM MUST remain independent of the Java toolchain used to compile the project, so that changing one does not force a change to the other.

#### Scenario: Project toolchain is newer than the daemon JVM
- **WHEN** the project declares a Java toolchain newer than the JVM running the daemon
- **THEN** the project is compiled with the declared toolchain
- **AND** the daemon continues to run on its own declared JVM
