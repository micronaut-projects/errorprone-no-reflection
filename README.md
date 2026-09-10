<!-- Checklist: https://github.com/micronaut-projects/micronaut-core/wiki/New-Module-Checklist -->

# ErrorProne No Reflection

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.errorprone/micronaut-errorprone-no-reflection.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.errorprone%22%20AND%20a:%22micronaut-errorprone-no-reflection%22)
[![Build Status](https://github.com/micronaut-projects/errorprone-no-reflection/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/errorprone-no-reflection/actions)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micronaut.io/scans)

An [ErrorProne](https://errorprone.info) check, `NoReflection`, that reports reflection: a call, a method reference or a constructor that reaches for it, named by the category it belongs to - most of them a cache the virtual machine fills for a class or a member. A project can allow categories, calls, classes and packages, and choose whether a suppression is honoured.

```kotlin
plugins {
    java
    id("io.micronaut.errorprone.no-reflection") version "1.0.0"
}

noReflection {
    allow("ENUM_CONSTANTS")
    allowIn("com.example.ReflectionAccess")
}
```

The check can also be added to ErrorProne on its own, as `errorprone("io.micronaut.errorprone:micronaut-errorprone-no-reflection:1.0.0")`, and configured with `-XepOpt:NoReflection:*` flags.

## Documentation

See the [Documentation](https://micronaut-projects.github.io/errorprone-no-reflection/latest/guide/) for more information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/errorprone-no-reflection/snapshot/guide/) for the current development docs.

## Snapshots and Releases

Snapshots are automatically published to [Sonatype Snapshots](https://s01.oss.sonatype.org/content/repositories/snapshots/io/micronaut/) using [GitHub Actions](https://github.com/micronaut-projects/errorprone-no-reflection/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to Maven Central via [GitHub Actions](https://github.com/micronaut-projects/errorprone-no-reflection/actions).

Releases are completely automated. To perform a release use the following steps:

* [Publish the draft release](https://github.com/micronaut-projects/errorprone-no-reflection/releases). There should be already a draft release created, edit and publish it. The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/errorprone-no-reflection/actions?query=workflow%3ARelease) to check it passed successfully.
* If everything went fine, [publish to Maven Central](https://github.com/micronaut-projects/errorprone-no-reflection/actions?query=workflow%3A"Maven+Central+Sync").
* Celebrate!
