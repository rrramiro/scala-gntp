package fr.ramiro.gntp.internal.message.read

import java.text._
import java.util.Date

import fr.ramiro.gntp.internal.GntpCallbackResult.GntpCallbackResult
import fr.ramiro.gntp.internal.GntpErrorStatus.GntpErrorStatus
import fr.ramiro.gntp.internal.GntpMessageHeader.GntpMessageHeader
import fr.ramiro.gntp.internal.GntpMessageType.GntpMessageType
import fr.ramiro.gntp.internal.message.{ GntpCallbackMessage, GntpErrorMessage, GntpMessage, GntpMessageResponse, GntpOkMessage }
import fr.ramiro.gntp.internal.{ GntpCallbackResult, GntpErrorStatus, GntpMessageHeader, GntpMessageType }

import scala.util.{ Failure, Success, Try }

class GntpMessageResponseParser {
  private val dateFormats = Seq(
    GntpMessage.DATE_TIME_FORMAT,
    GntpMessage.DATE_TIME_FORMAT_ALTERNATE,
    GntpMessage.DATE_TIME_FORMAT_GROWL_1_3
  )

  def parse(s: String): GntpMessageResponse = {
    val splitted: Array[String] = s.split(GntpMessage.SEPARATOR) //separatorSplitter.split(s)
    assert(splitted.nonEmpty, "Empty message received from Growl")
    val iter: Iterator[String] = splitted.iterator
    val statusLine: String = iter.next()
    assert(statusLine.startsWith(GntpMessage.PROTOCOL_ID + "/" + GntpMessage.VERSION), "Unknown protocol version")
    val statusLineIterable: Array[String] = statusLine.split(' ') //statusLineSplitter.split(statusLine)
    val messageTypeText: String = statusLineIterable(1).substring(1)
    val messageType: GntpMessageType = GntpMessageType.withName(messageTypeText)
    val headers = new collection.mutable.HashMap[String, String]
    while (iter.hasNext) {
      val splittedHeader: Array[String] = iter.next().split(":", 2)
      headers.put(splittedHeader(0), splittedHeader(1).trim)
    }
    messageType match {
      case GntpMessageType.OK =>
        createOkMessage(headers.toMap)
      case GntpMessageType.CALLBACK =>
        createCallbackMessage(headers.toMap)
      case GntpMessageType.ERROR =>
        createErrorMessage(headers.toMap)
      case _ =>
        throw new IllegalStateException("Unknown response message type: " + messageType)
    }
  }

  private def createOkMessage(headers: Map[String, String]): GntpOkMessage = {
    new GntpOkMessage(
      headers.getNotificationInternalId,
      headers.getRespondingType,
      headers.getNotificationId
    )
  }

  private def createCallbackMessage(headers: Map[String, String]): GntpCallbackMessage = {
    new GntpCallbackMessage(
      headers.getNotificationInternalId,
      headers.getNotificationId,
      headers.getNotificationCallbackResult,
      headers.getNotificationCallbackContext,
      headers.getNotificationCallbackContextType,
      headers.getNotificationCallbackTimestamp
    )
  }

  private def createErrorMessage(headers: Map[String, String]): GntpErrorMessage = {
    new GntpErrorMessage(
      headers.getNotificationInternalId,
      headers.getRespondingType,
      headers.getErrorCode,
      headers.getErrorDescription
    )
  }

  implicit class HeaderMapWrapper(headers: Map[String, String]) {
    private def getRequiredValue(gntpMessageHeader: GntpMessageHeader): String = headers.getOrElse(gntpMessageHeader.toString, throw new RuntimeException(s"Required header ${gntpMessageHeader.toString} not found"))

    def getRespondingType: GntpMessageType = GntpMessageType.withName(getRequiredValue(GntpMessageHeader.RESPONSE_ACTION))

    def getNotificationInternalId: Option[Long] = headers.get(GntpMessageHeader.NOTIFICATION_INTERNAL_ID.toString).map(_.toLong)

    def getNotificationId: Option[String] = headers.get(GntpMessageHeader.NOTIFICATION_ID.toString)

    def getErrorCode: GntpErrorStatus = GntpErrorStatus(getRequiredValue(GntpMessageHeader.ERROR_CODE).toInt)

    def getErrorDescription: String = getRequiredValue(GntpMessageHeader.ERROR_DESCRIPTION)

    def getNotificationCallbackResult: GntpCallbackResult = GntpCallbackResult.withName(getRequiredValue(GntpMessageHeader.NOTIFICATION_CALLBACK_RESULT))

    def getNotificationCallbackContext: String = getRequiredValue(GntpMessageHeader.NOTIFICATION_CALLBACK_CONTEXT)

    def getNotificationCallbackContextType: String = getRequiredValue(GntpMessageHeader.NOTIFICATION_CALLBACK_CONTEXT_TYPE)

    def getNotificationCallbackTimestamp: Date = parseTimestamp(getRequiredValue(GntpMessageHeader.NOTIFICATION_CALLBACK_TIMESTAMP), dateFormats)

    private def parseTimestamp(timestampText: String, dateFormats: Seq[String]): Date = {
      dateFormats match {
        case format :: tail =>
          Try(new SimpleDateFormat(format).parse(timestampText)) match {
            case Failure(e) => parseTimestamp(timestampText, tail)
            case Success(timestamp) => timestamp
          }
        case Nil =>
          throw new RuntimeException("Timestamp Bad Format")
      }
    }
  }

}
