package fr.ramiro.gntp

import java.util.concurrent.TimeUnit

trait GntpClient {
  /**
   * Send registration request asynchronously.
   */
  def register

  /**
   * @return True if this client is registered.
   */
  def isRegistered: Boolean

  /**
   * Send the given notification waiting uninterruptbly if this client is not
   * registered yet. Wait until this client is registered or is shutdown.
   *
   * @param notification Notification to send.
   */
  def notify(notification: GntpNotification)

  /**
   * Send the given notification waiting at most the given time if this client
   * is not registered yet.
   *
   * @param notification Notification to send.
   * @param time The maximum time to wait.
   * @param unit The time unit of the { @code time} argument.
   * @return True if this client sent the notification successfully before the waiting
   *         time elapsed, false otherwise.
   * @throws InterruptedException If the current thread is interrupted.
   */
  @throws(classOf[InterruptedException])
  def notify(notification: GntpNotification, time: Long, unit: TimeUnit): Boolean

  /**
   * Shutdown this client waiting at most the given time.
   *
   * @param time The maximum time to wait for shutdown.
   * @param unit The time unit of the { @code time} argument.
   * @throws InterruptedException If the current thread is interrupted.
   */
  @throws(classOf[InterruptedException])
  def shutdown(time: Long, unit: TimeUnit)

}
