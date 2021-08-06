package aia.testdriven

import akka.actor.{Actor, ActorLogging}

object Greeter01 {
  case class Greeting(message: String)
}

class Greeter01 extends Actor with ActorLogging {
  import Greeter01._

  override def receive: Receive = { case Greeting(message) =>
    log.info("Hello {}!", message)
  }
}
