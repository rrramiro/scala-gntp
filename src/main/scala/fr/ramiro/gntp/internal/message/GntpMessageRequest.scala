package fr.ramiro.gntp.internal.message

import java.io.{ ByteArrayOutputStream, OutputStream, OutputStreamWriter }

import fr.ramiro.gntp.internal.GntpMessageType.GntpMessageType
import fr.ramiro.gntp.internal.{ GntpMessageHeader, GntpMessageType }
import fr.ramiro.gntp.{ GntpApplicationInfo, GntpNotification, GntpPassword }

import scala.language.implicitConversions

abstract class GntpMessageRequest(val `type`: GntpMessageType, password: GntpPassword) extends GntpMessage {
  val allHeaders: Seq[(String, HeaderObject)]

  def append(output: OutputStream) {
    output.write({
      s"${GntpMessage.PROTOCOL_ID}/${GntpMessage.VERSION} ${`type`.toString} ${password.getEncryptionSpec}" + (
        if (password.encrypted)
          s" ${password.keyHashAlgorithm}:${password.keyHash}.${password.salt}${GntpMessage.SEPARATOR}"
        else
          GntpMessage.SEPARATOR
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

}

class GntpNotifyMessage(notification: GntpNotification, notificationId: Long, password: GntpPassword) extends GntpMessageRequest(GntpMessageType.NOTIFY, password) {
  val allHeaders: Seq[(String, HeaderObject)] = Seq(
    GntpMessageHeader.APPLICATION_NAME.toString -> (notification.applicationName: HeaderObject),
    GntpMessageHeader.NOTIFICATION_NAME.toString -> (notification.name: HeaderObject),
    GntpMessageHeader.NOTIFICATION_TITLE.toString -> (notification.title: HeaderObject)
  ) union notification.id.fold {
      GntpMessageHeader.NOTIFICATION_ID.toString -> (notificationId: HeaderObject)
    } { notificationIdVal =>
      GntpMessageHeader.NOTIFICATION_ID.toString -> (notificationIdVal: HeaderObject)
    } +: notification.text.map { notificationText =>
      GntpMessageHeader.NOTIFICATION_TEXT.toString -> (notificationText: HeaderObject)
    }.toSeq union notification.sticky.map { notificationSticky =>
      GntpMessageHeader.NOTIFICATION_STICKY.toString -> (notificationSticky: HeaderObject)
    }.toSeq union notification.priority.map { notificationPriority =>
      GntpMessageHeader.NOTIFICATION_PRIORITY.toString -> (notification.priority.get.id.toString: HeaderObject)
    }.toSeq union notification.icon.map {
      case Left(uri) =>
        GntpMessageHeader.NOTIFICATION_ICON.toString -> (uri: HeaderObject)
      case Right(image) =>
        GntpMessageHeader.NOTIFICATION_ICON.toString -> (image: HeaderObject)
    }.toSeq union notification.coalescingId.map { notificationCoalescingId =>
      GntpMessageHeader.NOTIFICATION_COALESCING_ID.toString -> (notificationCoalescingId: HeaderObject)
    }.toSeq union notification.callbackTarget.fold {
      Seq(
        GntpMessageHeader.NOTIFICATION_CALLBACK_CONTEXT.toString -> (notificationId: HeaderObject),
        GntpMessageHeader.NOTIFICATION_CALLBACK_CONTEXT_TYPE.toString -> ("int": HeaderObject)
      )
    } { callbackTarget =>
      Seq(GntpMessageHeader.NOTIFICATION_CALLBACK_TARGET.toString -> callbackTarget)
    } :+ (
      GntpMessageHeader.NOTIFICATION_INTERNAL_ID.toString -> (notificationId: HeaderObject)
    ) union notification.headers :+ ("" -> HeaderSpacer)

}

class GntpRegisterMessage(applicationInfo: GntpApplicationInfo, password: GntpPassword) extends GntpMessageRequest(GntpMessageType.REGISTER, password) {
  val allHeaders: Seq[(String, HeaderObject)] = Seq(
    GntpMessageHeader.APPLICATION_NAME.toString -> (applicationInfo.name: HeaderObject)
  ) union applicationInfo.icon.map {
      case Left(uri) =>
        GntpMessageHeader.APPLICATION_ICON.toString -> (uri: HeaderObject)
      case Right(image) =>
        GntpMessageHeader.APPLICATION_ICON.toString -> (image: HeaderObject)
    }.toSeq union Seq(
      GntpMessageHeader.NOTIFICATION_COUNT.toString -> (applicationInfo.notificationInfos.size: HeaderObject),
      "" -> HeaderSpacer
    ) union (for (notificationInfo <- applicationInfo.notificationInfos) yield {
        Seq(
          GntpMessageHeader.NOTIFICATION_NAME.toString -> (notificationInfo.name: HeaderObject)
        ) union notificationInfo.displayName.map { notificationInfoDisplayName =>
            GntpMessageHeader.NOTIFICATION_DISPLAY_NAME.toString -> (notificationInfoDisplayName: HeaderObject)
          }.toSeq union notificationInfo.icon.map {
            case Left(uri) =>
              GntpMessageHeader.NOTIFICATION_ICON.toString -> (uri: HeaderObject)
            case Right(image) =>
              GntpMessageHeader.NOTIFICATION_ICON.toString -> (image: HeaderObject)
          }.toSeq union Seq(
            GntpMessageHeader.NOTIFICATION_ENABLED.toString -> (notificationInfo.enabled: HeaderObject),
            "" -> HeaderSpacer
          )
      }).flatten

}

