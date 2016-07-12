package fr.ramiro.gntp.internal

import org.scalatest.FunSuite

class PriorityTest extends FunSuite {
  test("GetCode") {
    assert(-2 === Priority.LOWEST.id)
    assert(2 === Priority.HIGHEST.id)
  }
}
