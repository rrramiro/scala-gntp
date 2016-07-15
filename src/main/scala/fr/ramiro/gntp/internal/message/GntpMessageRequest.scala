package fr.ramiro.gntp.internal.message

import java.awt.image.RenderedImage
import java.io.{ ByteArrayOutputStream, OutputStream, OutputStreamWriter }
import java.net.URI

import fr.ramiro.gntp.internal.GntpMessageHeader._
import fr.ramiro.gntp.internal.GntpMessageType.GntpMessageType
import fr.ramiro.gntp.internal.{ GntpMessageHeader, GntpMessageType }
import fr.ramiro.gntp.{ GntpApplicationInfo, GntpNotification, GntpPassword }

import scala.language.implicitConversions

abstract class GntpMessageRequest(val `type`: GntpMessageType, password: GntpPassword) extends GntpMessage {

  val allHeaders: Seq[(String, HeaderObject)]

  def append(output: OutputStream) {
    output.write({
      s"${GntpMessage.PROTOCOL_ID}/${GntpMessage.VERSION} ${`type`.toString} ${password.getEncryptionSpec}" + (
        if (password.encrypted) {
          s" ${password.keyHashAlgorithm}:${password.keyHash}.${password.salt}${GntpMessage.SEPARATOR}"
        } else {
          GntpMessage.SEPARATOR
        }
      )
    }.getBytes(GntpMessage.ENCODING))
    writeHeaders(allHeaders, output)
    appendBinarySections(allHeaders, output)
  }

  def appendBinarySections(allHeaders: Seq[(String, HeaderObject)], output: OutputStream) {
    val binarySections = allHeaders.collect {
      case (key, value: BinaryHeaderValue) =>
        value.binarySection
    }
    val iter = binarySections.iterator
    while (iter.hasNext) {
      val binarySection: BinarySection = iter.next()
      val data = password.encrypt(binarySection.data)
      output.write(s"${GntpMessage.BINARY_SECTION_ID} ${binarySection.id}${GntpMessage.SEPARATOR}${GntpMessage.BINARY_SECTION_LENGTH} ${data.size.toString}${GntpMessage.SEPARATOR}${GntpMessage.SEPARATOR}".getBytes(GntpMessage.ENCODING))
      output.flush()
      output.write(data)
      if (iter.hasNext) {
        output.write(s"${GntpMessage.SEPARATOR}${GntpMessage.SEPARATOR}".getBytes(GntpMessage.ENCODING))
      }
    }
  }

  def writeHeaders(allHeaders: Seq[(String, HeaderObject)], output: OutputStream): Unit = {
    val buffer = new ByteArrayOutputStream()
    val writerTmp = new OutputStreamWriter(buffer, GntpMessage.ENCODING)
    allHeaders.foreach {
      case (_, HeaderSpacer) =>
        writerTmp.append(GntpMessage.SEPARATOR)
      case (name, valueInternal: HeaderObject) =>
        writerTmp.write(s"${name}${GntpMessage.HEADER_SEPARATOR} ${valueInternal.toHeader}")
        writerTmp.append(GntpMessage.SEPARATOR)
    }
    writerTmp.flush()
    val headerData: Array[Byte] = buffer.toByteArray

    output.flush()
    output.write(password.encrypt(headerData))
    output.write(s"${GntpMessage.SEPARATOR}${GntpMessage.SEPARATOR}".getBytes(GntpMessage.ENCODING))
  }

  def imageValue(value: Either[URI, RenderedImage]): HeaderObject = value match {
    case Left(uri) => uri
    case Right(image) => image
  }
}

import GntpMessageHeader._

class GntpNotifyMessage(
    notification: GntpNotification,
    notificationId: Long,
    password: GntpPassword
) extends GntpMessageRequest(GntpMessageType.NOTIFY, password) {

  val allHeaders: Seq[(String, HeaderObject)] = (Seq(
    APPLICATION_NAME -> (notification.applicationName: HeaderObject),
    NOTIFICATION_NAME -> (notification.name: HeaderObject),
    NOTIFICATION_TITLE -> (notification.title: HeaderObject)
  ) ++ Seq(notification.id.fold {
      NOTIFICATION_ID -> (notificationId: HeaderObject)
    } { notificationIdVal =>
      NOTIFICATION_ID -> (notificationIdVal: HeaderObject)
    }) ++ notification.text.map { notificationText =>
      NOTIFICATION_TEXT -> (notificationText: HeaderObject)
    } ++ notification.sticky.map { notificationSticky =>
      NOTIFICATION_STICKY -> (notificationSticky: HeaderObject)
    } ++ notification.priority.map { notificationPriority =>
      NOTIFICATION_PRIORITY -> (notification.priority.get.id.toString: HeaderObject)
    } ++ notification.icon.map { icon =>
      APPLICATION_ICON -> imageValue(icon)
    } ++ notification.coalescingId.map { notificationCoalescingId =>
      NOTIFICATION_COALESCING_ID -> (notificationCoalescingId: HeaderObject)
    } ++ notification.callbackTarget.fold {
      Seq(
        NOTIFICATION_CALLBACK_CONTEXT -> (notificationId: HeaderObject),
        NOTIFICATION_CALLBACK_CONTEXT_TYPE -> ("int": HeaderObject)
      )
    } { callbackTarget =>
      Seq(NOTIFICATION_CALLBACK_TARGET -> callbackTarget)
    } ++ Seq(
      NOTIFICATION_INTERNAL_ID -> (notificationId: HeaderObject)
    )).map { case (key, value) => key.toString -> value } ++ notification.headers :+ (HEADER_SPACER.toString -> HeaderSpacer)

}

class GntpRegisterMessage(applicationInfo: GntpApplicationInfo, password: GntpPassword) extends GntpMessageRequest(GntpMessageType.REGISTER, password) {
  val allHeaders: Seq[(String, HeaderObject)] = (Seq(
    APPLICATION_NAME -> (applicationInfo.name: HeaderObject)
  ) ++ applicationInfo.icon.map { icon =>
      APPLICATION_ICON -> imageValue(icon)
    } ++ Seq(
      NOTIFICATION_COUNT -> (applicationInfo.notificationInfos.size: HeaderObject),
      HEADER_SPACER -> HeaderSpacer
    ) ++ applicationInfo.notificationInfos.flatMap { notificationInfo =>
        Seq(
          NOTIFICATION_NAME -> (notificationInfo.name: HeaderObject)
        ) ++ notificationInfo.displayName.map { notificationInfoDisplayName =>
            NOTIFICATION_DISPLAY_NAME -> (notificationInfoDisplayName: HeaderObject)
          } ++ notificationInfo.icon.map { icon =>
            APPLICATION_ICON -> imageValue(icon)
          } ++ Seq(
            NOTIFICATION_ENABLED -> (notificationInfo.enabled: HeaderObject),
            HEADER_SPACER -> HeaderSpacer
          )
      }).map { case (key, value) => key.toString -> value }

}

