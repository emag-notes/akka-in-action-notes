package aia.testdriven

import akka.actor.Actor

class EchoActor extends Actor {
  override def receive: Receive = { case msg =>
    sender() ! msg
  }
}
