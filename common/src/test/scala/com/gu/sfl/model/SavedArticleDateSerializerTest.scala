package com.gu.sfl.model

import java.time.LocalDateTime

import org.specs2.mutable.Specification

class SavedArticleDateSerializerTest extends Specification {
  "SavedArticleDateSerializer.parse" should {
    "parse a date without milliseconds" in {
      SavedArticleDateSerializer.parse("2026-07-17T10:15:30Z") must beEqualTo(
        LocalDateTime.of(2026, 7, 17, 10, 15, 30)
      )
    }

    "parse a date with milliseconds" in {
      SavedArticleDateSerializer.parse("2026-07-17T10:15:30.123Z") must beEqualTo(
        LocalDateTime.of(2026, 7, 17, 10, 15, 30, 123000000)
      )
    }
  }

  "SavedArticleDateSerializer.outputFormatter" should {
    "always write dates without milliseconds" in {
      val date = LocalDateTime.of(2026, 7, 17, 10, 15, 30, 123000000)
      SavedArticleDateSerializer.outputFormatter.format(date) must beEqualTo("2026-07-17T10:15:30Z")
    }
  }
}
