package ala.testdriven

import akka.actor.{Actor, ActorRef, Props}

object SendingActor {
  def props(receiver: ActorRef) =
    Props(new SendingActor(receiver))

  case class Event(id: Long)
  case class SortEvents(unsorted: Vector[Event])
  case class SortedEvents(unsorted: Vector[Event])
}

class SendingActor(receiver: ActorRef) extends Actor {
  import SendingActor._

  override def receive: Receive = {
    case SortEvents(unsorted) =>
      receiver ! SortedEvents(unsorted.sortBy(_.id))
  }
}
