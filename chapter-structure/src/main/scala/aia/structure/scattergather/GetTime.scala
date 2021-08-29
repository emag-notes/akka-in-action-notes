package aia.structure.scattergather

import akka.actor.{Actor, ActorRef}

class GetTime(pipe: ActorRef) extends Actor {
  override def receive: Receive = { case msg: PhotoMessage =>
    pipe ! msg.copy(creationTime = ImageProcessing.getTime(msg.photo))
  }
}
