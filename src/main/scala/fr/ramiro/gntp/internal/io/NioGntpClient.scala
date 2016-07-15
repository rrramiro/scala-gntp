package fr.ramiro.gntp.internal.io

import java.net._
import java.util.concurrent._

import fr.ramiro.gntp.{ GntpApplicationInfo, GntpClient, GntpNotification, GntpPassword }
import org.slf4j._

import scala.collection.mutable

object NioGntpClient {
  val notificationsSent: mutable.Map[Long, GntpNotification] = new mutable.HashMap[Long, GntpNotification]
}
abstract class NioGntpClient(
    val applicationInfo: GntpApplicationInfo,
    val growlHost: InetAddress,
    val growlPort: Int,
    val password: GntpPassword
) extends GntpClient {
  private val logger: Logger = LoggerFactory.getLogger(classOf[NioGntpClient])
  val registrationLatch: CountDownLatch = new CountDownLatch(1)
  @volatile
  var closed: Boolean = false

  protected def doRegister

  protected def doNotify(notification: GntpNotification)

  @throws(classOf[InterruptedException])
  protected def doShutdown(timeout: Long, unit: TimeUnit)

  def retryRegistration

  def register {
    if (closed) {
      throw new IllegalStateException("GntpClient has been shutdown")
    }
    logger.debug("Registering GNTP application [{}]", applicationInfo)
    doRegister
  }

  def isRegistered: Boolean = {
    val isRegistered: Boolean = registrationLatch.getCount == 0 && !closed
    logger.debug("checking if the [{}] application is registered. Registered = {}", applicationInfo, isRegistered)
    isRegistered
  }

  def notify(notification: GntpNotification) {
    var interrupted: Boolean = false
    var notified = false
    while (!closed && !notified) {
      try {
        registrationLatch.await()
        notifyInternal(notification)
        notified = true
        //break //TODO: break is not supported
      } catch {
        case e: InterruptedException =>
          interrupted = true
      }
    }
    if (interrupted) {
      Thread.currentThread.interrupt()
    }
  }

  @throws(classOf[InterruptedException])
  def notify(notification: GntpNotification, time: Long, unit: TimeUnit): Boolean = {
    if (registrationLatch.await(time, unit)) {
      notifyInternal(notification)
      true
    } else {
      false
    }
  }

  @throws(classOf[InterruptedException])
  def shutdown(timeout: Long, unit: TimeUnit) {
    closed = true
    registrationLatch.countDown()
    doShutdown(timeout, unit)
  }

  protected def notifyInternal(notification: GntpNotification) {
    if (!closed) {
      logger.debug("Sending notification [{}]", notification)
      doNotify(notification)
    }
  }

}
