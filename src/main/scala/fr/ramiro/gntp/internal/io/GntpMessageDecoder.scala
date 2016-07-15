package fr.ramiro.gntp.internal.io

import java.util

import fr.ramiro.gntp.internal.message.GntpMessage
import fr.ramiro.gntp.internal.message.read.GntpMessageResponseParser
import io.netty.buffer._
import io.netty.channel.ChannelHandler._
import io.netty.channel._
import io.netty.handler.codec._
import org.slf4j._

@Sharable
class GntpMessageDecoder extends MessageToMessageDecoder[ByteBuf] {
  val logger: Logger = LoggerFactory.getLogger(classOf[GntpMessageDecoder])
  private final val parser: GntpMessageResponseParser = new GntpMessageResponseParser

  @throws(classOf[Exception])
  override def decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: util.List[AnyRef]): Unit = {
    val b = new Array[Byte](buffer.readableBytes)
    buffer.readBytes(b)
    val s = new String(b, GntpMessage.ENCODING)
    if (logger.isDebugEnabled) {
      logger.debug("Message received\n{}", s)
    }
    out.add(parser.parse(s))
  }
}
