package fr.ramiro.gntp.internal.io

import java.io._
import java.net._

import fr.ramiro.gntp.internal.message.{ GntpCallbackMessage, GntpErrorMessage, GntpMessageResponse, GntpOkMessage }
import fr.ramiro.gntp.internal.{ GntpCallbackResult, GntpErrorStatus, GntpMessageType }
import fr.ramiro.gntp.{ GntpListener, GntpNotification }
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ SimpleChannelInboundHandler, _ }
import org.slf4j._

@Sharable
class GntpChannelHandler(gntpClient: NioGntpClient, listener: Option[GntpListener]) extends SimpleChannelInboundHandler[GntpMessageResponse](true) { //SimpleChannelUpstreamHandler{//SimpleChannelInboundHandler[GntpMessageResponse] {
  private val logger: Logger = LoggerFactory.getLogger(classOf[GntpChannelHandler])

  @throws(classOf[Exception])
  override def handlerRemoved(ctx: ChannelHandlerContext) {
    logger.trace("Channel closed [{}]", ctx.channel())
  }

  @throws(classOf[Exception])
  override def channelRead0(ctx: ChannelHandlerContext, e: GntpMessageResponse) {
    handleMessage(e)
  }

  @throws(classOf[Exception])
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    try {
      if (gntpClient.isRegistered) {
        handleIOError(cause)
      } else {
        cause match {
          case cause: ConnectException =>
            handleIOError(cause)
            gntpClient.retryRegistration
          case _: IOException =>
            handleMessage(new GntpOkMessage(None, GntpMessageType.REGISTER, null)) // TODO Check
          case theCause =>
            handleIOError(theCause)
        }
      }
    } finally {
      ctx.channel.close
    }
  }

  private def handleMessage(message: GntpMessageResponse) {
    assert(message.isInstanceOf[GntpOkMessage] || message.isInstanceOf[GntpCallbackMessage] || message.isInstanceOf[GntpErrorMessage])
    logger.debug("handling message...")
    if (gntpClient.isRegistered) {
      message.internalNotificationId.flatMap { NioGntpClient.notificationsSent.get }
        .fold(logger.debug("notification is null. Not much we can do now...")) { notification: GntpNotification =>
          message match {
            case _: GntpOkMessage =>
              logger.debug("OK - message.")
              try {
                listener.foreach(_.onNotificationSuccess(notification))
              } finally {
                notification.callbackTarget.foreach { callback =>
                  message.internalNotificationId.foreach { NioGntpClient.notificationsSent.remove }
                }
              }
            case callbackMessage: GntpCallbackMessage =>
              logger.debug("Callback - message.")
              callbackMessage.internalNotificationId.foreach { NioGntpClient.notificationsSent.remove }

              listener.fold(throw new IllegalStateException("A GntpListener must be set in GntpClient to be able to receive callbacks")) { listener =>
                callbackMessage.callbackResult match {
                  case GntpCallbackResult.CLICK | GntpCallbackResult.CLICKED =>
                    listener.onClickCallback(notification)
                  case GntpCallbackResult.CLOSE | GntpCallbackResult.CLOSED =>
                    listener.onCloseCallback(notification)
                  case GntpCallbackResult.TIMEOUT | GntpCallbackResult.TIMEDOUT =>
                    listener.onTimeoutCallback(notification)
                  case _ =>
                    throw new IllegalStateException("Unknown callback result: " + callbackMessage.callbackResult)
                }
              }
            case errorMessage: GntpErrorMessage =>
              logger.debug("ERROR - message.")
              listener.foreach(_.onNotificationError(notification, errorMessage.status, errorMessage.description))
              if (GntpErrorStatus.UNKNOWN_APPLICATION == errorMessage.status || GntpErrorStatus.UNKNOWN_NOTIFICATION == errorMessage.status) {
                gntpClient.retryRegistration
              }
            case _ =>
              logger.warn("Unknown message type. [{}]", message)
          }
        }
    } else {
      logger.debug("application not registered. Not much we can do.")
      message match {
        case _: GntpOkMessage =>
          try {
            listener.foreach(_.onRegistrationSuccess)
          } finally {
            gntpClient.registrationLatch.countDown()
          }
        case errorMessage: GntpErrorMessage =>
          listener.foreach(_.onRegistrationError(errorMessage.status, errorMessage.description))
          if (GntpErrorStatus.NOT_AUTHORIZED eq errorMessage.status) {
            gntpClient.retryRegistration
          }
        case _ =>
      }
    }
  }

  private def handleIOError(t: Throwable) {
    listener.fold(logger.error("Error in GNTP I/O operation", t))(_.onCommunicationError(t))
  }
}
