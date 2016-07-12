package fr.ramiro.gntp.internal

import fr.ramiro.gntp.GntpPassword
import org.scalatest.FunSuite

class GntpPasswordScalaImplTest extends FunSuite {
  test("KeyGeneration") {
    val password: GntpPassword = new GntpPassword("test") {
      override protected def getSeed: Long = 10000000L
    }
    assert(password.keyHash === "8dea4bdb68ffd8a3d7a5a715acf4092b8a419d43889d5c0898b48bae7cd000854b0966dcf1a1d6bf607727ddf2b4b5dc094b59778bddc0aaaa9d70879a3674ed")
  }
}