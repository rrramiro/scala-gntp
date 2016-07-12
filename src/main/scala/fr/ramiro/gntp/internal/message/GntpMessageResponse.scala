package fr.ramiro.gntp.internal.message

import java.util._

import fr.ramiro.gntp.internal.GntpCallbackResult.GntpCallbackResult
import fr.ramiro.gntp.internal.GntpErrorStatus.GntpErrorStatus
import fr.ramiro.gntp.internal.GntpMessageType
import fr.ramiro.gntp.internal.GntpMessageType.GntpMessageType

abstract class GntpMessageResponse(
    val `type`: GntpMessageType,
    val respondingType: GntpMessageType,
    val internalNotificationId: Option[Long]
) extends GntpMessage {

}

class GntpCallbackMessage(
  internalNotificationId: Option[Long],
  val notificationId: Option[String],
  val callbackResult: GntpCallbackResult,
  val context: String,
  val contextType: String,
  val timestamp: Date
) extends GntpMessageResponse(GntpMessageType.CALLBACK, GntpMessageType.NOTIFY, internalNotificationId)

class GntpOkMessage(
  internalNotificationId: Option[Long],
  respondingType: GntpMessageType,
  val notificationId: Option[String]
) extends GntpMessageResponse(GntpMessageType.OK, respondingType, internalNotificationId)

class GntpErrorMessage(
  internalNotificationId: Option[Long],
  respondingType: GntpMessageType,
  val status: GntpErrorStatus,
  val description: String
) extends GntpMessageResponse(GntpMessageType.ERROR, respondingType, internalNotificationId)

