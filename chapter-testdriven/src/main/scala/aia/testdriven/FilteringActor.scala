package aia.testdriven

import akka.actor.{Actor, ActorRef, Props}

object FilteringActor {
  def props(nextActor: ActorRef, bufferSize: Int): Props = Props(new FilteringActor(nextActor, bufferSize))

  case class Event(id: Long)
}

class FilteringActor(nextActor: ActorRef, bufferSize: Int) extends Actor {
  import FilteringActor._

  var lastMessages: Vector[Event] = Vector[Event]()

  override def receive: Receive = { case msg: Event =>
    if (!lastMessages.contains(msg)) {
      lastMessages = lastMessages :+ msg
      nextActor ! msg
    }
  }
}
