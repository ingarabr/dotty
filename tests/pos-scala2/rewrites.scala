trait Test {

  def baz() {}

  def bar()

  def foo() {
    println("hi")
  }

  lazy val x: Int
}

object Test {

  lazy val x = 1

  @deprecated lazy val y = 2

  @deprecated private lazy val z = 2

  lazy val (x1, y1) = (1, 2)

  @deprecated private lazy val (x2, y2) = (1, 2)

  val yy = x1 _

}

