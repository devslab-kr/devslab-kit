# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The library major aligns with the Spring Boot major: `4.x.y` targets Spring Boot 4.x.

## [Unreleased]

### Added
- Initial project scaffold (Spring Boot 4 + Java 25 + Gradle).
- Base dependencies: Spring Web MVC, Spring Security, Spring Data JPA, Spring Data Redis,
  Flyway (PostgreSQL), Spring Boot Actuator, GraalVM Native, Testcontainers (PostgreSQL + Redis),
  Docker Compose support.
- Base package `kr.devslab.kit`.

### Notes
- This is the pre-`0.1.0` scaffold. Multi-module split, public contracts
  (`CurrentUserProvider`, `PermissionChecker`, `TenantResolver`, `MenuProvider`,
  `AuditEventPublisher`), AutoConfiguration, and the sample app come in subsequent PRs.
