# Changelog

<!---
https://keepachangelog.com/
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
--->

## [v1.8.0] - 2023-05-26

### Added

- Add support for interactions with config-server
    - Access token management on local Android device
    - Key pair management on local Android device
    - Public key management on the config-server
    - Download SINETStream parameters along with secrets from the config-server

### Changed

- Hide debug logging unless otherwise explicitly specified.
- Update build environ


## [v1.6.0] - 2021-12-22

### Added

- Support for getting SSL/TLS certificates from the Android Keystore.
- Support for data encryption/decryption.
- Add type-specific Reader/Writer classes extended from generic ones.

### Changed

- build.gradle: Use MavenCentral instead of jCenter
- build.gradle: Use JDK 11 instead of JDK 8, from Android Studio Arctic Fox.

- misc/Dockerfile: Update `openjdk`, `Command line tools` and `SDK Build Tools`.
- API: Split initialization process to 2 phases; initialize() and setup().
- API: User can now abort initialization process if something goes wrong.

### Fixed

- Make MQTT connection setup/close in robust way.
- Add missing try-catch clause.
- Resolve NullpointerException cases.
- Resolve some lint warnings.


## [v1.5.2] for Android - 2021-05-20

### Changed

- build.gradle: Update build environ for the Android Studio 4.1.2.
- AndroidConfigLoader: Rewrite usage of obsoleted Kotlin functions.

### Fixed

- CipherXXX: Resolve implementation compatibility issues (work in progress).
- MqttAsyncMessageIO: Now user can abort the ongoing connection request.


## [v1.4.0] - 2020-10-08

### Added

- Initial release
    - Limitations:
        - Data encryption is not implemented.

