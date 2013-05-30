package kamon.instrumentation

import scala.concurrent.{Await, Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.{OptionValues, WordSpec}
import org.scalatest.matchers.MustMatchers
import org.scalatest.concurrent.PatienceConfiguration
import kamon.TraceContext
import java.util.UUID
import scala.util.Success
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit


class FutureInstrumentationSpec extends WordSpec with MustMatchers with ScalaFutures with PatienceConfiguration with OptionValues {

  "a instrumented Future" when {
    "created in a thread that does have a TraceContext" must {
      "preserve the TraceContext" which {
        "should be available during the body's execution" in { new FutureWithContext {

            whenReady(futureWithContext) { result =>
              result.value must be === testContext
            }
        }}

        "should be available during the execution of onComplete callbacks" in { new FutureWithContext {
            val onCompleteContext = Promise[TraceContext]()

            futureWithContext.onComplete({
              case _ => onCompleteContext.complete(Success(TraceContext.current.get))
            })

            whenReady(onCompleteContext.future) { result =>
              result must be === testContext
            }
        }}
      }
    }

    "created in a thread that doest have a TraceContext" must {
      "not capture any TraceContext for the body execution" in { new FutureWithoutContext{

          whenReady(futureWithoutContext) { result =>
            result must be === None
          }
      }}

      "not make any TraceContext available during the onComplete callback" in { new FutureWithoutContext {
        val onCompleteContext = Promise[Option[TraceContext]]()

        futureWithoutContext.onComplete({
          case _ => onCompleteContext.complete(Success(TraceContext.current))
        })

        whenReady(onCompleteContext.future) { result =>
          result must be === None
        }
      }}
    }
  }




  trait FutureWithContext {
    val testContext = TraceContext(UUID.randomUUID(), Nil)
    TraceContext.set(testContext)

    val futureWithContext = Future { TraceContext.current }
  }

  trait FutureWithoutContext {
    TraceContext.clear // Make sure no TraceContext is available
    val futureWithoutContext = Future { TraceContext.current }
  }
}


