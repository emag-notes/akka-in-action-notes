package com.goticks

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

object FrontendMain extends App with Startup {
  val config = ConfigFactory.load("frontend")

  implicit val system: ActorSystem = ActorSystem("frontend", config)

  val api: RestApi = new RestApi() {
    override def log: LoggingAdapter = Logging(system.eventStream, "go-ticks")
    override implicit def requestTimeout: Timeout = configureRequestTimeout(config)

    def createPath: String = {
      val backendConfig = config.getConfig("backend")
      val host = backendConfig.getString("host")
      val port = backendConfig.getInt("port")
      val protocol = backendConfig.getString("protocol")
      val systemName = backendConfig.getString("system")
      val actorName = backendConfig.getString("actor")
      s"$protocol://$systemName@$host:$port/$actorName"
    }

    val proxy: ActorRef = system.actorOf(Props(new RemoteLookupProxy(createPath)), "lookupBoxOffice")

    override def createBoxOffice(): ActorRef = proxy
  }

  startup(api.routes)
}
