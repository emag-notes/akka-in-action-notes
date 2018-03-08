package aia.stream

import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Path}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import akka.{Done, NotUsed}
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ContentNegLogsApi(val logsDir: Path, val maxLine: Int, val maxJsObject: Int)(implicit val ec: ExecutionContext,
                                                                                   val materializer: ActorMaterializer)
    extends EventMarshalling {

  def logFile(id: String) = logsDir.resolve(id)

  val outFlow: Flow[Event, ByteString, NotUsed] = Flow[Event].map { event =>
    ByteString(event.toJson.compactPrint)
  }

  def logFileSource(logId: String) = FileIO.fromPath(logFile(logId))

  def logFileSink(logId: String) = FileIO.toPath(logFile(logId), Set(CREATE, WRITE, APPEND))

  val routes: Route = postRoute ~ getRoute ~ deleteRoute

  implicit val unmarshaller = EventUnmarshaller.create(maxLine, maxJsObject)

  def postRoute = pathPrefix("logs" / Segment) { logId =>
    pathEndOrSingleSlash {
      post {
        entity(as[Source[Event, _]]) { src =>
          onComplete(
            src
              .via(outFlow)
              .toMat(logFileSink(logId))(Keep.right)
              .run()
          ) {
            case Success(IOResult(count, Success(Done))) =>
              complete((StatusCodes.OK, LogReceipt(logId, count)))
            case Success(IOResult(count, Failure(e))) =>
              complete(
                (
                  StatusCodes.BadRequest,
                  ParseError(logId, e.getMessage)
                )
              )
            case Failure(e) =>
              complete(
                (
                  StatusCodes.BadRequest,
                  ParseError(logId, e.getMessage)
                )
              )
          }
        }
      }
    }
  }

  implicit val marshaller = LogEntityMarshaller.create(maxJsObject)

  def getRoute = pathPrefix("logs" / Segment) { logId =>
    pathEndOrSingleSlash {
      get {
        extractRequest { req =>
          if (Files.exists(logFile(logId))) {
            val src = logFileSource(logId)
            complete(Marshal(src).toResponseFor(req))
          } else {
            complete(StatusCodes.NotFound)
          }
        }
      }
    }
  }

  def deleteRoute = pathPrefix("logs" / Segment) { logId =>
    pathEndOrSingleSlash {
      delete {
        if (Files.deleteIfExists(logFile(logId))) complete(StatusCodes.NoContent)
        else complete(StatusCodes.InternalServerError)
      }
    }
  }

}
