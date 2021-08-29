package aia.structure.scattergather

import akka.actor.{Actor, ActorRef}

class GetSpeed(pipe: ActorRef) extends Actor {
  override def receive: Receive = { case msg: PhotoMessage =>
    pipe ! msg.copy(speed = ImageProcessing.getSpeed(msg.photo))
  }
}
