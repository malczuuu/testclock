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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

/**
 * Documents what each date-time type reads from a {@link TestClock}.
 *
 * <p>In 2024, Warsaw skips 02:00-03:00 on March 31 (offset changes from +01:00 to +02:00) and
 * repeats 02:00-03:00 on October 27 (offset changes from +02:00 to +01:00).
 */
class TemporalTests {

  private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

  @Test
  void givenInstant_whenForwardOneHourOverDstStart_thenMovesExactlyOneHour() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", WARSAW);
    assertThat(Instant.now(clock)).isEqualTo("2024-03-31T00:30:00Z");

    clock.forward(Duration.ofHours(1));

    assertThat(Instant.now(clock)).isEqualTo("2024-03-31T01:30:00Z");
  }

  @Test
  void givenZonedDateTime_whenForwardOneHourOverDstStart_thenSkipsMissingHour() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", WARSAW);
    assertThat(ZonedDateTime.now(clock)).hasToString("2024-03-31T01:30+01:00[Europe/Warsaw]");

    clock.forward(Duration.ofHours(1));

    assertThat(ZonedDateTime.now(clock)).hasToString("2024-03-31T03:30+02:00[Europe/Warsaw]");
  }

  @Test
  void givenZonedDateTime_whenForwardOneHourOverDstEnd_thenRepeatsHourWithNewOffset() {
    TestClock clock = TestClock.from("2024-10-27T00:30:00Z", WARSAW);
    assertThat(ZonedDateTime.now(clock)).hasToString("2024-10-27T02:30+02:00[Europe/Warsaw]");

    clock.forward(Duration.ofHours(1));

    assertThat(ZonedDateTime.now(clock)).hasToString("2024-10-27T02:30+01:00[Europe/Warsaw]");
  }

  @Test
  void givenOffsetDateTime_whenForwardOneHourOverDstStart_thenReadsNewOffset() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", WARSAW);
    assertThat(OffsetDateTime.now(clock)).hasToString("2024-03-31T01:30+01:00");

    clock.forward(Duration.ofHours(1));

    assertThat(OffsetDateTime.now(clock)).hasToString("2024-03-31T03:30+02:00");
  }

  @Test
  void givenOffsetDateTime_whenForwardOneHourOverDstEnd_thenReadsNewOffset() {
    TestClock clock = TestClock.from("2024-10-27T00:30:00Z", WARSAW);
    assertThat(OffsetDateTime.now(clock)).hasToString("2024-10-27T02:30+02:00");

    clock.forward(Duration.ofHours(1));

    assertThat(OffsetDateTime.now(clock)).hasToString("2024-10-27T02:30+01:00");
  }

  @Test
  void givenOffsetTime_whenForwardOneHourOverDstStart_thenReadsNewOffset() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", WARSAW);
    assertThat(OffsetTime.now(clock)).hasToString("01:30+01:00");

    clock.forward(Duration.ofHours(1));

    assertThat(OffsetTime.now(clock)).hasToString("03:30+02:00");
  }

  @Test
  void givenOffsetTime_whenForwardOneHourOverDstEnd_thenOnlyOffsetChanges() {
    TestClock clock = TestClock.from("2024-10-27T00:30:00Z", WARSAW);
    assertThat(OffsetTime.now(clock)).hasToString("02:30+02:00");

    clock.forward(Duration.ofHours(1));

    assertThat(OffsetTime.now(clock)).hasToString("02:30+01:00");
  }

  @Test
  void givenLocalDateTime_whenForwardOneHourOverDstStart_thenSkipsMissingHour() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", WARSAW);
    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-31T01:30");

    clock.forward(Duration.ofHours(1));

    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-31T03:30");
  }

  @Test
  void givenLocalDateTime_whenForwardOneHourOverDstEnd_thenReadsSameValueAgain() {
    TestClock clock = TestClock.from("2024-10-27T00:30:00Z", WARSAW);
    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-10-27T02:30");

    clock.forward(Duration.ofHours(1));

    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-10-27T02:30");
  }

  @Test
  void givenLocalDateTime_whenForwardPeriodOfOneDayOverDstStart_thenKeepsLocalTime() {
    TestClock clock = TestClock.from("2024-03-30T11:00:00Z", WARSAW);
    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-30T12:00");

    clock.forward(Period.ofDays(1));

    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-31T12:00");
  }

  @Test
  void givenLocalDateTime_whenForwardDurationOfOneDayOverDstStart_thenLocalTimeMovesOneHour() {
    TestClock clock = TestClock.from("2024-03-30T11:00:00Z", WARSAW);
    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-30T12:00");

    clock.forward(Duration.ofDays(1));

    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-31T13:00");
  }

  @Test
  void givenLocalTime_whenForwardOneHourOverDstStart_thenSkipsMissingHour() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", WARSAW);
    assertThat(LocalTime.now(clock)).isEqualTo("01:30");

    clock.forward(Duration.ofHours(1));

    assertThat(LocalTime.now(clock)).isEqualTo("03:30");
  }

  @Test
  void givenLocalTime_whenForwardOneHourOverDstEnd_thenReadsSameValueAgain() {
    TestClock clock = TestClock.from("2024-10-27T00:30:00Z", WARSAW);
    assertThat(LocalTime.now(clock)).isEqualTo("02:30");

    clock.forward(Duration.ofHours(1));

    assertThat(LocalTime.now(clock)).isEqualTo("02:30");
  }

  @Test
  void givenSameInstant_whenLocalDateReadInDifferentZones_thenUsesDateOfClockZone() {
    TestClock warsaw = TestClock.from("2024-12-31T23:30:00Z", WARSAW);
    TestClock utc = TestClock.from("2024-12-31T23:30:00Z");

    assertThat(LocalDate.now(warsaw)).isEqualTo("2025-01-01");
    assertThat(LocalDate.now(utc)).isEqualTo("2024-12-31");
  }

  @Test
  void givenNewYearInClockZone_whenReadYear_thenReturnsNewYear() {
    TestClock clock = TestClock.from("2024-12-31T23:30:00Z", WARSAW);

    assertThat(Year.now(clock)).isEqualTo(Year.of(2025));
  }

  @Test
  void givenNewYearInClockZone_whenReadYearMonth_thenReturnsNewMonth() {
    TestClock clock = TestClock.from("2024-12-31T23:30:00Z", WARSAW);

    assertThat(YearMonth.now(clock)).isEqualTo(YearMonth.of(2025, 1));
  }

  @Test
  void givenNewYearInClockZone_whenReadMonthDay_thenReturnsNewDay() {
    TestClock clock = TestClock.from("2024-12-31T23:30:00Z", WARSAW);

    assertThat(MonthDay.now(clock)).isEqualTo(MonthDay.of(1, 1));
  }

  @Test
  void givenNewYearInClockZone_whenReadMonth_thenReturnsNewMonth() {
    TestClock clock = TestClock.from("2024-12-31T23:30:00Z", WARSAW);

    assertThat(Month.from(LocalDate.now(clock))).isEqualTo(Month.JANUARY);
  }

  @Test
  void givenNewYearInClockZone_whenReadDayOfWeek_thenReturnsNewDay() {
    TestClock clock = TestClock.from("2024-12-31T23:30:00Z", WARSAW);

    assertThat(DayOfWeek.from(LocalDate.now(clock))).isEqualTo(DayOfWeek.WEDNESDAY);
  }

  @Test
  void givenNewYearInClockZone_whenReadJapaneseDate_thenReturnsNewYearInJapaneseCalendar() {
    TestClock clock = TestClock.from("2024-12-31T23:30:00Z", WARSAW);

    assertThat(JapaneseDate.now(clock)).isEqualTo(JapaneseDate.of(JapaneseEra.REIWA, 7, 1, 1));
  }

  @Test
  void givenNewYearInClockZone_whenReadHijrahDate_thenReturnsNewDayInHijrahCalendar() {
    TestClock clock = TestClock.from("2024-12-31T23:30:00Z", WARSAW);

    assertThat(HijrahDate.now(clock)).isEqualTo(HijrahDate.of(1446, 7, 1));
  }

  @Test
  void givenNewYearInClockZone_whenReadMinguoDate_thenReturnsNewYearInMinguoCalendar() {
    TestClock clock = TestClock.from("2024-12-31T23:30:00Z", WARSAW);

    assertThat(MinguoDate.now(clock)).isEqualTo(MinguoDate.of(114, 1, 1));
  }

  @Test
  void givenNewYearInClockZone_whenReadThaiBuddhistDate_thenReturnsNewYearInThaiCalendar() {
    TestClock clock = TestClock.from("2024-12-31T23:30:00Z", WARSAW);

    assertThat(ThaiBuddhistDate.now(clock)).isEqualTo(ThaiBuddhistDate.of(2568, 1, 1));
  }

  @Test
  void givenZonedDateTimeReadFromClock_whenPlusOneHourOverDstStart_thenFollowsZoneRules() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", WARSAW);

    ZonedDateTime result = ZonedDateTime.now(clock).plusHours(1);

    assertThat(result).hasToString("2024-03-31T03:30+02:00[Europe/Warsaw]");
  }

  @Test
  void givenOffsetDateTimeReadFromClock_whenPlusOneHourOverDstStart_thenKeepsOldOffset() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", WARSAW);

    OffsetDateTime result = OffsetDateTime.now(clock).plusHours(1);

    assertThat(result).hasToString("2024-03-31T02:30+01:00");
  }

  @Test
  void givenLocalDateTimeReadFromClock_whenPlusOneHourOverDstStart_thenReturnsMissingHour() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", WARSAW);

    LocalDateTime result = LocalDateTime.now(clock).plusHours(1);

    assertThat(result).isEqualTo("2024-03-31T02:30");
    assertThat(result.atZone(WARSAW)).hasToString("2024-03-31T03:30+02:00[Europe/Warsaw]");
  }

  @Test
  void givenFixedOffset_whenForwardPeriodOfOneDayOverWarsawDstStart_thenMovesExactly24Hours() {
    TestClock clock = TestClock.from("2024-03-30T11:00:00Z", ZoneOffset.ofHours(1));
    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-30T12:00");

    clock.forward(Period.ofDays(1));

    assertThat(clock.getCurrentTime()).isEqualTo("2024-03-31T11:00:00Z");
    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-31T12:00");
  }

  @Test
  void givenFixedOffset_whenForwardOneHourOverWarsawDstStart_thenReadsHourMissingInWarsaw() {
    TestClock clock = TestClock.from("2024-03-31T00:30:00Z", ZoneOffset.ofHours(1));
    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-31T01:30");

    clock.forward(Duration.ofHours(1));

    assertThat(LocalDateTime.now(clock)).isEqualTo("2024-03-31T02:30");
  }

  @Test
  void givenFixedOffset_whenReadOffsetDateTimeInSummer_thenKeepsOffsetUnlikeWarsaw() {
    TestClock fixed = TestClock.from("2024-07-15T10:00:00Z", ZoneOffset.ofHours(1));
    TestClock warsaw = TestClock.from("2024-07-15T10:00:00Z", WARSAW);

    assertThat(OffsetDateTime.now(fixed)).hasToString("2024-07-15T11:00+01:00");
    assertThat(OffsetDateTime.now(warsaw)).hasToString("2024-07-15T12:00+02:00");
  }

  @Test
  void givenOffsetParsedFromText_whenForwardPeriodOfOneDay_thenBehavesLikeZoneOffset() {
    TestClock parsed = TestClock.from("2024-03-30T11:00:00Z", ZoneId.of("+01:00"));
    TestClock offset = TestClock.from("2024-03-30T11:00:00Z", ZoneOffset.ofHours(1));

    parsed.forward(Period.ofDays(1));
    offset.forward(Period.ofDays(1));

    assertThat(parsed.getZone()).isEqualTo(offset.getZone());
    assertThat(parsed.getCurrentTime()).isEqualTo(offset.getCurrentTime());
  }

  @Test
  void givenLegacyDate_whenClockCreatedFromDateToInstant_thenClockStartsAtThatDate() {
    Date date = new Date(1_718_452_800_123L);

    TestClock clock = TestClock.from(date.toInstant(), WARSAW);

    assertThat(clock.getCurrentTime()).isEqualTo("2024-06-15T12:00:00.123Z");
  }

  @Test
  void givenLegacyDate_whenSetCurrentTimeFromDateToInstant_thenClockMovesToThatDate() {
    TestClock clock = TestClock.from(Instant.EPOCH, WARSAW);
    Date date = new Date(1_718_452_800_123L);

    clock.setCurrentTime(date.toInstant());

    assertThat(clock.getCurrentTime()).isEqualTo("2024-06-15T12:00:00.123Z");
  }

  @Test
  void givenClockCreatedFromLegacyDate_whenDateFromClockInstant_thenReturnsEqualDate() {
    Date date = new Date(1_718_452_800_123L);
    TestClock clock = TestClock.from(date.toInstant(), WARSAW);

    assertThat(Date.from(clock.instant())).isEqualTo(date);
  }

  @Test
  void givenClockCreatedFromLegacyDate_whenDateFromClockMillis_thenReturnsEqualDate() {
    Date date = new Date(1_718_452_800_123L);
    TestClock clock = TestClock.from(date.toInstant(), WARSAW);

    assertThat(new Date(clock.millis())).isEqualTo(date);
  }

  @Test
  void givenClockWithNanos_whenDateFromClockInstant_thenDateTruncatesToMillis() {
    TestClock clock = TestClock.from("2024-06-15T12:00:00.123456789Z");

    Date date = Date.from(clock.instant());

    assertThat(date.toInstant()).isEqualTo("2024-06-15T12:00:00.123Z");
    assertThat(clock.getCurrentTime()).isEqualTo("2024-06-15T12:00:00.123456789Z");
  }

  @Test
  void givenLegacyDate_whenForwardOneHourOverDstStart_thenFormatsAfterMissingHour()
      throws ParseException {
    SimpleDateFormat format = warsawFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    Date start = format.parse("2024-03-31T01:30:00+01:00");
    TestClock clock = TestClock.from(start.toInstant(), WARSAW);

    clock.forward(Duration.ofHours(1));

    assertThat(format.format(Date.from(clock.instant()))).isEqualTo("2024-03-31T03:30:00+02:00");
  }

  @Test
  void givenLegacyDate_whenForwardOneHourOverDstEnd_thenFormatsSameTimeWithNewOffset()
      throws ParseException {
    SimpleDateFormat format = warsawFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    Date start = format.parse("2024-10-27T02:30:00+02:00");
    TestClock clock = TestClock.from(start.toInstant(), WARSAW);

    clock.forward(Duration.ofHours(1));

    assertThat(format.format(Date.from(clock.instant()))).isEqualTo("2024-10-27T02:30:00+01:00");
  }

  @Test
  void givenLegacyDate_whenForwardPeriodOfOneDayOverDstStart_thenFormatsSameLocalTime()
      throws ParseException {
    SimpleDateFormat format = warsawFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    Date start = format.parse("2024-03-30T12:00:00+01:00");
    TestClock clock = TestClock.from(start.toInstant(), WARSAW);

    clock.forward(Period.ofDays(1));

    assertThat(format.format(Date.from(clock.instant()))).isEqualTo("2024-03-31T12:00:00+02:00");
  }

  @Test
  void givenWallClockTimeInDstGap_whenParsedByLegacyDateAndJavaTime_thenBothPickSameInstant()
      throws ParseException {
    Date legacy = warsawFormat("yyyy-MM-dd'T'HH:mm:ss").parse("2024-03-31T02:30:00");
    ZonedDateTime modern = LocalDateTime.parse("2024-03-31T02:30:00").atZone(WARSAW);

    assertThat(legacy.toInstant()).isEqualTo("2024-03-31T01:30:00Z");
    assertThat(modern.toInstant()).isEqualTo("2024-03-31T01:30:00Z");
  }

  @Test
  void givenWallClockTimeInDstOverlap_whenParsedByLegacyDateAndJavaTime_thenInstantsDiffer()
      throws ParseException {
    Date legacy = warsawFormat("yyyy-MM-dd'T'HH:mm:ss").parse("2024-10-27T02:30:00");
    ZonedDateTime modern = LocalDateTime.parse("2024-10-27T02:30:00").atZone(WARSAW);

    assertThat(legacy.toInstant()).isEqualTo("2024-10-27T01:30:00Z");
    assertThat(modern.toInstant()).isEqualTo("2024-10-27T00:30:00Z");
  }

  private static SimpleDateFormat warsawFormat(String pattern) {
    SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
    format.setTimeZone(TimeZone.getTimeZone(WARSAW));
    return format;
  }
}
