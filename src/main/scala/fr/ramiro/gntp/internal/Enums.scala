package fr.ramiro.gntp.internal

object GntpErrorStatus extends Enumeration {
  type GntpErrorStatus = Value

  val RESERVED = Value(100)
  val TIMED_OUT = Value(200)
  val NETWORK_FAILURE = Value(201)
  val INVALID_REQUEST = Value(300)
  val UNKNOWN_PROTOCOL = Value(301)
  val UNKNOWN_PROTOCOL_VERSION = Value(302)
  val REQUIRED_HEADER_MISSING = Value(303)
  val NOT_AUTHORIZED = Value(400)
  val UNKNOWN_APPLICATION = Value(401)
  val UNKNOWN_NOTIFICATION = Value(402)
  val INTERNAL_SERVER_ERROR = Value(500)

}

object GntpCallbackResult extends Enumeration {
  type GntpCallbackResult = Value
  val CLICK, CLICKED, CLOSE, CLOSED, TIMEOUT, TIMEDOUT = Value

}

object GntpMessageHeader extends Enumeration {

  type GntpMessageHeader = Value
  val HEADER_SPACER = Value("")
  val APPLICATION_NAME = Value("Application-Name")
  val APPLICATION_ICON = Value("Application-Icon")
  val NOTIFICATION_COUNT = Value("Notifications-Count")
  val NOTIFICATION_INTERNAL_ID = Value("X-Data-Internal-Notification-ID")
  val NOTIFICATION_ID = Value("Notification-ID")
  val NOTIFICATION_NAME = Value("Notification-Name")
  val NOTIFICATION_DISPLAY_NAME = Value("Notification-Display-Name")
  val NOTIFICATION_TITLE = Value("Notification-Title")
  val NOTIFICATION_ENABLED = Value("Notification-Enabled")
  val NOTIFICATION_ICON = Value("Notification-Icon")
  val NOTIFICATION_TEXT = Value("Notification-Text")
  val NOTIFICATION_STICKY = Value("Notification-Sticky")
  val NOTIFICATION_PRIORITY = Value("Notification-Priority")
  val NOTIFICATION_COALESCING_ID = Value("Notification-Coalescing-ID")
  val NOTIFICATION_CALLBACK_TARGET = Value("Notification-Callback-Target")
  val NOTIFICATION_CALLBACK_CONTEXT = Value("Notification-Callback-Context")
  val NOTIFICATION_CALLBACK_CONTEXT_TYPE = Value("Notification-Callback-Context-Type")
  val NOTIFICATION_CALLBACK_RESULT = Value("Notification-Callback-Result")
  val NOTIFICATION_CALLBACK_TIMESTAMP = Value("Notification-Callback-Timestamp")
  val RESPONSE_ACTION = Value("Response-Action")
  val ERROR_CODE = Value("Error-Code")
  val ERROR_DESCRIPTION = Value("Error-Description")

}

object Priority extends Enumeration {
  type Priority = Value
  val LOWEST = Value(-2)
  val LOW = Value(-1)
  val NORMAL = Value(0)
  val HIGH = Value(1)
  val HIGHEST = Value(2)
}

object GntpMessageType extends Enumeration {
  type GntpMessageType = Value
  val REGISTER, NOTIFY, OK, CALLBACK, ERROR = Value
}

