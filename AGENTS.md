# Repository Guidance

This repository publishes `micronaut-errorprone-no-reflection`, an ErrorProne check that reports reflection, and `micronaut-errorprone-no-reflection-gradle-plugin`, a Gradle plugin that applies and configures it. The Micronaut conventions prefix every artifact with `micronaut-`. It was created from `micronaut-project-template`; keep root guidance short.

## Repository Shape

- `errorprone-no-reflection/` is the `NoReflection` check: `ReflectionCategory` declares the categories and the flags, `ReflectionMatchers` the calls of each category as `CallPattern`s, `ReflectionPolicy` what a build allows, and `NoReflection` the check and its suppression.
- `errorprone-no-reflection-gradle-plugin/` is `io.micronaut.errorprone.no-reflection`: `NoReflectionExtension` is the `noReflection` block and `NoReflectionPlugin` turns it into flags. Its TestKit specs live in `src/functionalTest` and compile against the check this build has just built.
- `errorprone-no-reflection-bom/` is the BOM.
- `buildSrc/src/main/groovy/io.micronaut.build.internal.errorprone-no-reflection-*.gradle` are the convention plugins.

## Changing What Is Reported

- A call belongs in `ReflectionMatchers`, under the category whose cache it fills or whose kind it is. Categories are tried in the order they are declared, so a call two of them name is reported under the first.
- Every category has a sample in `NoReflectionTest.EVERY_CATEGORY`. A type javac warns about, such as `sun.misc.Unsafe`, must be named only on a line marked `// BUG: Diagnostic contains:`.
- Keep `src/main/docs/guide/categories.adoc` in step with the categories, and `flags.adoc` and `gradlePlugin.adoc` with the flags and the `noReflection` block.

## Template And Sync Rules

- Files copied by `micronaut-project-template`'s `files-sync.yml`, such as the workflows and `config/`, are changed in the template, not here.

## Verification

- Use `./gradlew check` for checkstyle, the check's tests and the plugin's TestKit specs.
- Use `./gradlew publishGuide` after guide or `toc.yml` changes.
