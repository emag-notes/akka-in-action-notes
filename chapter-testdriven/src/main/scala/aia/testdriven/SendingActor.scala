package aia.testdriven

import aia.testdriven.SendingActor.{SortEvents, SortedEvents}
import akka.actor.{Actor, ActorRef, Props}

object SendingActor {
  def props(receiver: ActorRef): Props = Props(new SendingActor(receiver))

  case class Event(id: Long)
  case class SortEvents(unsorted: Vector[SendingActor.Event])
  case class SortedEvents(sorted: Vector[SendingActor.Event])
}

class SendingActor(receiver: ActorRef) extends Actor {
  override def receive: Receive = { case SortEvents(unsorted) =>
    receiver ! SortedEvents(unsorted.sortBy(_.id))
  }
}
