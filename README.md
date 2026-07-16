[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-jcr-base/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-jcr-base/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-jcr-base/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-jcr-base/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-jcr-base&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-jcr-base)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-jcr-base&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-jcr-base)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.jcr.base.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.jcr.base)&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.jcr.base/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.jcr.base%22)&#32;[![jcr](https://sling.apache.org/badges/group-jcr.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/jcr.md) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling JCR Base Bundle

This module is part of the [Apache Sling](https://sling.apache.org) project.

The JCR base bundle provides JCR utility classes, base implementations for `SlingRepository`, `loginAdministrative` allow-list enforcement, repository initializer execution, node type and namespace loading helpers, repository status printer support, and repository mount integration for JCR-based legacy access.

# Build

```bash
mvn clean package -DskipTests
```

# Test

```bash
mvn clean verify
```

```bash
mvn test
mvn test -Dtest=LoginAdminAllowListTest
mvn test -Dtest=LoginAdminAllowListTest#testAllowList
mvn test -Dtest=RepositoryInitializersTest
mvn test -Dtest=RepositoryMountTest
mvn test -Dtest=NodeTypeLoaderTest
mvn test -Dtest=AccessControlUtilTest
```

```bash
mvn test jacoco:report
```

# Code Quality

```bash
mvn spotless:check
mvn apache-rat:check
mvn spotless:apply
```

```bash
mvn javadoc:javadoc
```

# Requirements

* Java 8 source/target
* Maven build for an OSGi bundle (Sling bundle parent)
* OSGi Declarative Services and Metatype annotations (`org.osgi.service.component.annotations`, `org.osgi.service.metatype.annotations`)
* Optional Jackrabbit RMI support via `jackrabbit-jcr-rmi` (provided scope, optional package import)
* JUnit 4 with Sling OSGi/Sling/JCR mock test tooling and Mockito for tests

# Main Components

* `AbstractSlingRepository2` and `AbstractSlingRepositoryManager` provide the core Sling repository base implementation and lifecycle integration.
* `LoginAdminAllowList`, `AllowListFragment`, and `LegacyFragment` enforce and bridge `loginAdministrative` allow-list configuration across modern and legacy property names.
* `SlingRepositoryInitializer` services are tracked and executed during repository startup, ordered by OSGi service ranking.
* `NodeTypeLoader` and `internal.loader.Loader` register CND node types and JCR namespaces from bundle headers.
* `org.apache.sling.jcr.base.spi.RepositoryMount` and the internal proxy classes support JCR repository mounts through a single active mount selected by service ranking.
* `internal.RepositoryPrinterProvider` and `internal.RepositoryPrinter` expose repository information to the Felix Web Console.
* `util.AccessControlUtil` and `util.RepositoryAccessor` provide reusable JCR access-control and repository lookup utilities.

# Project Structure

* `src/main/java/org/apache/sling/jcr/base` - bundle implementation, internal services, SPI, and utility classes
* `src/test/java` - JUnit 4 tests mirroring package structure with Sling testing mocks
* `pom.xml` - Maven build (Sling bundle parent)
* `bnd.bnd` - OSGi manifest instructions

# Login Administrative Allow List

`loginAdministrative` is protected by an allow list and is disabled unless both repository-manager settings and allow-list rules permit access.

Current configuration uses allow-list naming:

* Main PID: `org.apache.sling.jcr.base.LoginAdminAllowList`
* Fragment factory PID: `org.apache.sling.jcr.base.LoginAdminAllowList.fragment`

Legacy whitelist PIDs and properties are still supported for backward compatibility, but they are deprecated.

Also supported for backward compatibility:

* Legacy main PID: `org.apache.sling.jcr.base.internal.LoginAdminWhitelist`
* Legacy fragment factory PID: `org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment`

# Repository Mount

Apache Sling provides support for pluggable resource providers. While this allows for a very flexible and efficient
integration of custom data providers into Sling, this integration is done on Sling's resource API level. Legacy code
which may rely on being able to adapt a resource into a JCR node and continue with JCR API will not work with such
a resource provider.

To support legacy code, this bundle provides an SPI interface *org.apache.sling.jcr.base.spi.RepositoryMount* which
extends *JackrabbitRepository* (and through this *javax.jcr.Repository*). A service registered as *RepositoryMount* registers
itself with the service registration property *RepositoryMount.MOUNT_POINTS_KEY* which is a String+ property containing
the paths in the JCR tree where the mount takes over the control of the JCR nodes. The *RepositoryMount* can be registered
at a single path or multiple.

The JCR base implementation uses a single active mount. If multiple `RepositoryMount` services are available, the one with the highest OSGi service ranking is used.

As *RepositoryMount* extends *JackrabbitRepository* the implementation of a mount needs to implement the whole JCR API.
This is a lot of work compared to a *ResourceProvider*, therefore a *RepositoryMount* should only be used if legacy
code using JCR API needs to be supported.

# Node Types and Namespaces

When present, the following bundle manifest headers are processed to register repository metadata:

* `Sling-Nodetypes`
* `Sling-Namespaces`

# Repository Initializers

Services implementing `org.apache.sling.jcr.api.SlingRepositoryInitializer` are executed at repository startup before the repository service is registered.

If an initializer throws an exception or error, repository service registration is aborted.

# Security

This module follows the Apache Sling threat model:

https://github.com/apache/sling/blob/master/docs/threat-model.md
