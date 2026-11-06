# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog][keepachangelog], and this project adheres to [Semantic Versioning][semver].

## [Unreleased]

### Changed

- Make `TestClock` implement `Serializable`.
- Make `TestClock` override `equals` and `hashCode`. Clocks are equal if they have the same current time, zone and tick
  duration.
- Methods `forward(TemporalAmount)`, `forward(long, TemporalUnit)`, `rewind(TemporalAmount)` and
  `rewind(long, TemporalUnit)` now apply the amount in the clock's zone. Day-based amounts (`Period.ofDays`,
  `ChronoUnit.DAYS`) follow the zone's calendar.
- Across a DST transition, `TestClock` preserves the local time of day instead of moving by exactly 24 hours. Use
  `Duration.ofDays` for exact 24-hour steps.

### Fixed

- Methods `forward` and `rewind` no longer throw `UnsupportedTemporalTypeException` for date-based amounts such as
  `Period.ofMonths(1)`, `Period.ofYears(1)`, `ChronoUnit.WEEKS`, `ChronoUnit.MONTHS` or `ChronoUnit.YEARS`.

## [1.0.0] - 2026-04-15

### Added

- `TestClock` - a mutable `java.time.Clock` implementation for use in unit tests, with full control over the current
  time and thread-safe time-mutation operations.
- Various factory methods for creating a `TestClock`.
- `getCurrentTime()` - reads the current instant without advancing the clock, regardless of any configured tick
  duration.
- `setCurrentTime()` - unconditionally replaces the current instant.
- `forward()` - advance the clock and return the new instant.
- `rewind()` - move the clock back and return the new instant.
- `getTickDuration()` - returns the configured tick duration; `Duration.ZERO` for a non-ticking clock.
- `withTickDuration()` - returns an independent copy of the clock that automatically advances by the given duration on
  each call to `instant()`.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/

[semver]: https://semver.org/spec/v2.0.0.html
