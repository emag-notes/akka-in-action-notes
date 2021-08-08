package com.goticks

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait Startup extends RequestTimeout {

  def startup(api: Route)(implicit system: ActorSystem): Unit = {
    val host = system.settings.config.getString("http.host")
    val port = system.settings.config.getInt("http.port")
    startHttpServer(api, host, port)
  }

  def startHttpServer(api: Route, host: String, port: Int)(implicit system: ActorSystem): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    val bindingFuture: Future[ServerBinding] = Http().newServerAt(host, port).bind(api)

    val log = Logging(system.eventStream, "go-ticks")
    bindingFuture
      .map { serverBinding =>
        log.info(s"RestApi bound to ${serverBinding.localAddress}")
      }
      .onComplete {
        case Success(_) =>
          log.info(s"Success to bind to $host:$port")
        case Failure(ex) =>
          log.error(ex, s"Failed to bind to $host:$port")
          system.terminate()
      }
  }
}
