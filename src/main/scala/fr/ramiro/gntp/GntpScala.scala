/*
 * Copyright (C) 2010 Leandro Aparecido <lehphyro@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.ramiro.gntp

import java.net._
import java.util.concurrent._

import fr.ramiro.gntp.internal.io.{ NioTcpGntpClient, RetryParam }
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success, Try }

object GntpScala {
  val CUSTOM_HEADER_PREFIX: String = "X-"
  val APP_SPECIFIC_HEADER_PREFIX: String = "Data-"
  val WINDOWS_TCP_PORT: Int = 23053
  val MAC_TCP_PORT: Int = 23052
  val UDP_PORT: Int = 9887
  val DEFAULT_RETRY_TIME: Long = 3
  val DEFAULT_RETRY_TIME_UNIT: TimeUnit = TimeUnit.SECONDS
  val DEFAULT_NOTIFICATION_RETRIES: Int = 3
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def getTcpPort: Int = {
    val osName = Option(System.getProperty("os.name"))
    if (osName.exists(_.toLowerCase.contains("mac"))) {
      GntpScala.logger.debug("using mac port number: " + GntpScala.MAC_TCP_PORT)
      GntpScala.MAC_TCP_PORT
    } else {
      GntpScala.logger.debug("using the windows port: " + GntpScala.WINDOWS_TCP_PORT)
      GntpScala.WINDOWS_TCP_PORT
    }
  }

  def getInetAddress(hostName: Option[String]): InetAddress = hostName match {
    case Some(name) => Try(InetAddress.getByName(name)) match {
      case Success(inetAddress) => inetAddress
      case Failure(exception) => throw new IllegalStateException("Could not find inet address: " + name, exception)
    }
    case _ => InetAddress.getLocalHost
  }

}

