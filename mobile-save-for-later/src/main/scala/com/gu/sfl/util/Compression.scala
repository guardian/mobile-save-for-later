package com.gu.sfl.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import java.util.zip.{Deflater, GZIPInputStream, GZIPOutputStream}

import org.apache.commons.io.IOUtils

object Base64Helper {
  val encoder: Base64.Encoder = Base64.getEncoder
  val decoder: Base64.Decoder = Base64.getDecoder

  def encode(outputStream: OutputStream): OutputStream = {
    encoder.wrap(outputStream)
  }

  def decode(inputStream: InputStream): InputStream = {
    decoder.wrap(inputStream)
  }

}

object Compression {

  def encode(outputStream: OutputStream): OutputStream = {
    val stream = new GZIPOutputStream(outputStream) {
      def setLevel(level: Int) = {
        this.`def` = new Deflater(level, true)
      }
    }
    stream.setLevel(Deflater.BEST_COMPRESSION)
    stream
  }

  def decode(inputStream: InputStream): InputStream = {
    new GZIPInputStream(inputStream)
  }
}

object SealedCompression {

  sealed class Compression(val decode: InputStream => InputStream, val encode: OutputStream => OutputStream, val contentEncoding: String) {


    def decodeFromBase64(base64Gzipped: String): String = {
      val byteArrayInputStream = new ByteArrayInputStream(base64Gzipped.getBytes(UTF_8))
      IOUtils.toString(decode(Base64Helper.decode(byteArrayInputStream)), UTF_8)
    }

    def encodeToBase64(text: String): String = {
      val byteArrayOutputStream = new ByteArrayOutputStream()
      val base64Stream = Base64Helper.encode(byteArrayOutputStream)
      val wrappedStream = encode(base64Stream)
      IOUtils.write(text, wrappedStream, UTF_8)
      wrappedStream.close()
      base64Stream.close()
      new String(byteArrayOutputStream.toByteArray, UTF_8)
    }
  }

  case object Gzip extends Compression(Compression.decode, Compression.encode, "gzip")

  case object Brotli extends Compression(x => x, x => x, "br")

  val contentEncodings: Map[String, Compression] = List(Gzip, Brotli).groupBy(_.contentEncoding).mapValues(_.head)
}