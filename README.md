[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-org-apache-sling-jcr-base/master)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-jcr-base/job/master) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-org-apache-sling-jcr-base/job/master.svg)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-jcr-base/job/master/test_results_analyzer/) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.jcr.base/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.jcr.base%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.jcr.base.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.jcr.base) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![jcr](https://sling.apache.org/badges/group-jcr.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/jcr.md)

# Apache Sling JCR Base Bundle

This module is part of the [Apache Sling](https://sling.apache.org) project.

The JCR base bundle provides JCR utility classes and support for repository mounts.

# Repository Mount

Apache Sling provides support for pluggable resource providers. While this allows for a very flexible and efficient
integration of custom data providers into Sling, this integration is done on Sling's resource API level. Legacy code
which may rely on being able to adapt a resource into a JCR node and continue with JCR API will not work with such
a resource provider.

To support legacy code, this bundle provides an SPI interface *org.apache.sling.jcr.base.spi.RepositoryMount* which
extends *JackrabbitRepository* (and through this *javax.jcr.Repository*). A service registered as *RepositoryMount* registers
itself with the service registration property *RepositoryMount.MOUNT_POINTS_KEY* which is a String+ property containing
the paths in the JCR tree where the mount takes over the control of the JCR nodes. The *RepositoryMount* can registered
at a single path or multiple.

As *RepositoryMount* extends *JackrabbitRepository* the implementation of a mount needs to implement the whole JCR API.
This is a lot of work compared to a *ResourceProvider*, therefore a *RepositoryMount* should only be used if legacy
code using JCR API needs to be supported.

