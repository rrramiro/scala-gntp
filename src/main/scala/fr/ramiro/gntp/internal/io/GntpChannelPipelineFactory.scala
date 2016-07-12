package fr.ramiro.gntp.internal.io

import io.netty.buffer.{ ByteBuf, Unpooled }
import io.netty.channel._
import io.netty.channel.socket.SocketChannel

object GntpChannelPipelineFactory {
  def getDelimiter: ByteBuf = Unpooled.wrappedBuffer(Array[Byte]('\r'.toByte, '\n'.toByte, '\r'.toByte, '\n'.toByte))
}

class GntpChannelPipelineFactory(handler: GntpChannelHandler) extends ChannelInitializer[SocketChannel] {
  val messageDecoder = new GntpMessageDecoder
  val messageEncoder = new GntpMessageEncoder

  @Override
  def initChannel(ch: SocketChannel): Unit = {
    val pipeline = ch.pipeline()
    pipeline.addLast("message-decoder", messageDecoder)
    pipeline.addLast("message-encoder", messageEncoder)
    pipeline.addLast("handler", handler)
  }
}
