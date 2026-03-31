# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- bump to latest version after entityMapping removal
- rename to http-plugin and add HttpTaskPlugin
- netty support for new version
- add new plugin http-provider

### Fixed

- remove patch support
- remove webflux dependency
- lint pom.xml to force new library release
- lint to force new library release
- remove check of state in delete endpoint
- update copyright headers
- reject error with valid message
- resolve sonar issues
- fix checkstyle errors

### Changed

- remove entityMapping logic
- install jptp dependency before running tests
- replace response-to-json task with json-parsing from jptp


