/*
 * Copyright 2026 Damian Malczewski
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

package io.github.malczuuu.testclock

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import spock.lang.Specification

class TestClockSpec extends Specification {

  private static final Instant BASE = Instant.parse("2024-06-15T12:00:00Z")

  def "from(Instant) uses UTC zone and preserves instant"() {
	given:
	def clock = TestClock.from(BASE)

	expect:
	clock.zone == ZoneOffset.UTC
	clock.instant() == BASE
  }

  def "from(Instant, ZoneId) preserves zone and instant"() {
	given:
	def zone = ZoneId.of("Europe/Warsaw")
	def clock = TestClock.from(BASE, zone)

	expect:
	clock.zone == zone
	clock.instant() == BASE
  }

  def "from(String) parses instant and uses UTC zone"() {
	given:
	def clock = TestClock.from("2024-06-15T12:00:00Z")

	expect:
	clock.zone == ZoneOffset.UTC
	clock.instant() == BASE
  }

  def "from(String, ZoneId) parses instant and preserves zone"() {
	given:
	def zone = ZoneId.of("Europe/Warsaw")
	def clock = TestClock.from("2024-06-15T12:00:00Z", zone)

	expect:
	clock.zone == zone
	clock.instant() == BASE
  }

  def "from(String) throws DateTimeParseException for invalid text"() {
	when:
	TestClock.from("not-an-instant")

	then:
	thrown(DateTimeParseException)
  }

  def "from(Clock) preserves sub-second precision"() {
	given:
	def withNanos = Instant.parse("2024-06-15T12:00:00.123456789Z")
	def source = Clock.fixed(withNanos, ZoneOffset.UTC)
	def clock = TestClock.from(source)

	expect:
	clock.instant() == withNanos
  }

  def "from(Clock) preserves zone"() {
	given:
	def zone = ZoneId.of("America/New_York")
	def source = Clock.system(zone)
	def clock = TestClock.from(source)

	expect:
	clock.zone == zone
  }

  def "fromSystemUTC() uses UTC zone"() {
	expect:
	TestClock.fromSystemUTC().zone == ZoneOffset.UTC
  }

  def "fromSystem(ZoneId) preserves zone"() {
	given:
	def zone = ZoneId.of("Asia/Tokyo")

	expect:
	TestClock.fromSystem(zone).zone == zone
  }

  def "setCurrentTime replaces the current instant"() {
	given:
	def clock = TestClock.from(BASE)
	def newTime = BASE.plusSeconds(3600)

	when:
	clock.setCurrentTime(newTime)

	then:
	clock.instant() == newTime
  }

  def "setCurrentTime on ticking clock affects the next instant"() {
	given:
	def clock = TestClock.from(BASE).withTickDuration(Duration.ofSeconds(1))
	def newTime = BASE.plusSeconds(100)

	when:
	clock.setCurrentTime(newTime)

	then:
	clock.instant() == newTime
	clock.instant() == newTime.plusSeconds(1)
  }

  def "forward(Duration) advances time by the given duration"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.forward(Duration.ofMinutes(5))

	then:
	result == BASE.plusSeconds(300)
	clock.instant() == BASE.plusSeconds(300)
  }

  def "forwardSeconds advances time by the given seconds"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.forwardSeconds(10)

	then:
	result == BASE.plusSeconds(10)
  }

  def "forwardMillis advances time by the given milliseconds"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.forwardMillis(500)

	then:
	result == BASE.plusMillis(500)
  }

  def "forwardNanos advances time by the given nanoseconds"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.forwardNanos(1_000_000L)

	then:
	result == BASE.plusNanos(1_000_000L)
  }

  def "forward(long, TemporalUnit) advances time by the given amount"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.forward(3, ChronoUnit.HOURS)

	then:
	result == BASE.plus(3, ChronoUnit.HOURS)
	clock.instant() == BASE.plus(3, ChronoUnit.HOURS)
  }

  def "rewind(Duration) moves time back by the given duration"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.rewind(Duration.ofMinutes(5))

	then:
	result == BASE.minusSeconds(300)
	clock.instant() == BASE.minusSeconds(300)
  }

  def "rewindSeconds moves time back by the given seconds"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.rewindSeconds(10)

	then:
	result == BASE.minusSeconds(10)
  }

  def "rewindMillis moves time back by the given milliseconds"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.rewindMillis(500)

	then:
	result == BASE.minusMillis(500)
  }

  def "rewindNanos moves time back by the given nanoseconds"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.rewindNanos(1_000_000L)

	then:
	result == BASE.minusNanos(1_000_000L)
  }

  def "rewind(long, TemporalUnit) moves time back by the given amount"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.rewind(3, ChronoUnit.HOURS)

	then:
	result == BASE.minus(3, ChronoUnit.HOURS)
	clock.instant() == BASE.minus(3, ChronoUnit.HOURS)
  }

  def "forward then rewind results in the net time change"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	clock.forwardSeconds(60)
	clock.rewindSeconds(20)

	then:
	clock.instant() == BASE.plusSeconds(40)
  }

  def "forwardSeconds(0) leaves the clock unchanged"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.forwardSeconds(0)

	then:
	result == BASE
	clock.instant() == BASE
  }

  def "rewind(Duration.ZERO) leaves the clock unchanged"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	def result = clock.rewind(Duration.ZERO)

	then:
	result == BASE
	clock.instant() == BASE
  }

  def "non-ticking clock returns the same instant on repeated calls"() {
	given:
	def clock = TestClock.from(BASE)

	expect:
	clock.instant() == BASE
	clock.instant() == BASE
	clock.instant() == BASE
  }

  def "ticking clock advances time on each instant() call"() {
	given:
	def clock = TestClock.from(BASE).withTickDuration(Duration.ofSeconds(1))

	expect:
	clock.instant() == BASE
	clock.instant() == BASE.plusSeconds(1)
	clock.instant() == BASE.plusSeconds(2)
  }

  def "withTickDuration does not affect the original clock"() {
	given:
	def original = TestClock.from(BASE)
	def ticking = original.withTickDuration(Duration.ofSeconds(5))

	when:
	ticking.instant()

	then:
	original.instant() == BASE
  }

  def "withTickDuration captures the current time at the time of the call"() {
	given:
	def clock = TestClock.from(BASE)
	clock.forwardSeconds(30)

	when:
	def ticking = clock.withTickDuration(Duration.ofSeconds(1))

	then:
	ticking.instant() == BASE.plusSeconds(30)
  }

  def "withTickDuration(ZERO) produces a non-ticking clock"() {
	given:
	def ticking = TestClock.from(BASE).withTickDuration(Duration.ofSeconds(5))

	when:
	def stopped = ticking.withTickDuration(Duration.ZERO)

	then:
	stopped.tickDuration == Duration.ZERO
	stopped.instant() == BASE
	stopped.instant() == BASE
  }

  def "getCurrentTime does not advance a ticking clock"() {
	given:
	def clock = TestClock.from(BASE).withTickDuration(Duration.ofSeconds(1))

	expect:
	clock.currentTime == BASE
	clock.currentTime == BASE
  }

  def "getCurrentTime returns the latest time after forwarding"() {
	given:
	def clock = TestClock.from(BASE)
	clock.forwardSeconds(15)

	expect:
	clock.currentTime == BASE.plusSeconds(15)
  }

  def "getTickDuration returns ZERO for a non-ticking clock"() {
	expect:
	TestClock.from(BASE).tickDuration == Duration.ZERO
  }

  def "getTickDuration returns the configured duration"() {
	expect:
	TestClock.from(BASE).withTickDuration(Duration.ofSeconds(5)).tickDuration == Duration.ofSeconds(5)
  }

  def "withZone returns a clock with the new zone"() {
	given:
	def clock = TestClock.from(BASE)
	def zone = ZoneId.of("Europe/Paris")

	expect:
	clock.withZone(zone).zone == zone
  }

  def "withZone preserves the current instant"() {
	given:
	def clock = TestClock.from(BASE)
	clock.forwardSeconds(60)

	expect:
	clock.withZone(ZoneId.of("America/Chicago")).instant() == BASE.plusSeconds(60)
  }

  def "withZone does not affect the original clock's zone"() {
	given:
	def clock = TestClock.from(BASE)

	when:
	clock.withZone(ZoneId.of("Asia/Seoul"))

	then:
	clock.zone == ZoneOffset.UTC
  }

  def "withZone clone is independent from the original clock"() {
	given:
	def original = TestClock.from(BASE)
	def clone = original.withZone(ZoneId.of("Europe/Warsaw"))

	when:
	original.forwardSeconds(60)

	then:
	clone.instant() == BASE
  }

  def "withZone preserves the tick duration"() {
	given:
	def tick = Duration.ofSeconds(3)
	def clock = TestClock.from(BASE).withTickDuration(tick)

	when:
	def zoned = clock.withZone(ZoneId.of("Europe/Warsaw"))

	then:
	zoned.tickDuration == tick
	zoned.instant() == BASE
	zoned.instant() == BASE.plusSeconds(3)
  }

  def "toString for non-ticking clock omits tick segment"() {
	expect:
	TestClock.from(BASE).toString() == "TestClock[2024-06-15T12:00:00Z,Z]"
  }

  def "toString for ticking clock includes tick segment"() {
	expect:
	TestClock.from(BASE).withTickDuration(Duration.ofSeconds(5)).toString() == "TestClock[2024-06-15T12:00:00Z,Z,tick=PT5S]"
  }
}
