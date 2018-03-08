package com.goticks

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.{Failure, Success}

object Main extends App with RequestTimeout {

  val config: Config = ConfigFactory.load()
  val host           = config.getString("http.host")
  val port           = config.getInt("http.port")

  implicit val system       = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val api = new RestApi(system, requestTimeout(config)).routes

  val bindingFuture = Http().bindAndHandle(api, host, port)

  val log = Logging(system.eventStream, "go-ticks")
  bindingFuture
    .map { serverBinding =>
      log.info(s"RestApi bound to ${serverBinding.localAddress}")
    }
    .onComplete {
      case Success(_) =>
        log.info("Success to bind to {}:{}", host, port)
      case Failure(ex) =>
        log.error(ex, "Failed to bind to {}:{}!", host, port)
        system.terminate()
    }

}

trait RequestTimeout {
  import scala.concurrent.duration._

  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
