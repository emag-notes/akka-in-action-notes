package aia.testdriven

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Greeter02 {
  def props(listener: Option[ActorRef] = None) = Props(new Greeter02(listener))

  case class Greeting(message: String)
}

class Greeter02(listener: Option[ActorRef]) extends Actor with ActorLogging {

  import Greeter02._

  override def receive: Receive = { case Greeting(who) =>
    val message = s"Hello $who!"
    log.info(message)
    listener.foreach(_ ! message)
  }
}
