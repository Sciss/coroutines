package org.coroutines.extra



import org.coroutines._
import org.scalatest._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._



class AsyncAwaitTests extends FunSuite with Matchers {
  class TestException extends Throwable

  /** Source: https://git.io/vorXv
   *  The use of Async/Await as opposed to pure futures allows this control flow
   *  to be written more easily.
   *  The execution blocks when awaiting for the result of `f1`. `f2` only blocks
   *  after `AsyncAwait.await(f1)` evaluates to `true`.
   */
  test("simple test") {
    val future = AsyncAwait.async(coroutine { () =>
      val f1 = Future(true)
      val f2 = Future(42)
      if (AsyncAwait.await(f1)) {
        AsyncAwait.await(f2)
      } else {
        0
      }
    })
    assert(Await.result(future, 1 seconds) == 42)
  }

  /** Asynchronous blocks of code can be defined either outside of or within any
   *  part of an `async` block. This allows the user to avoid triggering the
   *  computation of slow futures until it is necessary.
   *  For instance, computation will not begin on `innerFuture` until
   *  `await(trueFuture)` evaluates to true.
   */
  test("nested async blocks") {
    val outerFuture = AsyncAwait.async(coroutine {() =>
      val trueFuture = Future { true }
      if (AsyncAwait.await(trueFuture)) {
        val innerFuture = AsyncAwait.async(coroutine { () =>
          AsyncAwait.await(Future { 100 } )
        })
        AsyncAwait.await(innerFuture)
      } else {
        200
      }
    })
    assert(Await.result(outerFuture, 1 seconds) == 100)
  }

  test("error handling test 1") {
    val errorMessage = "System error!"
    val exception = intercept[RuntimeException] {
      val c = coroutine { () =>
        sys.error(errorMessage)
        AsyncAwait.await(Future("dog"))
      }
      val future = AsyncAwait.async(c)
      val result = Await.result(future, 1 seconds)
    }
    assert(exception.getMessage == errorMessage)
  }

  test("error handling test 2") {
    intercept[TestException] {
      val c = coroutine { () =>
        throw new TestException
        yieldval((Future("god"), new AsyncAwait.Cell[String]))
        AsyncAwait.await(Future("dog"))
      }
      val future = AsyncAwait.async(c)
      Await.result(future, 1 seconds)
    }
  }

  // Source: https://git.io/vowde
  test("uncaught exception within async after await") {
    val future = AsyncAwait.async(coroutine { () =>
      AsyncAwait.await(Future(()))
      throw new TestException
    })
    intercept[TestException] { Await.result(future, 1 seconds) }
  }

  // Source: https://git.io/vowdk
  test("await failing future within async") {
    val base = Future[Int] { throw new TestException }
    val future = AsyncAwait.async(coroutine { () =>
      val x = AsyncAwait.await(base)
      x * 2
    })
    intercept[TestException] { Await.result(future, 1 seconds) }
  }

  // Source: https://git.io/vowdY
  test("await failing future within async after await") {
    val base = Future[Any] { "five!".length }
    val future = AsyncAwait.async(coroutine { () =>
      val a = AsyncAwait.await(base.mapTo[Int])
      val b = AsyncAwait.await(Future { (a * 2).toString }.mapTo[Int])
      val c = AsyncAwait.await(Future { (7 * 2).toString })
      b + "-" + c
    })
    intercept[ClassCastException] {
      Await.result(future, 1 seconds)
    }
  }

  test("nested failing future within async after await") {
    val base = Future[Any] { "five!".length }
    val future = AsyncAwait.async(coroutine { () =>
      val a = AsyncAwait.await(base.mapTo[Int])
      val b = AsyncAwait.await(AsyncAwait.await(Future((Future { (a * 2).toString }).mapTo[Int])))
      val c = AsyncAwait.await(Future { (7 * 2).toString })
      b + "-" + c
    })
    intercept[ClassCastException] {
      Await.result(future, 1 seconds)
    }
  }
}
