package aia.testdriven

import aia.testdriven.Greeter02.Greeting
import akka.actor.{ActorSystem, UnhandledMessage}
import akka.testkit.TestKit
import org.scalatest.wordspec.AnyWordSpecLike

class Greeter02Test extends TestKit(ActorSystem("testSystem")) with AnyWordSpecLike with StopSystemAfterAll {
  "The Greeter" must {
    "say Hello World! when a Greeting(\"World\") is sent to it" in {
      val props = Greeter02.props(Some(testActor))
      val greeter = system.actorOf(props, "greeter02-1")
      greeter ! Greeting("World")
      expectMsg("Hello World!")
    }
    "say something else and see what happens" in {
      val props = Greeter02.props(Some(testActor))
      val greeter = system.actorOf(props, "greeter02-2")
      system.eventStream.subscribe(testActor, classOf[UnhandledMessage])
      greeter ! "World"
      expectMsg(UnhandledMessage("World", system.deadLetters, greeter))
    }
  }
}
