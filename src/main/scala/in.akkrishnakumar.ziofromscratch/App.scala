import zio.*

case class Person(name: String, age: Int)

object Person:
  val akhil: Person = Person("Akhil", 30)

trait ZIOApp:
  def run: ZIO[Any]
  def main(a: Array[String]) =
    println("=" * 100)
    run.run(r => println(s"Result: $r"))
    Thread.sleep(3000) // Waiting for the async operations to complete before the main thread exits.
    println("=" * 100)

object succeedNow extends ZIOApp:
  override def run: ZIO[Any] = ZIO.succeedNow(Person.akhil)

/*
  Greet will be triggered during the creation of this Object.
  That is basically a side effect.
  So we need to find a way to avoid it.
 */
object succeedNowUhOh extends ZIOApp:
  val greet                  = ZIO.succeedNow(println("Greeting!"))
  override def run: ZIO[Any] = ZIO.succeedNow(42)

/*
  Using ZIO.Succeed will avoid having pre mature calls.
  It will help avoid side effects
 */
object succeed extends ZIOApp:
  val greet                  = ZIO.succeed(println("This should not be called !!"))
  override def run: ZIO[Any] = ZIO.succeedNow(42)

/*
  Now we can create out own version of "println" which returns a ZIO
 */
object succeedAgain extends ZIOApp:
  override def run: ZIO[Any] = printLine("This is printed through ZIO")

/*
  We want a method to combine 2 ZIOs
  For that we will create a type called Zip
 */
object zip extends ZIOApp:
  override def run: ZIO[Any] = zippedZIO

/*
  But now you would also like to transform the value within a ZIO
  In comes the "map" method which can help in transformations
 */
object map extends ZIOApp:
  override def run: ZIO[Any] = personZIO

object mapUhOh extends ZIOApp:
  override def run: ZIO[Any] =
    zippedZIO.map(t => printLine(s"The result will be a type of t: $t"))

object flatMap extends ZIOApp:
  override def run: ZIO[Any] =
    zippedZIO.flatMap(t => printLine(s"The result will be the value of t: $t"))

object forComprehension extends ZIOApp:
  override def run: ZIO[Any] =
    for
      t <- zippedZIO
      _ <- printLine(s"Result printed by For Comprehension: $t")
    yield "Great!!!"

object async extends ZIOApp:
  override def run: ZIO[Any] = asyncZIO

object fork extends ZIOApp:
  override def run: ZIO[Any] =
    for
      fiber1 <- asyncZIO.fork
      fiber2 <- asyncZIO.fork
      _      <- printLine("forking......")
      str1   <- fiber1.join
      str2   <- fiber2.join
    yield s"From forking is : $str1, $str2"

object zipPar extends ZIOApp:
  override def run: ZIO[Any] = asyncZIO zipPar asyncZIO

object misleadingZIO extends ZIOApp:
  override def run: ZIO[Any] = misleadingAsyncZIO

// Some reusable variables and functions used in various examples.

val zippedZIO: ZIO[(String, Int)] = ZIO.succeed("Akhil") zip ZIO.succeed(30)
val personZIO: ZIO[Person]        = zippedZIO map (Person(_, _))

val asyncZIO: ZIO[String] =
  ZIO.async[String] { complete =>
    println("Something async is happening...")
    Thread.sleep(2000)
    complete("Akhil")
  }

// It says that this val is of type ZIO[String],
// but I don't call the complete.
// It's only that complete can only be called using a String value.
val misleadingAsyncZIO: ZIO[String] =
  ZIO.async { complete => () } // the complete callback was completely avoided.

def printLine[A](value: A): ZIO[Unit] = ZIO.succeed(println(value)) // Side effecting function
