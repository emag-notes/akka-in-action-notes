package com.goticks

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

object FrontendRemoteDeployMain extends App with Startup {
  val config = ConfigFactory.load("frontend-remote-deploy")

  implicit val system: ActorSystem = ActorSystem("frontend", config)

  val api: RestApi = new RestApi() {
    override def log: LoggingAdapter = Logging(system.eventStream, "frontend-remote")
    override implicit def requestTimeout: Timeout = configureRequestTimeout(config)
    override def createBoxOffice(): ActorRef = system.actorOf(BoxOffice.props, BoxOffice.name)
  }

  startup(api.routes)
}
