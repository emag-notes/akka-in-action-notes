package aia.structure.pipeandfilter

import akka.actor.{Actor, ActorRef}

class SpeedFilter(minSpeed: Int, pipe: ActorRef) extends Actor {
  override def receive: Receive = { case msg: Photo =>
    if (msg.speed > minSpeed) pipe ! msg
  }
}
