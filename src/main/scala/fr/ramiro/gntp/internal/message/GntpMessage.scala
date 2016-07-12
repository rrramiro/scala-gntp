package fr.ramiro.gntp.internal.message

import java.nio.charset._
import java.security.MessageDigest

import fr.ramiro.gntp.GntpPassword
import fr.ramiro.gntp.internal.GntpMessageType.GntpMessageType
import fr.ramiro.gntp.util.Hex

object GntpMessage {
  val PROTOCOL_ID: String = "GNTP"
  val VERSION = "1.0"
  val SEPARATOR: String = "\r\n"
  val HEADER_SEPARATOR: Char = ':'
  val ENCODING: Charset = StandardCharsets.UTF_8
  val DATE_TIME_FORMAT: String = "yyyy-MM-dd'T'HH:mm:ssZ"
  val DATE_TIME_FORMAT_ALTERNATE: String = "yyyy-MM-dd HH:mm:ss'Z'"
  val DATE_TIME_FORMAT_GROWL_1_3: String = "yyyy-MM-dd"
  val IMAGE_FORMAT: String = "png"
  val BINARY_SECTION_ID: String = "Identifier:"
  val BINARY_SECTION_LENGTH: String = "Length:"
  val BINARY_SECTION_PREFIX: String = "x-growl-resource://"
}

trait GntpMessage {
  val `type`: GntpMessageType
}

case class BinarySection(data: Array[Byte]) {
  val id = {
    val digest = MessageDigest.getInstance(GntpPassword.BINARY_HASH_FUNCTION)
    digest.update(data)
    Hex.toHexadecimal(digest.digest)
  }
}

