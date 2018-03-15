package ala.testdriven

import akka.actor.{ActorSystem, UnhandledMessage}
import akka.testkit.TestKit
import org.scalatest.{MustMatchers, WordSpecLike}

class GreeterSpec
    extends TestKit(ActorSystem("testsystem"))
    with WordSpecLike
    with MustMatchers
    with StopSystemAfterAll {

  // Greeter
  "The Greeter" must {
    // Greeting("World") を送ると [Hello World!] と出力
    "say Hello World! when a Greeting(\"World\") is sent to it" in {
      val props   = Greeter.props(Some(testActor))
      val greeter = system.actorOf(props, "greeter-1")
      greeter ! Greeting("World")
      expectMsg("Hello World!")
    }

    // 何か他のメッセージを送ると何が起こるか
    "say something else and see what happens" in {
      val props   = Greeter.props(Some(testActor))
      val greeter = system.actorOf(props, "greeter-2")
      system.eventStream.subscribe(testActor, classOf[UnhandledMessage])
      greeter ! "World"
      expectMsg(UnhandledMessage("World", system.deadLetters, greeter))
    }

  }
}
