package ala.testdriven

import akka.actor.{Actor, ActorRef}

object SilentActor {
  case class SilentMessage(data: String)
  case class GetState(receiver: ActorRef)
}

class SilentActor extends Actor {
  import SilentActor._

  var internalState = Vector.empty[String]

  override def receive: Receive = {
    case SilentMessage(data) =>
      internalState = internalState :+ data
    case GetState(receiver) =>
      receiver ! internalState
  }

  def state: Vector[String] = internalState
}
