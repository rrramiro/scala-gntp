package fr.ramiro.gntp

import java.awt.image.RenderedImage
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security._
import javax.crypto.spec.{ DESKeySpec, IvParameterSpec }
import javax.crypto.{ Cipher, SecretKeyFactory }

import fr.ramiro.gntp.internal.Priority.Priority
import fr.ramiro.gntp.internal.message.HeaderObject
import fr.ramiro.gntp.util.Hex

case class GntpApplicationInfo(
  name: String,
  icon: Option[Either[URI, RenderedImage]],
  notificationInfos: Seq[GntpNotificationInfo]
)

case class GntpNotificationInfo(
  name: String,
  icon: Option[Either[URI, RenderedImage]],
  displayName: Option[String] = None,
  enabled: Boolean = true
)

case class GntpNotification(
  applicationName: String,
  name: String,
  title: String,
  text: Option[String],
  callbackTarget: Option[URI] = None,
  icon: Option[Either[URI, RenderedImage]] = None,
  id: Option[String] = None,
  sticky: Option[Boolean] = None,
  priority: Option[Priority] = None,
  coalescingId: Option[String] = None,
  headers: Seq[(String, HeaderObject)]
)

object GntpPassword {
  val DEFAULT_RANDOM_SALT_ALGORITHM: String = "SHA1PRNG"
  val DEFAULT_SALT_SIZE: Int = 16
  val DEFAULT_KEY_HASH_ALGORITHM: String = "SHA-512"
  val DEFAULT_ALGORITHM: String = "DES"
  val DEFAULT_TRANSFORMATION: String = "DES/CBC/PKCS5Padding"
  val NONE_ENCRYPTION_ALGORITHM: String = "NONE"
  val BINARY_HASH_FUNCTION: String = "MD5"
}

case class GntpPassword(
    textPassword: String = "",
    encrypted: Boolean = false,
    hashAlgorithm: String = GntpPassword.DEFAULT_KEY_HASH_ALGORITHM,
    randomSaltAlgorithm: String = GntpPassword.DEFAULT_RANDOM_SALT_ALGORITHM
) {
  private val _salt: Seq[Byte] = getSalt
  private val _key = hash(textPassword.getBytes(StandardCharsets.UTF_8).toSeq ++ _salt)
  private val _secretKey = SecretKeyFactory.getInstance(GntpPassword.DEFAULT_ALGORITHM).generateSecret(new DESKeySpec(_key.toArray))
  val salt: String = Hex.toHexadecimal(_salt.toArray)
  val keyHash: String = Hex.toHexadecimal(hash(_key).toArray)
  val keyHashAlgorithm: String = hashAlgorithm.replaceAll("-", "")
  private val _iv = new IvParameterSpec(_secretKey.getEncoded)
  private val _cipher = Cipher.getInstance(GntpPassword.DEFAULT_TRANSFORMATION)
  _cipher.init(Cipher.ENCRYPT_MODE, _secretKey, _iv)

  protected def getSeed: Long = System.currentTimeMillis()

  def encrypt(in: Array[Byte]): Array[Byte] = if (encrypted) _cipher.doFinal(in) else in

  def getEncryptionSpec: String = if (encrypted) GntpPassword.DEFAULT_ALGORITHM + ':' + Hex.toHexadecimal(_iv.getIV) else GntpPassword.NONE_ENCRYPTION_ALGORITHM

  private def getSalt: Seq[Byte] = {
    val random = SecureRandom.getInstance(randomSaltAlgorithm)
    random.setSeed(getSeed)
    val saltArray: Array[Byte] = new Array[Byte](GntpPassword.DEFAULT_SALT_SIZE)
    random.nextBytes(saltArray)
    saltArray.toSeq
  }

  private def hash(keyToUse: Seq[Byte]): Seq[Byte] = {
    MessageDigest.getInstance(hashAlgorithm).digest(keyToUse.toArray).toSeq
  }
}

