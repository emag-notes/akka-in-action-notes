package aia.testdriven

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class EchoActorTest
    extends TestKit(ActorSystem("testsystem"))
    with AnyWordSpecLike
    with Matchers
    with ImplicitSender
    with StopSystemAfterAll {

  "An EchoActor" must {
    "Reply with the same message it receives" in {
      import akka.pattern.ask

      import scala.concurrent.duration._
      implicit val timeout: Timeout = Timeout(3.seconds)
      implicit val ec: ExecutionContext = system.dispatcher

      val echo = system.actorOf(Props[EchoActor])
      val future = echo.ask("some message")
      future.onComplete {
        case Failure(err) => fail(s"MUST NOT be failed, bur $err")
        case Success(msg) => msg must be("some message")
      }

      Await.ready(future, timeout.duration)
    }

    "Reply with the same message it receives without ask" in {
      val echo = system.actorOf(Props[EchoActor])
      echo ! "some message"
      expectMsg("some message")
    }
  }
}
