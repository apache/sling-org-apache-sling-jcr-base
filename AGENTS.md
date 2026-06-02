# Project Overview

`org.apache.sling.jcr.base` is an OSGi bundle that provides JCR foundation classes for Apache Sling. It includes base classes for implementing `SlingRepository` (`AbstractSlingRepository2`, `AbstractSlingRepositoryManager`), `loginAdministrative` allow-list enforcement via OSGi component configuration, repository initializer execution (`SlingRepositoryInitializer` services), JCR node-type/namespace loading utilities, access-control helpers, and a proxy/mount layer that lets multiple JCR repositories appear as one. Components are wired at runtime via the OSGi service registry.

# Core Commands

```bash
# Build (compile + package, skip tests)
mvn clean package -DskipTests

# Full build with tests
mvn clean verify

# Run all tests
mvn test

# Run coverage report
mvn test jacoco:report

# Run a single test class
mvn test -Dtest=LoginAdminAllowListTest

# Run a single test method
mvn test -Dtest=LoginAdminAllowListTest#testAllowList

# Lint / code style (Spotless + RAT)
mvn spotless:check
mvn apache-rat:check

# Apply Spotless auto-formatting
mvn spotless:apply

# Generate Javadoc
mvn javadoc:javadoc
```

No dev server — this is a library bundle deployed into an OSGi container (e.g., Apache Felix / Karaf).

# Project Layout

```
pom.xml                          Maven build descriptor
bnd.bnd                          OSGi bundle manifest overrides
src/
  main/java/org/apache/sling/jcr/base/
    AbstractSlingRepository2.java      Base SlingRepository implementation
    AbstractSlingRepositoryManager.java  Manages repository lifecycle, initializers, and allow-list
    NodeTypeLoader.java                Registers CND node-type definitions and namespaces
    package-info.java                  Package-level OSGi version annotation
    internal/
      LoginAdminAllowList.java         Enforces loginAdministrative allow-list
      LoginAdminAllowListConfiguration.java  OSGi Metatype config interface
      AllowListFragment.java           Whiteboard fragment for allow-list entries
      LegacyFragment.java              Compatibility shim for old allow-list configs
      RepositoryPrinter*.java          Felix WebConsole status printer provider + printer
      loader/Loader.java               Bootstraps node types from bundle resources
      mount/Proxy*.java                Proxy wrappers for RepositoryMount SPI
      mount/ChainedIterator.java       Iterator utility for mount proxy traversal
    spi/
      RepositoryMount.java             SPI: plug in an additional JCR repository
    util/
      AccessControlUtil.java           JCR/Jackrabbit ACL helpers
      RepositoryAccessor.java          Repository lookup (JNDI / RMI / OSGi)
  test/java/…                          JUnit 4 tests mirroring the main package tree
    RepositoryInitializersTest.java    Verifies SlingRepositoryInitializer ordering/failure behavior
    internal/AllowListWiringTest.java  Verifies allow-list wiring across modern + legacy configs
target/                          Build output (gitignored)
```

# Development Patterns & Constraints

- **Java 8** source/target (`sling.java.version=8` in pom.xml). Do not use APIs above Java 8.
- **OSGi DS annotations only**: use `org.osgi.service.component.annotations.*` and `org.osgi.service.metatype.annotations.*`. No Felix SCR annotations.
- **4-space indentation**, no tabs. Spotless (Eclipse formatter) enforces this — run `mvn spotless:apply` after edits.
- Internal implementation classes live under `*.internal.*` packages; these are not exported and must not be referenced from outside.
- Public API packages carry `@Version` annotations in `package-info.java` — bump the version in that file and in `bnd.bnd` when changing API.
- Logging via SLF4J only (`org.slf4j.Logger`); never use `java.util.logging` or `System.out`.
- All source files require the Apache License 2.0 header; RAT enforces this.
- The `bnd.bnd` file declares `Import-Package` overrides (e.g., optional RMI import). Keep it in sync when adding new optional dependencies.
- Repository initializer execution order follows OSGi service ranking (`SlingRepositoryInitializer` with higher ranking runs first).

# Git Workflow

- Default branch: `master`
- Feature branches: `<JIRA-issue>/short-description` (e.g., `SLING-12345/fix-allowlist`)
- Commit messages: start with the JIRA issue key — `SLING-XXXXX: <imperative summary>`
- PRs target `master`; CI (Jenkins via `Jenkinsfile`) must pass before merge
- Do not push directly to `master`; use PRs

# Testing Guidelines

- Framework: **JUnit 4** with Mockito 5 and Sling OSGi Mock / Sling Mock / JCR Mock
- Test files mirror main sources under `src/test/java/…`
- OSGi component tests use `OsgiContext` (from `org.apache.sling.testing.osgi-mock.junit4`)
- `RepositoryInitializersTest` covers initializer ordering and repository registration failure paths.
- Allow-list behavior is covered by `LoginAdminAllowListTest`, `AllowListWiringTest`, and `LegacyFragmentTest`.
- Run coverage: `mvn test jacoco:report` (JaCoCo is inherited from the Sling bundle parent POM)
- Coverage report appears in `target/site/jacoco/`

# Gotchas

- **`loginAdministrative` is off by default**: bundles must register an `AllowListFragment` OSGi service to be permitted. Tests that call `loginAdministrative` without wiring the allow-list will get an exception.
- **Allow-list configuration names changed from "whitelist" to "allowlist"**: modern PIDs/properties are preferred, while legacy whitelist naming is still supported for compatibility.
- **OSGi mock version matters**: the tests use `osgi-mock.junit4` 3.x; mixing with 2.x artefacts breaks context setup.
- **RMI dependency is optional**: `jackrabbit-jcr-rmi` is `provided` scope and the Import-Package is `resolution:=optional`. Do not make it mandatory.
- **Repository startup is blocked on initializer failures**: exceptions/errors from `SlingRepositoryInitializer` prevent SlingRepository service registration.
- **Baseline check**: the parent POM runs OSGi semantic-version baseline against the previous release JAR. Adding API without bumping the package version will fail the build.
- **Spotless must pass before RAT**: run `mvn spotless:apply` before committing; otherwise the RAT XML-header check may report false positives on reformatted files.
- The `internal` packages are deliberately excluded from Javadoc generation (see `pom.xml` `maven-javadoc-plugin` config).
