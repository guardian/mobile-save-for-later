package com.gu.sfl.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import java.util.zip.{Deflater, GZIPInputStream, GZIPOutputStream}

import com.gu.sfl.util.Base64Helper.{base64Decode, base64Encode}
import org.apache.commons.io.IOUtils

object Base64Helper {
  private val encoder: Base64.Encoder = Base64.getEncoder
  private val decoder: Base64.Decoder = Base64.getDecoder

  def base64Encode(outputStream: OutputStream): OutputStream = encoder.wrap(outputStream)

  def base64Decode(inputStream: InputStream): InputStream = decoder.wrap(inputStream)
}

object Compression {
  def encode(outputStream: OutputStream): OutputStream = {
    val gZIPOutputStream = new GZIPOutputStream(outputStream) {
      def setLevel(level: Int) = {
        this.`def` = new Deflater(level, true)
      }
    }
    gZIPOutputStream.setLevel(Deflater.BEST_COMPRESSION) // open for experimentation
    gZIPOutputStream
  }

  def decode(inputStream: InputStream): InputStream = new GZIPInputStream(inputStream)
}

object SealedCompression {

  sealed class Compression(val decode: InputStream => InputStream, val encode: OutputStream => OutputStream, val contentEncoding: String) {
    def decodeFromBase64(base64Gzipped: String): String =
      IOUtils.toString(
        decode(
          base64Decode(new ByteArrayInputStream(base64Gzipped.getBytes(UTF_8)))), UTF_8)


    def encodeToBase64(text: String): String = {
      val byteArrayOutputStream = new ByteArrayOutputStream()
      val base64Stream = base64Encode(byteArrayOutputStream)
      val wrappedStream = encode(base64Stream)
      IOUtils.write(text, wrappedStream, UTF_8)
      wrappedStream.close()
      base64Stream.close()
      new String(byteArrayOutputStream.toByteArray, UTF_8)
    }
  }

  case object Gzip extends Compression(Compression.decode, Compression.encode, "gzip")

  val contentEncodings: Map[String, Compression] = List(Gzip).groupBy(_.contentEncoding).mapValues(_.head)
}