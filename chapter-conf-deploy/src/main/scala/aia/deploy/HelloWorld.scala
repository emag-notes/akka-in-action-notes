package aia.deploy

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class HelloWorld extends Actor with ActorLogging {
  override def receive: Receive = { case msg: String =>
    val hello = "Hello %s".format(msg)
    sender() ! hello
    log.info("Sent response {}", hello)
  }
}

class HelloWorldCaller(timer: FiniteDuration, actor: ActorRef) extends Actor with ActorLogging {
  case class TimerTick(msg: String)

  override def preStart(): Unit = {
    implicit val ec: ExecutionContext = context.dispatcher
    context.system.scheduler.scheduleWithFixedDelay(
      initialDelay = timer,
      delay = timer,
      receiver = self,
      message = TimerTick("everybody")
    )
  }

  override def receive: Receive = {
    case msg: String     => log.info("received {}", msg)
    case tick: TimerTick => actor ! tick.msg
  }
}
