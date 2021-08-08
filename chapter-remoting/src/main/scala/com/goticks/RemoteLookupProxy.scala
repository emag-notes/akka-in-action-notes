package com.goticks

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify, ReceiveTimeout, Terminated}

import scala.concurrent.duration.{Duration, DurationInt}

class RemoteLookupProxy(path: String) extends Actor with ActorLogging {

  context.setReceiveTimeout(3.seconds)

  sendIdentityRequest()

  def sendIdentityRequest(): Unit = {
    log.debug("sendIdentityRequest")
    val selection = context.actorSelection(path)
    selection ! Identify(path)
  }

  override def receive: Receive = identity

  def identity: Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      context.setReceiveTimeout(Duration.Undefined)
      log.info(s"switching to active stage with path '$path'")
      context.become(active(actor))
      context.watch(actor)

    case ActorIdentity(`path`, None) =>
      log.error(s"Remote actor with path '$path' is not available.")

    case ReceiveTimeout =>
      log.warning("receive timeout")
      sendIdentityRequest()

    case msg: Any =>
      log.error(s"Ignoring message '$msg', remote actor is not ready yet.")
  }

  def active(actor: ActorRef): Receive = {
    case Terminated(actorRef: ActorRef) =>
      log.info(s"Actor '$actorRef' terminated.")
      log.info("switching to identity stage")
      context.become(identity)
      context.setReceiveTimeout(3.seconds)
      sendIdentityRequest()

    case msg: Any => actor forward msg
  }
}
