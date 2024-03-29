package com.goticks

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, Terminated}
import akka.util.Timeout

import scala.concurrent.duration.{Duration, DurationInt}

object RemoteBoxOfficeForwarder {
  def props(implicit timeout: Timeout): Props = Props(new RemoteBoxOfficeForwarder)
  def name = "forwarder"
}

class RemoteBoxOfficeForwarder(implicit timeout: Timeout) extends Actor with ActorLogging {
  context.setReceiveTimeout(3.seconds)

  deployAndWatch()

  def deployAndWatch(): Unit = {
    val actor = context.actorOf(BoxOffice.props, BoxOffice.name)
    context.watch(actor)
    log.info("switching to maybe active state")
    context.become(maybeActive(actor))
    context.setReceiveTimeout(Duration.Undefined)
  }

  override def receive: Receive = deploying

  def deploying: Receive = {
    case ReceiveTimeout => deployAndWatch()
    case msg: Any       => log.error(s"Ignoring message '$msg', remote actor is not ready yet.")
  }

  def maybeActive(actor: ActorRef): Receive = {
    case Terminated(actorRef) =>
      log.info(s"Actor '$actorRef' terminated.")
      log.info("switching to deploying state")
      context.become(deploying)
      context.setReceiveTimeout(3.seconds)
      deployAndWatch()

    case msg: Any => actor forward msg
  }
}
