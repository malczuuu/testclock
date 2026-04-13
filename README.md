# TestClock

A mutable & playable `java.time.Clock` for unit tests.

## Table of Contents

- [Why to use `TestClock`](#why-to-use-testclock)
- [Installation](#installation)
- [Usage](#usage)
  - [Creating a `TestClock` object](#creating-a-testclock-object)
  - [Moving time forward](#moving-time-forward)
  - [Moving time backward](#moving-time-backward)
  - [Setting time directly](#setting-time-directly)
  - [Auto-ticking](#auto-ticking)
  - [Inspecting the current time without side effects](#inspecting-the-current-time-without-side-effects)

## Why to use `TestClock`

`Clock.fixed()` from the standard library is immutable. The common workaround - replacing the `Clock` reference between
test steps - does not work when the object under test holds its own reference and calls `Clock.instant()` multiple
times.

This may (or may not) be an issue. For situations where it is, `TestClock` solves this by being mutable in place. You
inject it once and control time by calling `forward`, `rewind`, or `setCurrentTime` directly on the same instance the
production code holds.

It also supports auto-ticking: each call to `instant()` can advance the clock by a configured duration. This is useful
for testing code that calls `Clock.instant()` multiple times and expects time to progress between calls - something
neither `Clock.fixed()` nor `Clock.systemUTC()` can simulate deterministically.

```java
TestClock clock = TestClock.from(Instant.parse("2025-01-01T00:00:00Z"));
PaymentService service = new PaymentService(clock);

Invoice invoice = service.createInvoice();
clock.forwardSeconds(30);
Receipt receipt = service.pay(invoice);

assertThat(receipt.getProcessedAt()).isAfter(invoice.getCreatedAt());
```

## Installation

Requires Java 8 or later.

> **JSpecify compliance** - the entire public API is annotated with `@NullMarked`. All parameters and return types are
> non-null by default. Tools that understand JSpecify (e.g., NullAway, IntelliJ IDEA) will surface nullability errors at
> compile time.

**Maven**

```xml
<dependency>
    <groupId>io.github.malczuuu</groupId>
    <artifactId>testclock</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

**Gradle**

```kotlin
testImplementation("io.github.malczuuu:testclock:1.0.0")
```

## Usage

### Creating a `TestClock` object

```java
// Fixed to a specific instant (UTC)
TestClock clock = TestClock.from(Instant.parse("2025-06-15T10:00:00Z"));

// Fixed to a specific instant in a given zone
TestClock clock = TestClock.from(Instant.parse("2025-06-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));

// Snapshot of an existing Clock
TestClock clock = TestClock.from(Clock.systemUTC());

// Current UTC system time
TestClock clock = TestClock.fromSystemUTC();
```

### Moving time forward

```java
clock.forward(Duration.ofMinutes(5));
clock.forwardSeconds(30);
clock.forwardMillis(500);
clock.forwardNanos(1_000_000L);
```

All `forward` methods return the resulting `Instant`.

**Note** that these methods do not validate if the input is positive. You can just as much call `forwardSeconds(-10)`
to move time backward by 10 seconds. It is up to developer to choose what's more convenient for the test scenario.

### Moving time backward

```java
clock.rewind(Duration.ofHours(1));
clock.rewindSeconds(10);
clock.rewindMillis(250);
clock.rewindNanos(500_000L);
```

All `rewind` methods return the resulting `Instant`.

**Note** that these methods do not validate if the input is positive. You can just as much call `rewindSeconds(-5)` to
move time forward by 5 seconds. It is up to developer to choose what's more convenient for the test scenario.

### Setting time directly

```java
clock.setCurrentTime(Instant.parse("2025-12-31T23:59:59Z"));
```

### Auto-ticking

By default the clock does not advance on its own. You can configure it to advance by a fixed duration on each call to
`instant()`:

```java
TestClock clock = TestClock.from(Instant.parse("2025-01-01T00:00:00Z"))
    .withTickDuration(Duration.ofSeconds(1));

clock.instant(); // 2025-01-01T00:00:00Z
clock.instant(); // 2025-01-01T00:00:01Z
clock.instant(); // 2025-01-01T00:00:02Z
```

Method `withTickDuration` returns a new `TestClock` and does not modify the original.

### Inspecting the current time without side effects

`Clock.instant()` auto-ticks if a tick duration is configured (see above). Use `getCurrentTime()` to read the current
time without advancing it.

```java
Instant now = clock.getCurrentTime();
```

## Building from source

<details>
<summary><b>Expand...</b></summary>

Gradle **9.x+** requires **Java 17** or higher to run. For building the project, Gradle automatically picks up **Java
25** via **toolchains** and the `foojay-resolver-convention` plugin. This Java version is needed because the project
uses **ErrorProne** and **NullAway** for static nullness analysis.

The produced artifacts are compatible with **Java 8** thanks to `options.release = 8` in the Gradle `JavaCompile` task.
This means that regardless of the Java version used to run Gradle, the resulting bytecode remains compatible.

The **default Gradle tasks** include `spotlessApply` (for code formatting) and `build` (for compilation and tests).The
simplest way to build the project is to run:

```bash
./gradlew
```

---

To **execute tests** use `test` task. Tests do not change `options.release` so newer Java API can be used.

```bash
./gradlew test
```

---

To **format the code** according to the style defined in [`build.gradle.kts`](./build.gradle.kts) rules use `spotlessApply`
task. **Note** that **building will fail** if code is not properly formatted.

```bash
./gradlew spotlessApply
```

---

To **publish** the built artifacts to **local Maven repository**, use `publishToMavenLocal` task.

```bash
./gradlew publishToMavenLocal
```

Note that for using Maven Local artifacts in target projects, you need to add `mavenLocal()` repository.

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
```

</details>
