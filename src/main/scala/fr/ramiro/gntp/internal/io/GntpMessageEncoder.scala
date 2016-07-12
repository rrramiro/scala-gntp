package fr.ramiro.gntp.internal.io

import java.io._

import fr.ramiro.gntp.internal.message.{ GntpMessage, GntpMessageRequest }
import io.netty.buffer._
import io.netty.channel.ChannelHandler._
import io.netty.channel._
import io.netty.handler.codec.MessageToByteEncoder
import org.slf4j._

@Sharable class GntpMessageEncoder extends MessageToByteEncoder[GntpMessageRequest] {
  val logger: Logger = LoggerFactory.getLogger(classOf[GntpMessageEncoder])

  @throws(classOf[Exception])
  override def encode(ctx: ChannelHandlerContext, msg: GntpMessageRequest, out: ByteBuf): Unit = {
    val outBuffer = new ByteArrayOutputStream
    msg.append(outBuffer)
    if (logger.isDebugEnabled) {
      logger.debug("Sending message\n{}", new String(outBuffer.toByteArray, GntpMessage.ENCODING))
    }
    out.writeBytes(outBuffer.toByteArray)
  }
}
