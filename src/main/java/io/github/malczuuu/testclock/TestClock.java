/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.malczuuu.testclock;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

/**
 * A mutable &amp; playable {@link Clock} implementation designed for use in tests. It allows full control over the
 * current time, including setting an arbitrary instant, advancing or rewinding time, and configuring an automatic tick
 * duration applied on each call to {@link #instant()}.
 *
 * <p>By default, the clock is non-ticking - {@link #instant()} returns the same value until the time is explicitly
 * changed. An automatic tick can be enabled via {@link #withTickDuration(Duration)}, which causes each call to {@link
 * #instant()} to advance the clock by the configured amount.
 *
 * <p>All time-mutation methods ({@link #setCurrentTime}, {@link #forward}, {@link #rewind}, etc.) are thread-safe.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TestClock clock = TestClock.from("2024-01-01T00:00:00Z");
 * clock.forwardSeconds(30);
 * Instant now = clock.instant(); // 2024-01-01T00:00:30Z
 * }</pre>
 *
 * @since 1.0.0
 */
public final class TestClock extends Clock implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a {@code TestClock} initialized from the current UTC system time.
   *
   * @return a new {@code TestClock} set to the current UTC instant
   * @since 1.0.0
   */
  public static TestClock fromSystemUTC() {
    return from(Clock.systemUTC());
  }

  /**
   * Creates a {@code TestClock} initialized from the current system time in the given time zone.
   *
   * @param zone the time zone to use; not null
   * @return a new {@code TestClock} set to the current instant in the specified zone
   * @since 1.0.0
   */
  public static TestClock fromSystem(ZoneId zone) {
    return from(Clock.system(zone));
  }

  /**
   * Creates a {@code TestClock} initialized from an existing {@link Clock}. The starting instant and zone are taken
   * from the source clock.
   *
   * @param clock the clock to snapshot; not null
   * @return a new {@code TestClock} with the instant and zone of the given clock
   * @since 1.0.0
   */
  public static TestClock from(Clock clock) {
    return new TestClock(clock);
  }

  /**
   * Creates a non-ticking {@code TestClock} fixed at the given instant in {@link ZoneOffset#UTC}.
   *
   * @param instant the starting instant; not null
   * @return a new {@code TestClock} set to the given instant in UTC
   * @since 1.0.0
   */
  public static TestClock from(Instant instant) {
    return new TestClock(instant, ZoneOffset.UTC, Duration.ZERO);
  }

  /**
   * Creates a non-ticking {@code TestClock} fixed at the parsed instant in {@link ZoneOffset#UTC}. The text is parsed
   * using {@link Instant#parse(CharSequence)}.
   *
   * @param instantAsText the instant string to parse (e.g. {@code "2024-01-01T00:00:00Z"}); not null
   * @return a new {@code TestClock} set to the parsed instant in UTC
   * @throws java.time.format.DateTimeParseException if the text cannot be parsed
   * @since 1.0.0
   */
  public static TestClock from(String instantAsText) {
    return from(Instant.parse(instantAsText));
  }

  /**
   * Creates a non-ticking {@code TestClock} fixed at the parsed instant in the given time zone. The text is parsed
   * using {@link Instant#parse(CharSequence)}.
   *
   * @param instantAsText the instant string to parse (e.g. {@code "2024-01-01T00:00:00Z"}); not null
   * @param zone the time zone; not null
   * @return a new {@code TestClock} set to the parsed instant and zone
   * @throws java.time.format.DateTimeParseException if the text cannot be parsed
   * @since 1.0.0
   */
  public static TestClock from(String instantAsText, ZoneId zone) {
    return from(Instant.parse(instantAsText), zone);
  }

  /**
   * Creates a non-ticking {@code TestClock} fixed at the given instant in the given time zone.
   *
   * @param instant the starting instant; not null
   * @param zone the time zone; not null
   * @return a new {@code TestClock} set to the given instant and zone
   * @since 1.0.0
   */
  public static TestClock from(Instant instant, ZoneId zone) {
    return new TestClock(instant, zone, Duration.ZERO);
  }

  /** The current time of this clock, mutated atomically by all time-advancing operations. */
  private final AtomicReference<Instant> currentTime;

  /** The time zone of this clock, fixed at construction time. */
  private final ZoneId zone;

  /**
   * The amount by which {@link #instant()} advances the clock on each call. {@link Duration#ZERO} means non-ticking.
   */
  private final Duration tickDuration;

  /**
   * Constructs a {@code TestClock} by snapshotting the instant and zone of the given clock.
   *
   * @param clock the clock to snapshot; not null
   * @since 1.0.0
   */
  private TestClock(Clock clock) {
    this(clock.instant(), clock.getZone(), Duration.ZERO);
  }

  /**
   * Constructs a {@code TestClock} with explicit initial state.
   *
   * @param currentTime the starting instant; not null
   * @param zone the time zone; not null
   * @param tickDuration the tick duration; not null
   * @since 1.0.0
   */
  private TestClock(Instant currentTime, ZoneId zone, Duration tickDuration) {
    this.currentTime = new AtomicReference<>(currentTime);
    this.zone = zone;
    this.tickDuration = tickDuration;
  }

  /**
   * Returns a new, independent {@code TestClock} with the given tick duration. The new clock captures the current time
   * of this clock at the moment of the call; subsequent mutations to either clock do not affect the other.
   *
   * <p>Each call to {@link #instant()} on the returned clock will advance its time by {@code tickDuration}.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:00Z");
   * TestClock ticking = clock.withTickDuration(Duration.ofSeconds(1));
   * Instant first = ticking.instant();  // 2024-01-01T00:00:00Z
   * Instant second = ticking.instant(); // 2024-01-01T00:00:01Z
   * }</pre>
   *
   * @param tickDuration the amount to advance on each {@link #instant()} call; use {@link Duration#ZERO} for a
   *     non-ticking clock
   * @return a new {@code TestClock} with the current time and zone of this clock but the new tick duration
   * @since 1.0.0
   */
  public TestClock withTickDuration(Duration tickDuration) {
    return new TestClock(currentTime.get(), zone, tickDuration);
  }

  /**
   * Returns the current instant without advancing the clock, regardless of any configured tick duration. This is useful
   * for inspecting the current time in tests without side effects.
   *
   * @return the current instant of this clock
   * @since 1.0.0
   */
  public Instant getCurrentTime() {
    return currentTime.get();
  }

  /**
   * Returns the tick duration configured for this clock. {@link Duration#ZERO} means non-ticking.
   *
   * @return the tick duration of this clock
   * @since 1.0.0
   */
  public Duration getTickDuration() {
    return tickDuration;
  }

  /**
   * Sets the current time of this clock to the given instant. This replaces the current value unconditionally.
   *
   * @param currentTime the new current instant; not null
   * @since 1.0.0
   */
  public void setCurrentTime(Instant currentTime) {
    this.currentTime.set(currentTime);
  }

  /**
   * Sets the current time of this clock to the given local date-time, interpreted in the zone of this clock. This
   * replaces the current value unconditionally.
   *
   * <p>A local date-time affected by a DST transition is resolved as in {@link LocalDateTime#atZone(ZoneId)} - a time
   * skipped by the transition moves later by the length of the gap, and a time that occurs twice uses the earlier
   * offset.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:00Z", ZoneId.of("Europe/Warsaw"));
   * clock.setCurrentTime(LocalDateTime.parse("2024-06-15T12:00:00"));
   * // clock.getCurrentTime() == 2024-06-15T10:00:00Z
   * // LocalDateTime.now(clock) == 2024-06-15T12:00
   * }</pre>
   *
   * @param currentTime the new current local date-time; not null
   * @since 1.1.0
   */
  public void setCurrentTime(LocalDateTime currentTime) {
    this.currentTime.set(currentTime.atZone(zone).toInstant());
  }

  /**
   * Advances the current time by the given amount and returns the new instant.
   *
   * <p>The amount is applied to the current time viewed in the zone of this clock (see {@link
   * ZonedDateTime#plus(TemporalAmount)}), so both time-based amounts (e.g. {@link Duration}) and date-based amounts
   * (e.g. {@link Period} of days, weeks, months or years) are supported. Date-based amounts follow calendar rules of
   * the zone - adding a month to January 31 yields the last day of February, and adding a day across a DST transition
   * preserves the local time of day.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-31T00:00:00Z");
   * Instant updated = clock.forward(Duration.ofMinutes(5));
   * // updated == 2024-01-31T00:05:00Z
   * updated = clock.forward(Period.ofMonths(1));
   * // updated == 2024-02-29T00:05:00Z
   * }</pre>
   *
   * @param amountToAdd the amount of time to add; not null
   * @return the updated instant after advancing
   * @throws java.time.DateTimeException if the amount cannot be added
   * @throws java.time.temporal.UnsupportedTemporalTypeException if the amount contains an unsupported unit
   * @throws ArithmeticException if numeric overflow occurs
   * @since 1.0.0
   */
  public Instant forward(TemporalAmount amountToAdd) {
    return currentTime.updateAndGet(it -> it.atZone(zone).plus(amountToAdd).toInstant());
  }

  /**
   * Advances the current time by the given amount of the given unit and returns the new instant.
   *
   * <p>The amount is applied to the current time viewed in the zone of this clock (see {@link ZonedDateTime#plus(long,
   * TemporalUnit)}), so both time-based units (e.g. {@link java.time.temporal.ChronoUnit#HOURS}) and date-based units
   * (e.g. {@link java.time.temporal.ChronoUnit#MONTHS}) are supported.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:00Z");
   * Instant updated = clock.forward(3, ChronoUnit.HOURS);
   * // updated == 2024-01-01T03:00:00Z
   * updated = clock.forward(2, ChronoUnit.MONTHS);
   * // updated == 2024-03-01T03:00:00Z
   * }</pre>
   *
   * @param amountToAdd the amount of the unit to add
   * @param unit the unit of the amount; not null
   * @return the updated instant after advancing
   * @throws java.time.DateTimeException if the amount cannot be added
   * @throws java.time.temporal.UnsupportedTemporalTypeException if the unit is not supported
   * @throws ArithmeticException if numeric overflow occurs
   * @since 1.0.0
   */
  public Instant forward(long amountToAdd, TemporalUnit unit) {
    return currentTime.updateAndGet(it -> it.atZone(zone).plus(amountToAdd, unit).toInstant());
  }

  /**
   * Advances the current time by the given number of seconds and returns the new instant.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:00Z");
   * Instant updated = clock.forwardSeconds(30);
   * // updated == 2024-01-01T00:00:30Z
   * }</pre>
   *
   * @param secondsToAdd the number of seconds to add
   * @return the updated instant after advancing
   * @since 1.0.0
   */
  public Instant forwardSeconds(long secondsToAdd) {
    return currentTime.updateAndGet(it -> it.plusSeconds(secondsToAdd));
  }

  /**
   * Advances the current time by the given number of milliseconds and returns the new instant.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:00Z");
   * Instant updated = clock.forwardMillis(500);
   * // updated == 2024-01-01T00:00:00.500Z
   * }</pre>
   *
   * @param millisToAdd the number of milliseconds to add
   * @return the updated instant after advancing
   * @since 1.0.0
   */
  public Instant forwardMillis(long millisToAdd) {
    return currentTime.updateAndGet(it -> it.plusMillis(millisToAdd));
  }

  /**
   * Advances the current time by the given number of nanoseconds and returns the new instant.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:00Z");
   * Instant updated = clock.forwardNanos(1_000_000);
   * // updated == 2024-01-01T00:00:00.001Z
   * }</pre>
   *
   * @param nanosToAdd the number of nanoseconds to add
   * @return the updated instant after advancing
   * @since 1.0.0
   */
  public Instant forwardNanos(long nanosToAdd) {
    return currentTime.updateAndGet(it -> it.plusNanos(nanosToAdd));
  }

  /**
   * Moves the current time back by the given amount and returns the new instant.
   *
   * <p>The amount is applied to the current time viewed in the zone of this clock (see {@link
   * ZonedDateTime#minus(TemporalAmount)}), so both time-based amounts (e.g. {@link Duration}) and date-based amounts
   * (e.g. {@link Period} of days, weeks, months or years) are supported. Date-based amounts follow calendar rules of
   * the zone - subtracting a month from March 31 yields the last day of February, and subtracting a day across a DST
   * transition preserves the local time of day.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-03-31T01:00:00Z");
   * Instant updated = clock.rewind(Duration.ofMinutes(30));
   * // updated == 2024-03-31T00:30:00Z
   * updated = clock.rewind(Period.ofMonths(1));
   * // updated == 2024-02-29T00:30:00Z
   * }</pre>
   *
   * @param amountToSubtract the amount of time to subtract; not null
   * @return the updated instant after rewinding
   * @throws java.time.DateTimeException if the amount cannot be subtracted
   * @throws java.time.temporal.UnsupportedTemporalTypeException if the amount contains an unsupported unit
   * @throws ArithmeticException if numeric overflow occurs
   * @since 1.0.0
   */
  public Instant rewind(TemporalAmount amountToSubtract) {
    return currentTime.updateAndGet(it -> it.atZone(zone).minus(amountToSubtract).toInstant());
  }

  /**
   * Moves the current time back by the given amount of the given unit and returns the new instant.
   *
   * <p>The amount is applied to the current time viewed in the zone of this clock (see {@link ZonedDateTime#minus(long,
   * TemporalUnit)}), so both time-based units (e.g. {@link java.time.temporal.ChronoUnit#HOURS}) and date-based units
   * (e.g. {@link java.time.temporal.ChronoUnit#MONTHS}) are supported.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-03-01T06:00:00Z");
   * Instant updated = clock.rewind(2, ChronoUnit.HOURS);
   * // updated == 2024-03-01T04:00:00Z
   * updated = clock.rewind(2, ChronoUnit.MONTHS);
   * // updated == 2024-01-01T04:00:00Z
   * }</pre>
   *
   * @param amountToSubtract the amount of the unit to subtract
   * @param unit the unit of the amount; not null
   * @return the updated instant after rewinding
   * @throws java.time.DateTimeException if the amount cannot be subtracted
   * @throws java.time.temporal.UnsupportedTemporalTypeException if the unit is not supported
   * @throws ArithmeticException if numeric overflow occurs
   * @since 1.0.0
   */
  public Instant rewind(long amountToSubtract, TemporalUnit unit) {
    return currentTime.updateAndGet(it -> it.atZone(zone).minus(amountToSubtract, unit).toInstant());
  }

  /**
   * Moves the current time back by the given number of seconds and returns the new instant.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:01:00Z");
   * Instant updated = clock.rewindSeconds(30);
   * // updated == 2024-01-01T00:00:30Z
   * }</pre>
   *
   * @param secondsToSubtract the number of seconds to subtract
   * @return the updated instant after rewinding
   * @since 1.0.0
   */
  public Instant rewindSeconds(long secondsToSubtract) {
    return currentTime.updateAndGet(it -> it.minusSeconds(secondsToSubtract));
  }

  /**
   * Moves the current time back by the given number of milliseconds and returns the new instant.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:01Z");
   * Instant updated = clock.rewindMillis(500);
   * // updated == 2024-01-01T00:00:00.500Z
   * }</pre>
   *
   * @param millisToSubtract the number of milliseconds to subtract
   * @return the updated instant after rewinding
   * @since 1.0.0
   */
  public Instant rewindMillis(long millisToSubtract) {
    return currentTime.updateAndGet(it -> it.minusMillis(millisToSubtract));
  }

  /**
   * Moves the current time back by the given number of nanoseconds and returns the new instant.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:00.001Z");
   * Instant updated = clock.rewindNanos(1_000_000);
   * // updated == 2024-01-01T00:00:00Z
   * }</pre>
   *
   * @param nanosToSubtract the number of nanoseconds to subtract
   * @return the updated instant after rewinding
   * @since 1.0.0
   */
  public Instant rewindNanos(long nanosToSubtract) {
    return currentTime.updateAndGet(it -> it.minusNanos(nanosToSubtract));
  }

  /**
   * Gets the time-zone being used to create dates and times.
   *
   * @return the time-zone being used to interpret instants, not null
   * @since 1.0.0
   */
  @Override
  public ZoneId getZone() {
    return zone;
  }

  /**
   * Returns a copy of this clock with a different time-zone.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:00Z");
   * TestClock warsaw = clock.withZone(ZoneId.of("Europe/Warsaw"));
   * // warsaw.getZone() == ZoneId.of("Europe/Warsaw")
   * }</pre>
   *
   * @param zone the time-zone to change to, not null
   * @return a new, independent {@code TestClock} with the given zone. The new clock captures the current time of this
   *     clock at the moment of the call; subsequent mutations to either clock do not affect the other.
   * @since 1.0.0
   */
  @Override
  public TestClock withZone(ZoneId zone) {
    return new TestClock(getCurrentTime(), zone, tickDuration);
  }

  /**
   * Returns the current instant and, if a tick duration is configured, advances the clock by that amount for the next
   * call.
   *
   * <p>If the tick duration is {@link Duration#ZERO} (the default), successive calls return the same instant until the
   * time is changed explicitly.
   *
   * <pre>{@code
   * TestClock clock = TestClock.from("2024-01-01T00:00:00Z");
   * Instant t1 = clock.instant(); // 2024-01-01T00:00:00Z
   * Instant t2 = clock.instant(); // 2024-01-01T00:00:00Z (unchanged)
   * clock.forwardSeconds(10);
   * Instant t3 = clock.instant(); // 2024-01-01T00:00:10Z
   * }</pre>
   *
   * @return the current instant of this clock
   * @since 1.0.0
   */
  @Override
  public Instant instant() {
    return currentTime.getAndUpdate(it -> it.plus(tickDuration));
  }

  /**
   * Compares this clock to another object. Two clocks are equal if they have the same current time, zone and tick
   * duration. Comparing does not advance either clock.
   *
   * <p>The result reflects the state at the moment of the call, so equal clocks stop being equal once the time of only
   * one of them changes.
   *
   * <pre>{@code
   * TestClock first = TestClock.from("2024-01-01T00:00:00Z");
   * TestClock second = TestClock.from("2024-01-01T00:00:00Z");
   * first.equals(second); // true
   * first.forwardSeconds(1);
   * first.equals(second); // false
   * }</pre>
   *
   * @param obj the object to compare with, may be null
   * @return {@code true} if the given object is a {@code TestClock} with the same state
   * @since 1.1.0
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TestClock)) {
      return false;
    }
    TestClock other = (TestClock) obj;
    return currentTime.get().equals(other.currentTime.get())
        && zone.equals(other.zone)
        && tickDuration.equals(other.tickDuration);
  }

  /**
   * Returns a hash code based on the current time, zone and tick duration. Calling this method does not advance the
   * clock.
   *
   * <p>The hash code changes whenever the current time changes, so a {@code TestClock} should not be used as a key in a
   * hash-based collection while its time is being changed.
   *
   * @return the hash code of the current state of this clock
   * @since 1.1.0
   */
  @Override
  public int hashCode() {
    return Objects.hash(currentTime.get(), zone, tickDuration);
  }

  /**
   * Returns a string representation of this clock, formatted {@code TestClock[<instant>,<zone>]}, or {@code
   * TestClock[<instant>,<zone>,tick=<duration>]} when a non-zero tick duration is configured.
   *
   * <p>Its primary purpose is debugging convenience in an IDE.
   *
   * <pre>{@code
   * TestClock.from("2024-01-01T00:00:00Z").toString();
   * // "TestClock[2024-01-01T00:00:00Z,UTC]"
   *
   * TestClock.from("2024-01-01T00:00:00Z").withTickDuration(Duration.ofSeconds(1)).toString();
   * // "TestClock[2024-01-01T00:00:00Z,UTC,tick=PT1S]"
   * }</pre>
   *
   * @return a string representation of this clock
   * @since 1.0.0
   */
  @Override
  public String toString() {
    String base = "TestClock[" + currentTime.get() + "," + zone;
    if (tickDuration.isZero()) {
      return base + "]";
    }
    return base + ",tick=" + tickDuration + "]";
  }
}
