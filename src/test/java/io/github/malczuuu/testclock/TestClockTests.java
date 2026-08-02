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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class TestClockTests {

  private static final Instant BASE = Instant.parse("2024-06-15T12:00:00Z");

  @Test
  void givenInstant_whenFrom_thenUsesUtcZoneAndPreservesInstant() {
    TestClock clock = TestClock.from(BASE);

    assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    assertThat(clock.instant()).isEqualTo(BASE);
  }

  @Test
  void givenInstantAndZone_whenFrom_thenPreservesZoneAndInstant() {
    ZoneId zone = ZoneId.of("Europe/Warsaw");

    TestClock clock = TestClock.from(BASE, zone);

    assertThat(clock.getZone()).isEqualTo(zone);
    assertThat(clock.instant()).isEqualTo(BASE);
  }

  @Test
  void givenInstantText_whenFrom_thenParsesInstantAndUsesUtcZone() {
    TestClock clock = TestClock.from("2024-06-15T12:00:00Z");

    assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    assertThat(clock.instant()).isEqualTo(BASE);
  }

  @Test
  void givenInstantTextAndZone_whenFrom_thenParsesInstantAndPreservesZone() {
    ZoneId zone = ZoneId.of("Europe/Warsaw");

    TestClock clock = TestClock.from("2024-06-15T12:00:00Z", zone);

    assertThat(clock.getZone()).isEqualTo(zone);
    assertThat(clock.instant()).isEqualTo(BASE);
  }

  @Test
  void givenInvalidText_whenFrom_thenThrowsDateTimeParseException() {
    assertThatThrownBy(() -> TestClock.from("not-an-instant"))
        .isInstanceOf(DateTimeParseException.class);
  }

  @Test
  void givenClockWithNanos_whenFrom_thenPreservesSubSecondPrecision() {
    Instant withNanos = Instant.parse("2024-06-15T12:00:00.123456789Z");
    Clock source = Clock.fixed(withNanos, ZoneOffset.UTC);

    TestClock clock = TestClock.from(source);

    assertThat(clock.instant()).isEqualTo(withNanos);
  }

  @Test
  void givenClockWithZone_whenFrom_thenPreservesZone() {
    ZoneId zone = ZoneId.of("America/New_York");
    Clock source = Clock.system(zone);

    TestClock clock = TestClock.from(source);

    assertThat(clock.getZone()).isEqualTo(zone);
  }

  @Test
  void givenNothing_whenFromSystemUtc_thenUsesUtcZone() {
    assertThat(TestClock.fromSystemUTC().getZone()).isEqualTo(ZoneOffset.UTC);
  }

  @Test
  void givenZone_whenFromSystem_thenPreservesZone() {
    ZoneId zone = ZoneId.of("Asia/Tokyo");

    assertThat(TestClock.fromSystem(zone).getZone()).isEqualTo(zone);
  }

  @Test
  void givenClock_whenSetCurrentTime_thenReplacesCurrentInstant() {
    TestClock clock = TestClock.from(BASE);
    Instant newTime = BASE.plusSeconds(3600);

    clock.setCurrentTime(newTime);

    assertThat(clock.instant()).isEqualTo(newTime);
  }

  @Test
  void givenTickingClock_whenSetCurrentTime_thenAffectsNextInstant() {
    TestClock clock = TestClock.from(BASE).withTickDuration(Duration.ofSeconds(1));
    Instant newTime = BASE.plusSeconds(100);

    clock.setCurrentTime(newTime);

    assertThat(clock.instant()).isEqualTo(newTime);
    assertThat(clock.instant()).isEqualTo(newTime.plusSeconds(1));
  }

  @Test
  void givenClock_whenForwardDuration_thenAdvancesTimeByGivenDuration() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(Duration.ofMinutes(5));

    assertThat(result).isEqualTo(BASE.plusSeconds(300));
    assertThat(clock.instant()).isEqualTo(BASE.plusSeconds(300));
  }

  @Test
  void givenClock_whenForwardSeconds_thenAdvancesTimeByGivenSeconds() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forwardSeconds(10);

    assertThat(result).isEqualTo(BASE.plusSeconds(10));
  }

  @Test
  void givenClock_whenForwardMillis_thenAdvancesTimeByGivenMillis() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forwardMillis(500);

    assertThat(result).isEqualTo(BASE.plusMillis(500));
  }

  @Test
  void givenClock_whenForwardNanos_thenAdvancesTimeByGivenNanos() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forwardNanos(1_000_000L);

    assertThat(result).isEqualTo(BASE.plusNanos(1_000_000L));
  }

  @Test
  void givenClock_whenForwardAmountAndUnit_thenAdvancesTimeByGivenAmount() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(3, ChronoUnit.HOURS);

    assertThat(result).isEqualTo(BASE.plus(3, ChronoUnit.HOURS));
    assertThat(clock.instant()).isEqualTo(BASE.plus(3, ChronoUnit.HOURS));
  }

  @Test
  void givenClock_whenRewindDuration_thenMovesTimeBackByGivenDuration() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(Duration.ofMinutes(5));

    assertThat(result).isEqualTo(BASE.minusSeconds(300));
    assertThat(clock.instant()).isEqualTo(BASE.minusSeconds(300));
  }

  @Test
  void givenClock_whenRewindSeconds_thenMovesTimeBackByGivenSeconds() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewindSeconds(10);

    assertThat(result).isEqualTo(BASE.minusSeconds(10));
  }

  @Test
  void givenClock_whenRewindMillis_thenMovesTimeBackByGivenMillis() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewindMillis(500);

    assertThat(result).isEqualTo(BASE.minusMillis(500));
  }

  @Test
  void givenClock_whenRewindNanos_thenMovesTimeBackByGivenNanos() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewindNanos(1_000_000L);

    assertThat(result).isEqualTo(BASE.minusNanos(1_000_000L));
  }

  @Test
  void givenClock_whenRewindAmountAndUnit_thenMovesTimeBackByGivenAmount() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(3, ChronoUnit.HOURS);

    assertThat(result).isEqualTo(BASE.minus(3, ChronoUnit.HOURS));
    assertThat(clock.instant()).isEqualTo(BASE.minus(3, ChronoUnit.HOURS));
  }

  @Test
  void givenClock_whenForwardThenRewind_thenResultsInNetTimeChange() {
    TestClock clock = TestClock.from(BASE);

    clock.forwardSeconds(60);
    clock.rewindSeconds(20);

    assertThat(clock.instant()).isEqualTo(BASE.plusSeconds(40));
  }

  @Test
  void givenClock_whenForwardZeroSeconds_thenLeavesClockUnchanged() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forwardSeconds(0);

    assertThat(result).isEqualTo(BASE);
    assertThat(clock.instant()).isEqualTo(BASE);
  }

  @Test
  void givenClock_whenRewindZeroDuration_thenLeavesClockUnchanged() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(Duration.ZERO);

    assertThat(result).isEqualTo(BASE);
    assertThat(clock.instant()).isEqualTo(BASE);
  }

  @Test
  void givenNonTickingClock_whenInstantCalledRepeatedly_thenReturnsSameInstant() {
    TestClock clock = TestClock.from(BASE);

    assertThat(clock.instant()).isEqualTo(BASE);
    assertThat(clock.instant()).isEqualTo(BASE);
    assertThat(clock.instant()).isEqualTo(BASE);
  }

  @Test
  void givenTickingClock_whenInstantCalledRepeatedly_thenAdvancesTimeOnEachCall() {
    TestClock clock = TestClock.from(BASE).withTickDuration(Duration.ofSeconds(1));

    assertThat(clock.instant()).isEqualTo(BASE);
    assertThat(clock.instant()).isEqualTo(BASE.plusSeconds(1));
    assertThat(clock.instant()).isEqualTo(BASE.plusSeconds(2));
  }

  @Test
  void givenClock_whenWithTickDuration_thenOriginalClockIsUnaffected() {
    TestClock original = TestClock.from(BASE);
    TestClock ticking = original.withTickDuration(Duration.ofSeconds(5));

    ticking.instant();

    assertThat(original.instant()).isEqualTo(BASE);
  }

  @Test
  void givenClockAdvanced_whenWithTickDuration_thenCapturesCurrentTimeAtCallTime() {
    TestClock clock = TestClock.from(BASE);
    clock.forwardSeconds(30);

    TestClock ticking = clock.withTickDuration(Duration.ofSeconds(1));

    assertThat(ticking.instant()).isEqualTo(BASE.plusSeconds(30));
  }

  @Test
  void givenTickingClock_whenWithTickDurationZero_thenProducesNonTickingClock() {
    TestClock ticking = TestClock.from(BASE).withTickDuration(Duration.ofSeconds(5));

    TestClock stopped = ticking.withTickDuration(Duration.ZERO);

    assertThat(stopped.getTickDuration()).isEqualTo(Duration.ZERO);
    assertThat(stopped.instant()).isEqualTo(BASE);
    assertThat(stopped.instant()).isEqualTo(BASE);
  }

  @Test
  void givenTickingClock_whenGetCurrentTime_thenDoesNotAdvanceClock() {
    TestClock clock = TestClock.from(BASE).withTickDuration(Duration.ofSeconds(1));

    assertThat(clock.getCurrentTime()).isEqualTo(BASE);
    assertThat(clock.getCurrentTime()).isEqualTo(BASE);
  }

  @Test
  void givenClockForwarded_whenGetCurrentTime_thenReturnsLatestTime() {
    TestClock clock = TestClock.from(BASE);
    clock.forwardSeconds(15);

    assertThat(clock.getCurrentTime()).isEqualTo(BASE.plusSeconds(15));
  }

  @Test
  void givenNonTickingClock_whenGetTickDuration_thenReturnsZero() {
    assertThat(TestClock.from(BASE).getTickDuration()).isEqualTo(Duration.ZERO);
  }

  @Test
  void givenTickingClock_whenGetTickDuration_thenReturnsConfiguredDuration() {
    assertThat(TestClock.from(BASE).withTickDuration(Duration.ofSeconds(5)).getTickDuration())
        .isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  void givenClock_whenWithZone_thenReturnsClockWithNewZone() {
    TestClock clock = TestClock.from(BASE);
    ZoneId zone = ZoneId.of("Europe/Paris");

    assertThat(clock.withZone(zone).getZone()).isEqualTo(zone);
  }

  @Test
  void givenClockForwarded_whenWithZone_thenPreservesCurrentInstant() {
    TestClock clock = TestClock.from(BASE);
    clock.forwardSeconds(60);

    assertThat(clock.withZone(ZoneId.of("America/Chicago")).instant())
        .isEqualTo(BASE.plusSeconds(60));
  }

  @Test
  void givenClock_whenWithZone_thenOriginalClockZoneIsUnaffected() {
    TestClock clock = TestClock.from(BASE);

    clock.withZone(ZoneId.of("Asia/Seoul"));

    assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
  }

  @Test
  void givenClockClonedViaWithZone_whenOriginalForwarded_thenCloneIsUnaffected() {
    TestClock original = TestClock.from(BASE);
    TestClock clone = original.withZone(ZoneId.of("Europe/Warsaw"));

    original.forwardSeconds(60);

    assertThat(clone.instant()).isEqualTo(BASE);
  }

  @Test
  void givenTickingClock_whenWithZone_thenPreservesTickDuration() {
    Duration tick = Duration.ofSeconds(3);
    TestClock clock = TestClock.from(BASE).withTickDuration(tick);

    TestClock zoned = clock.withZone(ZoneId.of("Europe/Warsaw"));

    assertThat(zoned.getTickDuration()).isEqualTo(tick);
    assertThat(zoned.instant()).isEqualTo(BASE);
    assertThat(zoned.instant()).isEqualTo(BASE.plusSeconds(3));
  }

  @Test
  void givenNonTickingClock_whenToString_thenOmitsTickSegment() {
    assertThat(TestClock.from(BASE).toString()).isEqualTo("TestClock[2024-06-15T12:00:00Z,Z]");
  }

  @Test
  void givenTickingClock_whenToString_thenIncludesTickSegment() {
    assertThat(TestClock.from(BASE).withTickDuration(Duration.ofSeconds(5)).toString())
        .isEqualTo("TestClock[2024-06-15T12:00:00Z,Z,tick=PT5S]");
  }
}
