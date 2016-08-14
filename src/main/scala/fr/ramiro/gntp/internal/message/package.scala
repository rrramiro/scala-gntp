package fr.ramiro.gntp.internal

import java.awt.image.RenderedImage
import java.io.{ ByteArrayOutputStream, InputStream }
import java.net.URI
import java.security.MessageDigest
import java.text.{ DateFormat, SimpleDateFormat }
import java.util.Date
import javax.imageio.ImageIO

import fr.ramiro.gntp.GntpPassword
import fr.ramiro.gntp.util.Hex

import scala.language.implicitConversions

package object message {

  private final val dateFormat: DateFormat = new SimpleDateFormat(GntpMessage.DATE_TIME_FORMAT)

  sealed trait HeaderObject {
    def toHeader: String
  }

  trait BinaryHeaderValue extends HeaderObject {
    val byteArray: Array[Byte]
    lazy val uniqueId: String = {
      val digest = MessageDigest.getInstance(GntpPassword.BINARY_HASH_FUNCTION)
      digest.update(byteArray)
      Hex.toHexadecimal(digest.digest)
    }
    def toHeader: String = GntpMessage.BINARY_SECTION_PREFIX + uniqueId
  }

  object HeaderSpacer extends HeaderObject {
    def toHeader: String = GntpMessage.SEPARATOR
  }

  implicit def toHeaderString(field: String): HeaderObject = new HeaderObject {
    override def toHeader: String = field.replaceAll("\r\n", "\n")
  }
  implicit def toHeaderNumber(field: Number): HeaderObject = new HeaderObject {
    override def toHeader: String = field.toString
  }
  implicit def toHeaderLong(field: Long): HeaderObject = toHeaderNumber(field)
  implicit def toHeaderInt(field: Int): HeaderObject = toHeaderNumber(field)
  implicit def toHeaderBoolean(field: Boolean): HeaderObject = new HeaderObject {
    override def toHeader: String = field.toString.toLowerCase.capitalize
  }
  implicit def toHeaderDate(field: Date): HeaderObject = new HeaderObject {
    override def toHeader: String = dateFormat.format(field)
  }
  implicit def toHeaderUri(field: URI): HeaderObject = new HeaderObject {
    override def toHeader: String = field.toString
  }
  implicit def toHeaderArrayBytes(field: Array[Byte]): BinaryHeaderValue = new BinaryHeaderValue {
    override val byteArray: Array[Byte] = field
  }
  implicit def toHeaderInputStream(field: InputStream): BinaryHeaderValue = new BinaryHeaderValue {
    val byteArray: Array[Byte] = try {
      Stream.continually(field.read)
        .takeWhile(_ != -1)
        .map(_.toByte).toArray
    } finally {
      field.close()
    }
  }

  implicit def toHeaderRenderedImage(field: RenderedImage): BinaryHeaderValue = new BinaryHeaderValue {
    val byteArray = {
      val output: ByteArrayOutputStream = new ByteArrayOutputStream
      if (!ImageIO.write(field, GntpMessage.IMAGE_FORMAT, output)) {
        throw new IllegalStateException("Could not read icon data")
      }
      output.toByteArray
    }
  }

}
