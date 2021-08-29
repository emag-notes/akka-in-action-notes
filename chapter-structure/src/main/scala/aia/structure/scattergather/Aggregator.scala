package aia.structure.scattergather

import akka.actor.{Actor, ActorRef}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class Aggregator(timeout: FiniteDuration, pipe: ActorRef) extends Actor {
  val messages = new ListBuffer[PhotoMessage]
  implicit val ec: ExecutionContext = context.system.dispatcher

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    messages.foreach(self ! _)
    messages.clear()
  }

  override def receive: Receive = {
    case rcvMsg: PhotoMessage =>
      messages.find(_.id == rcvMsg.id) match {
        case Some(alreadyRcvMsg) =>
          val newCombinedMsg = PhotoMessage(
            id = rcvMsg.id,
            photo = rcvMsg.photo,
            creationTime = rcvMsg.creationTime.orElse(alreadyRcvMsg.creationTime),
            speed = rcvMsg.speed.orElse(alreadyRcvMsg.speed)
          )
          pipe ! newCombinedMsg
          messages -= alreadyRcvMsg
        case None =>
          messages += rcvMsg
          context.system.scheduler.scheduleOnce(
            delay = timeout,
            receiver = self,
            message = TimeoutMessage(rcvMsg)
          )
      }
    case TimeoutMessage(rcvMsg) =>
      messages.find(_.id == rcvMsg.id) match {
        case Some(alreadyRcvMsg) =>
          pipe ! alreadyRcvMsg
          messages -= alreadyRcvMsg
        case None =>
        // message is already processed
      }
    case ex: Exception => throw ex
  }
}
