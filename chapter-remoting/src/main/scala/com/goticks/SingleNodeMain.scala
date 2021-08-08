package com.goticks

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

object SingleNodeMain extends App with Startup {
  val config = ConfigFactory.load("singlenode")

  implicit val system: ActorSystem = ActorSystem("singlenode", config)

  val api = new RestApi() {
    override def log: LoggingAdapter = Logging(system.eventStream, "go-ticks")
    override implicit def requestTimeout: Timeout = configureRequestTimeout(config)
    override def createBoxOffice(): ActorRef = system.actorOf(BoxOffice.props, BoxOffice.name)
  }

  startup(api.routes)
}
