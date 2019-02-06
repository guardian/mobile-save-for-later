package com.gu.sfl.util

import com.gu.sfl.util.SealedCompression.Gzip
import org.specs2.mutable.Specification

class CompressionSpec extends Specification {
  val gzippedBackAndForth: String = "H4sIAAAAAAAAAHNKTM5WSMxLUXDLLyrJAACgDPjcDgAAAA=="

  val backAndForth: String = "Back and Forth"
  "Compression" should {

    "gzip back and forth" in {
      val encoded = Gzip.encodeToBase64(backAndForth)
      encoded must beEqualTo(gzippedBackAndForth)
      val decoded = Gzip.decodeFromBase64(encoded)
      decoded must beEqualTo(backAndForth)
    }
  }
}