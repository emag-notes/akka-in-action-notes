package aia.structure.pipeandfilter

import akka.actor.{Actor, ActorRef}

class LicenseFilter(pipe: ActorRef) extends Actor {
  override def receive: Receive = { case msg: Photo =>
    if (msg.license.nonEmpty) pipe ! msg
  }
}
