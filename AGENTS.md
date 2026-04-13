# TestClock - Agent Instructions

A single-class library containing `TestClock` - a custom `Clock` implementation to be used in unit tests.

## Common commands

```bash
# Default Gradle tasks - spotlessApply and build
./gradlew

# Apply code formatting (Spotless) without building
./gradlew spotlessApply

# Build, lint (Spotless), and test
./gradlew build

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests "io.github.malczuuu.testclock.TestClockTests"

# Run a single test method
./gradlew test --tests "io.github.malczuuu.testclock.TestClockTests.forwardSecondsAdvancesTimeBySeconds"
```

## Architecture

This is a single-class library. The entire public API lives in `TestClock.java`.

**Multi-release JAR setup** - the build produces a multi-release JAR (MR-JAR):

- `src/main/java/` - Java 8 sources, compiled with `--release 8`
- `src/main9/java/` - Java 9+ sources; only `module-info.java` lives here, compiled with `--release 9` and packaged into
  `META-INF/versions/9/` of the JAR

## Build & Validate

- **Always run `./gradlew`** (default tasks: `spotlessApply build`) to format, compile, and test.
- If Spotless fails, run `./gradlew spotlessApply` to auto-fix, then re-run `./gradlew`.
- Java 17+ required for Gradle runtime; code compiles to Java 8 bytecode.
- Dependencies: `gradle/libs.versions.toml`. Refresh with `./gradlew --refresh-dependencies`.
- Always validate changes with a full `./gradlew` run before considering a task complete.

## Project Layout

| Path               | Contents                        |
|--------------------|---------------------------------|
| `src/main/java`    | Production source               |
| `src/test/java`    | Tests (JUnit Jupiter + AssertJ) |
| `build.gradle.kts` | Build config & Spotless setup   |
| `buildSrc`         | Custom Gradle plugins/scripts   |

## Agent Rules

- Do not use terminal commands (e.g., `cat`, `find`, `ls`) to read or list project files - use IDE/agent tools instead.
- Run tests once, save output to `build/test-run.log` inside the repo (`> build/test-run.log 2>&1`), then read from that
  file to extract errors. Never run the same test command multiple times, without changes in sources. Store test output
  in multiple files if you want to compare before/after changes (ex. `build/test-run-{i}.log`).

## Coding Rules

- No self-explaining comments - only add comments for non-obvious context.
- No wildcard imports.
- Follow existing code patterns and naming conventions.
- Let `spotlessApply` handle all formatting - never format manually.

## Test Conventions

- Method naming: `givenThis_whenThat_thenWhat`.
- No `// given`, `// when`, `// then` section comments.
- Cover both positive and negative cases.
- Use AssertJ for assertions.
