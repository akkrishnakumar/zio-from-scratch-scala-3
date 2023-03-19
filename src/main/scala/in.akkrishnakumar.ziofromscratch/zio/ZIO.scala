package zio

import scala.concurrent.ExecutionContext

sealed trait Fiber[+A]:
  def join: ZIO[A]
  def start(): Unit

object Fiber:

  def apply[A](zio: ZIO[A]) = new FiberImpl[A](zio)

  class FiberImpl[A](zio: ZIO[A]) extends Fiber[A]:
    private var maybeResult: Option[A]    = None
    private var callbacks: List[A => Any] = List.empty[A => Any]

    // Join is when we get the value
    override def join: ZIO[A] =
      maybeResult match
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.async(complete => callbacks = complete :: callbacks)

    // start is when we evaluate the value.
    // This is immediate because of the ExecutionContext,
    // which starts a new thread to execute the task.
    override def start(): Unit =
      ExecutionContext.global.execute { () =>
        zio.run { a =>
          maybeResult = Some(a)
          callbacks.foreach(c => c(a))
        }
      }

sealed trait ZIO[+A]:
  def run(callback: A => Unit): Unit

  def flatMap[B](f: A => ZIO[B]): ZIO[B] = ZIO.FlatMap(this, f)
  def map[B](f: A => B): ZIO[B]          = flatMap(a => ZIO.succeed(f(a)))

  def zipPar[B](that: ZIO[B]): ZIO[(A, B)] =
    for
      f1 <- this.fork
      f2 <- that.fork
      a  <- f1.join
      b  <- f2.join
    yield (a, b)

  def zip[B](that: ZIO[B]): ZIO[(A, B)] =
    for
      a <- this
      b <- that
    yield (a, b)

  def fork: ZIO[Fiber[A]] = ZIO.Fork(this)

object ZIO:

  def succeedNow[A](value: A): ZIO[A]               = Succeed(value)
  def succeed[A](value: => A): ZIO[A]               = Effect(() => value)
  def async[A](register: (A => Any) => Any): ZIO[A] = Async(register)

  case class Succeed[A](value: A) extends ZIO[A]:
    override def run(callback: A => Unit): Unit = callback(value)

  case class Effect[A](a: () => A) extends ZIO[A]:
    override def run(callback: A => Unit): Unit = callback(a())

  /* Implemented using FlatMap
  case class Zip[A, B](left: ZIO[A], right: ZIO[B]) extends ZIO[(A, B)]:
    override def run(callback: ((A, B)) => Unit): Unit =
      left.run(a => right.run(b => callback((a, b))))
   */

  /* Implemented using FlatMap
  case class Map[A, B](self: ZIO[A], f: A => B) extends ZIO[B]:
    override def run(callback: B => Unit): Unit =
      self.run(a => callback(f(a)))
   */

  case class FlatMap[A, B](self: ZIO[A], f: A => ZIO[B]) extends ZIO[B]:
    override def run(callback: B => Unit): Unit =
      self.run(a => f(a).run(callback))

  // I don't clearly understand this. The type signature can be a little misleading
  // Check out misleadingZIO as an example.
  case class Async[A](register: (A => Any) => Any) extends ZIO[A]:
    override def run(callback: A => Unit): Unit =
      register(callback)

  case class Fork[A](self: ZIO[A]) extends ZIO[Fiber[A]]:
    override def run(callback: Fiber[A] => Unit): Unit = {
      val f = Fiber[A](self)
      f.start()
      callback(f)
    }
