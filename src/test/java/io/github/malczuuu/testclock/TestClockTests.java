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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import org.junit.jupiter.api.Test;

class TestClockTests {

  private static final Instant BASE = Instant.parse("2024-06-15T12:00:00Z");

  // In 2024, Warsaw skips 02:00-03:00 on March 31 and repeats 02:00-03:00 on October 27.
  private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

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
  void givenClock_whenForwardPeriodOfDays_thenAdvancesTimeByGivenDays() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(Period.ofDays(3));

    assertThat(result).isEqualTo(Instant.parse("2024-06-18T12:00:00Z"));
    assertThat(clock.instant()).isEqualTo(Instant.parse("2024-06-18T12:00:00Z"));
  }

  @Test
  void givenClock_whenForwardPeriodOfWeeks_thenAdvancesTimeByGivenWeeks() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(Period.ofWeeks(2));

    assertThat(result).isEqualTo(Instant.parse("2024-06-29T12:00:00Z"));
  }

  @Test
  void givenClock_whenForwardPeriodOfMonths_thenAdvancesTimeByGivenMonths() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(Period.ofMonths(1));

    assertThat(result).isEqualTo(Instant.parse("2024-07-15T12:00:00Z"));
    assertThat(clock.instant()).isEqualTo(Instant.parse("2024-07-15T12:00:00Z"));
  }

  @Test
  void givenClock_whenForwardPeriodOfYears_thenAdvancesTimeByGivenYears() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(Period.ofYears(2));

    assertThat(result).isEqualTo(Instant.parse("2026-06-15T12:00:00Z"));
  }

  @Test
  void givenClock_whenForwardMixedPeriod_thenAdvancesTimeByAllComponents() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(Period.of(1, 2, 3));

    assertThat(result).isEqualTo(Instant.parse("2025-08-18T12:00:00Z"));
  }

  @Test
  void givenClock_whenForwardNegativePeriod_thenMovesTimeBack() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(Period.ofMonths(-1));

    assertThat(result).isEqualTo(Instant.parse("2024-05-15T12:00:00Z"));
  }

  @Test
  void givenEndOfMonth_whenForwardPeriodOfMonths_thenClampsToLastDayOfShorterMonth() {
    TestClock clock = TestClock.from("2024-01-31T12:00:00Z");

    Instant result = clock.forward(Period.ofMonths(1));

    assertThat(result).isEqualTo(Instant.parse("2024-02-29T12:00:00Z"));
  }

  @Test
  void givenLeapDay_whenForwardPeriodOfYears_thenClampsToLastDayOfFebruary() {
    TestClock clock = TestClock.from("2024-02-29T12:00:00Z");

    Instant result = clock.forward(Period.ofYears(1));

    assertThat(result).isEqualTo(Instant.parse("2025-02-28T12:00:00Z"));
  }

  @Test
  void givenClockInZone_whenForwardPeriodOfMonths_thenUsesCalendarOfClockZone() {
    TestClock utc = TestClock.from("2024-02-29T20:00:00Z");
    TestClock tokyo = TestClock.from("2024-02-29T20:00:00Z", ZoneId.of("Asia/Tokyo"));

    Instant utcResult = utc.forward(Period.ofMonths(1));
    Instant tokyoResult = tokyo.forward(Period.ofMonths(1));

    assertThat(utcResult).isEqualTo(Instant.parse("2024-03-29T20:00:00Z"));
    assertThat(tokyoResult).isEqualTo(Instant.parse("2024-03-31T20:00:00Z"));
  }

  @Test
  void givenClockBeforeDstStart_whenForwardPeriodOfDays_thenPreservesLocalTime() {
    TestClock clock = TestClock.from("2024-03-30T12:00:00Z", ZoneId.of("Europe/Warsaw"));

    Instant result = clock.forward(Period.ofDays(1));

    assertThat(result).isEqualTo(Instant.parse("2024-03-31T11:00:00Z"));
  }

  @Test
  void givenClockBeforeDstStart_whenForwardDurationOfDays_thenAdvancesExactHours() {
    TestClock clock = TestClock.from("2024-03-30T12:00:00Z", ZoneId.of("Europe/Warsaw"));

    Instant result = clock.forward(Duration.ofDays(1));

    assertThat(result).isEqualTo(Instant.parse("2024-03-31T12:00:00Z"));
  }

  @Test
  void givenClock_whenForwardAmountOfMonths_thenAdvancesTimeByGivenMonths() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(2, ChronoUnit.MONTHS);

    assertThat(result).isEqualTo(Instant.parse("2024-08-15T12:00:00Z"));
    assertThat(clock.instant()).isEqualTo(Instant.parse("2024-08-15T12:00:00Z"));
  }

  @Test
  void givenClock_whenForwardAmountOfWeeks_thenAdvancesTimeByGivenWeeks() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(1, ChronoUnit.WEEKS);

    assertThat(result).isEqualTo(Instant.parse("2024-06-22T12:00:00Z"));
  }

  @Test
  void givenClock_whenForwardAmountOfYears_thenAdvancesTimeByGivenYears() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.forward(1, ChronoUnit.YEARS);

    assertThat(result).isEqualTo(Instant.parse("2025-06-15T12:00:00Z"));
  }

  @Test
  void givenClock_whenForwardUnsupportedUnit_thenThrowsAndLeavesClockUnchanged() {
    TestClock clock = TestClock.from(BASE);

    assertThatThrownBy(() -> clock.forward(1, ChronoUnit.FOREVER))
        .isInstanceOf(UnsupportedTemporalTypeException.class);
    assertThat(clock.getCurrentTime()).isEqualTo(BASE);
  }

  @Test
  void givenClock_whenForwardNonIsoPeriod_thenThrowsAndLeavesClockUnchanged() {
    TestClock clock = TestClock.from(BASE);
    ChronoPeriod japanesePeriod = JapaneseDate.of(2024, 1, 1).until(JapaneseDate.of(2024, 2, 1));

    assertThatThrownBy(() -> clock.forward(japanesePeriod)).isInstanceOf(DateTimeException.class);
    assertThat(clock.getCurrentTime()).isEqualTo(BASE);
  }

  @Test
  void givenClock_whenRewindPeriodOfDays_thenMovesTimeBackByGivenDays() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(Period.ofDays(3));

    assertThat(result).isEqualTo(Instant.parse("2024-06-12T12:00:00Z"));
    assertThat(clock.instant()).isEqualTo(Instant.parse("2024-06-12T12:00:00Z"));
  }

  @Test
  void givenClock_whenRewindPeriodOfWeeks_thenMovesTimeBackByGivenWeeks() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(Period.ofWeeks(2));

    assertThat(result).isEqualTo(Instant.parse("2024-06-01T12:00:00Z"));
  }

  @Test
  void givenClock_whenRewindPeriodOfMonths_thenMovesTimeBackByGivenMonths() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(Period.ofMonths(1));

    assertThat(result).isEqualTo(Instant.parse("2024-05-15T12:00:00Z"));
    assertThat(clock.instant()).isEqualTo(Instant.parse("2024-05-15T12:00:00Z"));
  }

  @Test
  void givenClock_whenRewindPeriodOfYears_thenMovesTimeBackByGivenYears() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(Period.ofYears(2));

    assertThat(result).isEqualTo(Instant.parse("2022-06-15T12:00:00Z"));
  }

  @Test
  void givenClock_whenRewindMixedPeriod_thenMovesTimeBackByAllComponents() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(Period.of(1, 2, 3));

    assertThat(result).isEqualTo(Instant.parse("2023-04-12T12:00:00Z"));
  }

  @Test
  void givenEndOfMonth_whenRewindPeriodOfMonths_thenClampsToLastDayOfShorterMonth() {
    TestClock clock = TestClock.from("2024-03-31T12:00:00Z");

    Instant result = clock.rewind(Period.ofMonths(1));

    assertThat(result).isEqualTo(Instant.parse("2024-02-29T12:00:00Z"));
  }

  @Test
  void givenClockAfterDstStart_whenRewindPeriodOfDays_thenPreservesLocalTime() {
    TestClock clock = TestClock.from("2024-03-31T11:00:00Z", ZoneId.of("Europe/Warsaw"));

    Instant result = clock.rewind(Period.ofDays(1));

    assertThat(result).isEqualTo(Instant.parse("2024-03-30T12:00:00Z"));
  }

  @Test
  void givenClock_whenRewindAmountOfMonths_thenMovesTimeBackByGivenMonths() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(2, ChronoUnit.MONTHS);

    assertThat(result).isEqualTo(Instant.parse("2024-04-15T12:00:00Z"));
    assertThat(clock.instant()).isEqualTo(Instant.parse("2024-04-15T12:00:00Z"));
  }

  @Test
  void givenClock_whenRewindAmountOfYears_thenMovesTimeBackByGivenYears() {
    TestClock clock = TestClock.from(BASE);

    Instant result = clock.rewind(1, ChronoUnit.YEARS);

    assertThat(result).isEqualTo(Instant.parse("2023-06-15T12:00:00Z"));
  }

  @Test
  void givenClock_whenRewindUnsupportedUnit_thenThrowsAndLeavesClockUnchanged() {
    TestClock clock = TestClock.from(BASE);

    assertThatThrownBy(() -> clock.rewind(1, ChronoUnit.FOREVER))
        .isInstanceOf(UnsupportedTemporalTypeException.class);
    assertThat(clock.getCurrentTime()).isEqualTo(BASE);
  }

  @Test
  void givenClock_whenRewindNonIsoPeriod_thenThrowsAndLeavesClockUnchanged() {
    TestClock clock = TestClock.from(BASE);
    ChronoPeriod japanesePeriod = JapaneseDate.of(2024, 1, 1).until(JapaneseDate.of(2024, 2, 1));

    assertThatThrownBy(() -> clock.rewind(japanesePeriod)).isInstanceOf(DateTimeException.class);
    assertThat(clock.getCurrentTime()).isEqualTo(BASE);
  }

  @Test
  void givenClockBeforeDstStart_whenForwardAmountOfDays_thenPreservesLocalTime() {
    TestClock clock = TestClock.from(at("2024-03-30T12:00:00+01:00"), WARSAW);

    Instant result = clock.forward(1, ChronoUnit.DAYS);

    assertThat(result).isEqualTo(at("2024-03-31T12:00:00+02:00"));
    assertThat(clock.instant()).isEqualTo(at("2024-03-31T12:00:00+02:00"));
  }

  @Test
  void givenClockBeforeDstStart_whenForwardAmountOfHours_thenAdvancesExactHours() {
    TestClock clock = TestClock.from(at("2024-03-30T12:00:00+01:00"), WARSAW);

    Instant result = clock.forward(24, ChronoUnit.HOURS);

    assertThat(result).isEqualTo(at("2024-03-31T13:00:00+02:00"));
  }

  @Test
  void givenClockBeforeDstStart_whenForwardPeriodOfMonths_thenPreservesLocalTime() {
    TestClock clock = TestClock.from(at("2024-03-15T12:00:00+01:00"), WARSAW);

    Instant result = clock.forward(Period.ofMonths(1));

    assertThat(result).isEqualTo(at("2024-04-15T12:00:00+02:00"));
  }

  @Test
  void givenClockAfterDstStart_whenRewindDurationOfDays_thenMovesBackExactHours() {
    TestClock clock = TestClock.from(at("2024-03-31T12:00:00+02:00"), WARSAW);

    Instant result = clock.rewind(Duration.ofDays(1));

    assertThat(result).isEqualTo(at("2024-03-30T11:00:00+01:00"));
  }

  @Test
  void givenClockAfterDstStart_whenRewindAmountOfDays_thenPreservesLocalTime() {
    TestClock clock = TestClock.from(at("2024-03-31T12:00:00+02:00"), WARSAW);

    Instant result = clock.rewind(1, ChronoUnit.DAYS);

    assertThat(result).isEqualTo(at("2024-03-30T12:00:00+01:00"));
  }

  @Test
  void givenClockAfterDstStart_whenRewindPeriodOfMonths_thenPreservesLocalTime() {
    TestClock clock = TestClock.from(at("2024-04-15T12:00:00+02:00"), WARSAW);

    Instant result = clock.rewind(Period.ofMonths(1));

    assertThat(result).isEqualTo(at("2024-03-15T12:00:00+01:00"));
  }

  @Test
  void givenLocalTimeInDstGap_whenForwardPeriodOfDays_thenShiftsLaterByGapLength() {
    TestClock clock = TestClock.from(at("2024-03-30T02:30:00+01:00"), WARSAW);

    Instant result = clock.forward(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-03-31T03:30:00+02:00"));
  }

  @Test
  void givenLocalTimeInDstGap_whenRewindPeriodOfDays_thenShiftsLaterByGapLength() {
    TestClock clock = TestClock.from(at("2024-04-01T02:30:00+02:00"), WARSAW);

    Instant result = clock.rewind(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-03-31T03:30:00+02:00"));
  }

  @Test
  void givenLocalTimeInHalfHourDstGap_whenForwardPeriodOfDays_thenShiftsLaterByHalfHour() {
    TestClock clock =
        TestClock.from(at("2024-10-05T02:15:00+10:30"), ZoneId.of("Australia/Lord_Howe"));

    Instant result = clock.forward(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-10-06T02:45:00+11:00"));
  }

  @Test
  void givenClockBeforeDstGap_whenForwardDuration_thenSkipsMissingLocalHour() {
    TestClock clock = TestClock.from(at("2024-03-31T01:30:00+01:00"), WARSAW);

    Instant result = clock.forward(Duration.ofHours(1));

    assertThat(result).isEqualTo(at("2024-03-31T03:30:00+02:00"));
  }

  @Test
  void givenClockBeforeDstEnd_whenForwardPeriodOfDays_thenPreservesLocalTime() {
    TestClock clock = TestClock.from(at("2024-10-26T12:00:00+02:00"), WARSAW);

    Instant result = clock.forward(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-10-27T12:00:00+01:00"));
    assertThat(clock.instant()).isEqualTo(at("2024-10-27T12:00:00+01:00"));
  }

  @Test
  void givenClockBeforeDstEnd_whenForwardAmountOfDays_thenPreservesLocalTime() {
    TestClock clock = TestClock.from(at("2024-10-26T12:00:00+02:00"), WARSAW);

    Instant result = clock.forward(1, ChronoUnit.DAYS);

    assertThat(result).isEqualTo(at("2024-10-27T12:00:00+01:00"));
  }

  @Test
  void givenClockBeforeDstEnd_whenForwardDurationOfDays_thenAdvancesExactHours() {
    TestClock clock = TestClock.from(at("2024-10-26T12:00:00+02:00"), WARSAW);

    Instant result = clock.forward(Duration.ofDays(1));

    assertThat(result).isEqualTo(at("2024-10-27T11:00:00+01:00"));
  }

  @Test
  void givenClockBeforeDstEnd_whenForwardAmountOfHours_thenAdvancesExactHours() {
    TestClock clock = TestClock.from(at("2024-10-26T12:00:00+02:00"), WARSAW);

    Instant result = clock.forward(24, ChronoUnit.HOURS);

    assertThat(result).isEqualTo(at("2024-10-27T11:00:00+01:00"));
  }

  @Test
  void givenClockAfterDstEnd_whenRewindPeriodOfDays_thenPreservesLocalTime() {
    TestClock clock = TestClock.from(at("2024-10-27T12:00:00+01:00"), WARSAW);

    Instant result = clock.rewind(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-10-26T12:00:00+02:00"));
  }

  @Test
  void givenClockAfterDstEnd_whenRewindDurationOfDays_thenMovesBackExactHours() {
    TestClock clock = TestClock.from(at("2024-10-27T12:00:00+01:00"), WARSAW);

    Instant result = clock.rewind(Duration.ofDays(1));

    assertThat(result).isEqualTo(at("2024-10-26T13:00:00+02:00"));
  }

  @Test
  void givenLocalTimeInDstOverlap_whenForwardPeriodOfDays_thenKeepsSummerOffset() {
    TestClock clock = TestClock.from(at("2024-10-26T02:30:00+02:00"), WARSAW);

    Instant result = clock.forward(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-10-27T02:30:00+02:00"));
  }

  @Test
  void givenLocalTimeInDstOverlap_whenRewindPeriodOfDays_thenKeepsWinterOffset() {
    TestClock clock = TestClock.from(at("2024-10-28T02:30:00+01:00"), WARSAW);

    Instant result = clock.rewind(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-10-27T02:30:00+01:00"));
  }

  @Test
  void givenClockInDstOverlap_whenForwardDuration_thenRepeatsLocalHour() {
    TestClock clock = TestClock.from(at("2024-10-27T02:30:00+02:00"), WARSAW);

    Instant result = clock.forward(Duration.ofHours(1));

    assertThat(result).isEqualTo(at("2024-10-27T02:30:00+01:00"));
  }

  @Test
  void givenClockInDstOverlap_whenForwardPeriodOfDays_thenUsesOffsetOfTargetDay() {
    TestClock clock = TestClock.from(at("2024-10-27T02:30:00+02:00"), WARSAW);

    Instant result = clock.forward(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-10-28T02:30:00+01:00"));
  }

  @Test
  void givenLocalTimeInDstGap_whenForwardThenRewindPeriodOfDays_thenDoesNotReturnToStart() {
    TestClock clock = TestClock.from(at("2024-03-30T02:30:00+01:00"), WARSAW);

    clock.forward(Period.ofDays(1));
    Instant result = clock.rewind(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-03-30T03:30:00+01:00"));
  }

  @Test
  void givenClockInDstOverlap_whenForwardThenRewindPeriodOfDays_thenDoesNotReturnToStart() {
    TestClock clock = TestClock.from(at("2024-10-27T02:30:00+02:00"), WARSAW);

    clock.forward(Period.ofDays(1));
    Instant result = clock.rewind(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-10-27T02:30:00+01:00"));
  }

  @Test
  void givenClockAcrossDstStart_whenForwardThenRewindDuration_thenReturnsToStart() {
    TestClock clock = TestClock.from(at("2024-03-30T12:00:00+01:00"), WARSAW);

    clock.forward(Duration.ofDays(1));
    Instant result = clock.rewind(Duration.ofDays(1));

    assertThat(result).isEqualTo(at("2024-03-30T12:00:00+01:00"));
  }

  @Test
  void givenTickingClockBeforeDstGap_whenInstantCalledRepeatedly_thenTicksExactDuration() {
    TestClock clock =
        TestClock.from(at("2024-03-31T01:30:00+01:00"), WARSAW)
            .withTickDuration(Duration.ofHours(1));

    assertThat(clock.instant()).isEqualTo(at("2024-03-31T01:30:00+01:00"));
    assertThat(clock.instant()).isEqualTo(at("2024-03-31T03:30:00+02:00"));
  }

  @Test
  void givenZoneWithoutDst_whenForwardPeriodOfDays_thenAdvancesExactly24Hours() {
    TestClock clock = TestClock.from(at("2024-03-30T12:00:00+09:00"), ZoneId.of("Asia/Tokyo"));

    Instant result = clock.forward(Period.ofDays(1));

    assertThat(result).isEqualTo(at("2024-03-31T12:00:00+09:00"));
  }

  @Test
  void givenUtcClock_whenForwardPeriodOfDaysOverWarsawDstStart_thenAdvancesExactly24Hours() {
    TestClock clock = TestClock.from("2024-03-30T12:00:00Z");

    Instant result = clock.forward(Period.ofDays(1));

    assertThat(result).isEqualTo(Instant.parse("2024-03-31T12:00:00Z"));
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

  @Test
  void givenClocksWithSameState_whenEquals_thenReturnsTrueAndHashCodesMatch() {
    ZoneId zone = ZoneId.of("Europe/Warsaw");
    Duration tick = Duration.ofSeconds(5);
    TestClock first = TestClock.from(BASE, zone).withTickDuration(tick);
    TestClock second = TestClock.from(BASE, zone).withTickDuration(tick);

    assertThat(first).isEqualTo(second);
    assertThat(second).isEqualTo(first);
    assertThat(first).hasSameHashCodeAs(second);
  }

  @Test
  void givenClock_whenEqualsItself_thenReturnsTrue() {
    TestClock clock = TestClock.from(BASE);

    assertThat(clock.equals(clock)).isTrue();
  }

  @Test
  void givenClocksWithDifferentCurrentTime_whenEquals_thenReturnsFalse() {
    TestClock first = TestClock.from(BASE);
    TestClock second = TestClock.from(BASE.plusNanos(1));

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void givenClocksWithDifferentZone_whenEquals_thenReturnsFalse() {
    TestClock first = TestClock.from(BASE, ZoneId.of("Europe/Warsaw"));
    TestClock second = TestClock.from(BASE, ZoneId.of("Europe/Paris"));

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void givenClocksWithUtcOffsetAndUtcRegion_whenEquals_thenReturnsFalse() {
    TestClock offset = TestClock.from(BASE, ZoneOffset.UTC);
    TestClock region = TestClock.from(BASE, ZoneId.of("UTC"));

    assertThat(offset).isNotEqualTo(region);
  }

  @Test
  void givenClocksWithDifferentTickDuration_whenEquals_thenReturnsFalse() {
    TestClock first = TestClock.from(BASE);
    TestClock second = first.withTickDuration(Duration.ofSeconds(1));

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void givenClock_whenEqualsNull_thenReturnsFalse() {
    assertThat(TestClock.from(BASE).equals(null)).isFalse();
  }

  @Test
  void givenFixedClockWithSameInstantAndZone_whenEquals_thenReturnsFalse() {
    TestClock clock = TestClock.from(BASE);
    Clock fixed = Clock.fixed(BASE, ZoneOffset.UTC);

    assertThat(clock).isNotEqualTo(fixed);
  }

  @Test
  void givenEqualClocks_whenOneForwarded_thenStopBeingEqual() {
    TestClock first = TestClock.from(BASE);
    TestClock second = TestClock.from(BASE);

    first.forwardSeconds(1);

    assertThat(first).isNotEqualTo(second);
    assertThat(first.hashCode()).isNotEqualTo(second.hashCode());
  }

  @Test
  void givenClockForwardedThenRewound_whenEquals_thenReturnsTrue() {
    TestClock first = TestClock.from(BASE);
    TestClock second = TestClock.from(BASE);

    first.forwardSeconds(1);
    first.rewindSeconds(1);

    assertThat(first).isEqualTo(second);
  }

  @Test
  void givenClock_whenSerializedAndDeserialized_thenStateIsPreserved() throws Exception {
    TestClock original = TestClock.from(BASE, ZoneId.of("Europe/Warsaw"));
    original.forwardSeconds(30);

    TestClock deserialized = serializeAndDeserialize(original);

    assertThat(deserialized.getCurrentTime()).isEqualTo(original.getCurrentTime());
    assertThat(deserialized.getZone()).isEqualTo(original.getZone());
    assertThat(deserialized).isEqualTo(original);
  }

  @Test
  void givenTickingClock_whenSerializedAndDeserialized_thenTickDurationIsPreserved()
      throws Exception {
    TestClock original = TestClock.from(BASE).withTickDuration(Duration.ofSeconds(5));

    TestClock deserialized = serializeAndDeserialize(original);

    Instant first = deserialized.instant();
    Instant second = deserialized.instant();
    assertThat(second).isEqualTo(first.plusSeconds(5));
  }

  private static Instant at(String dateTimeWithOffset) {
    return OffsetDateTime.parse(dateTimeWithOffset).toInstant();
  }

  private static TestClock serializeAndDeserialize(TestClock clock) throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(buf)) {
      out.writeObject(clock);
    }
    try (ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(buf.toByteArray()))) {
      return (TestClock) in.readObject();
    }
  }
}
