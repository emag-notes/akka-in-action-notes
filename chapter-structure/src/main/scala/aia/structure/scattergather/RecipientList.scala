package aia.structure.scattergather

import akka.actor.{Actor, ActorRef}

class RecipientList(recipientList: Seq[ActorRef]) extends Actor {
  override def receive: Receive = { case msg: AnyRef => recipientList.foreach(_ ! msg) }
}
