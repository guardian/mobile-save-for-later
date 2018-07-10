package sfl.lib

import java.util.Base64

trait Base64Utils {
  val decoder: Base64.Decoder = Base64.getDecoder
  val encoder: Base64.Encoder = Base64.getEncoder
  val IsBase64Encoded = true
  val IsNotBase64Encoded = false
  def encodeByeArray(bytes: Array[Byte]) = new String(encoder.encode(bytes))
 }
