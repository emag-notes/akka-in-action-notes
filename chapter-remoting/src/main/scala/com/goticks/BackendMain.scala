package com.goticks

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

object BackendMain extends App with RequestTimeout {
  val config = ConfigFactory.load("backend")
  val system = ActorSystem("backend", config)
  implicit val requestTimeout: Timeout = configureRequestTimeout(config)
  system.actorOf(BoxOffice.props, BoxOffice.name)
}
