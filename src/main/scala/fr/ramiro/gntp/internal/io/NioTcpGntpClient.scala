package fr.ramiro.gntp.internal.io

import java.net._
import java.util.concurrent.atomic.{ AtomicInteger, AtomicLong }
import java.util.concurrent.{ Executor, Executors, ScheduledExecutorService, TimeUnit }

import fr.ramiro.gntp.internal.message.{ GntpMessage, GntpNotifyMessage, GntpRegisterMessage }
import fr.ramiro.gntp.{ GntpApplicationInfo, GntpListener, GntpNotification, GntpPassword }
import io.netty.bootstrap._
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.group._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ AdaptiveRecvByteBufAllocator, _ }
import io.netty.util.concurrent.GlobalEventExecutor
import org.slf4j._

import scala.collection.concurrent.TrieMap

class NioTcpGntpClient(
    applicationInfo: GntpApplicationInfo,
    growlHost: InetAddress,
    growlPort: Int,
    executor: Executor,
    listener: GntpListener,
    password: GntpPassword,
    retryTime: Long = 0L,
    retryTimeUnit: TimeUnit,
    notificationRetryCount: Int = 0
) extends NioGntpClient(applicationInfo, growlHost, growlPort, password) {
  val logger: Logger = LoggerFactory.getLogger(classOf[NioTcpGntpClient])
  assert(executor != null, "Executor must not be null")
  if (retryTime > 0) {
    assert(retryTimeUnit != null, "Retry time unit must not be null")
  }
  assert(notificationRetryCount >= 0, "Notification retries must be equal or greater than zero")

  val group = new NioEventLoopGroup
  private final val bootstrap: Bootstrap = new Bootstrap()

  bootstrap.group(group)
  bootstrap.remoteAddress(new InetSocketAddress(growlHost, growlPort))

  bootstrap.option(ChannelOption.TCP_NODELAY, true.asInstanceOf[java.lang.Boolean])
  bootstrap.option(ChannelOption.SO_TIMEOUT, Integer.valueOf(60 * 1000))
  bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
  bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
  bootstrap.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
  bootstrap.channel(classOf[NioSocketChannel])
  bootstrap.handler(new GntpChannelPipelineFactory(new GntpChannelHandler(this, Option(listener))))

  private final val retryExecutorService: ScheduledExecutorService = if (retryTime > 0) Executors.newSingleThreadScheduledExecutor else null
  private final val channelGroup: ChannelGroup = new DefaultChannelGroup("gntp", GlobalEventExecutor.INSTANCE)

  @volatile
  private var tryingRegistration: Boolean = false
  private final val notificationIdGenerator: AtomicLong = new AtomicLong
  val notificationRetries = new TrieMap[GntpNotification, AtomicInteger]

  protected def doRegister {
    bootstrap.connect(growlHost, growlPort).addListener(new ChannelFutureListener {
      @throws(classOf[Exception])
      def operationComplete(future: ChannelFuture) {
        tryingRegistration = false
        if (future.isSuccess) {
          channelGroup.add(future.channel())
          val message: GntpMessage = new GntpRegisterMessage(applicationInfo, password)
          future.channel().writeAndFlush(message)
        } else {
          future.cause().printStackTrace();
          future.channel().close();
        }
      }
    })
  }

  protected def doNotify(notification: GntpNotification) {
    bootstrap.connect.addListener(new ChannelFutureListener {
      @throws(classOf[Exception])
      def operationComplete(future: ChannelFuture) {
        if (future.isSuccess) {
          channelGroup.add(future.channel)
          val notificationId: Long = notificationIdGenerator.getAndIncrement
          NioGntpClient.notificationsSent.put(notificationId, notification)
          val message: GntpMessage = new GntpNotifyMessage(notification, notificationId, password)
          future.channel.writeAndFlush(message)
        } else {
          if (retryExecutorService != null) {
            var count = notificationRetries(notification)
            if (count == null) {
              count = new AtomicInteger(1)
              notificationRetries.put(notification, count)
            }
            if (count.get() <= notificationRetryCount) {
              logger.debug("Failed to send notification [{}], retry [{}/{}] in [{}-{}]", Array(notification, count, notificationRetryCount, retryTime, retryTimeUnit))
              count.incrementAndGet()
              retryExecutorService.schedule(new Runnable {
                override def run(): Unit = NioTcpGntpClient.this.notify(notification)
              }, retryTime, retryTimeUnit)
            } else {
              logger.debug("Failed to send notification [{}], giving up", notification)
              notificationRetries.remove(notification)
            }
          }
          NioGntpClient.notificationsSent.find(_._2 == notification).foreach(entity =>
            NioGntpClient.notificationsSent.remove(entity._1))
          future.cause().printStackTrace()
          future.channel().close()
        }
      }
    })
  }

  @throws(classOf[InterruptedException])
  protected def doShutdown(timeout: Long, unit: TimeUnit) {
    if (retryExecutorService != null) {
      retryExecutorService.shutdownNow
      retryExecutorService.awaitTermination(timeout, unit)
    }
    channelGroup.close.await(timeout, unit)
  }

  def retryRegistration {
    if (retryExecutorService != null && !tryingRegistration) {
      tryingRegistration = true
      logger.info("Scheduling registration retry in [{}-{}]", retryTime, retryTimeUnit)
      retryExecutorService.schedule(new Runnable {
        override def run() = {
          register
        }
      }, retryTime, retryTimeUnit)
    }
  }
}
