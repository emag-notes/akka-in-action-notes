package aia.stream

import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption._

import aia.stream.LogStreamProcessor.LogParseException
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorAttributes, ActorMaterializer, IOResult, Supervision}
import akka.util.ByteString
import spray.json._

import scala.concurrent.Future

object ResumingEventFilter extends App with EventMarshalling {

  val maxLine    = 10240
  val inputFile  = FileArg.shellExpanded(args(0))
  val outputFile = FileArg.shellExpanded(args(1))

  val filterState = args(2) match {
    case State(state) => state
    case unknown =>
      System.err.println(s"Unknown state $unknown, exiting.")
      System.exit(1)
  }

  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(inputFile)
  val sink: Sink[ByteString, Future[IOResult]]     = FileIO.toPath(outputFile, Set(CREATE, WRITE, APPEND))

  val frame: Flow[ByteString, String, NotUsed] = Framing
    .delimiter(ByteString("\n"), maxLine)
    .map(_.decodeString(StandardCharsets.UTF_8))

  val decider: Supervision.Decider = {
    case _: LogParseException => Supervision.Resume
    case _                    => Supervision.Stop
  }

  val parse: Flow[String, Event, NotUsed] =
    Flow[String]
      .map(LogStreamProcessor.parseLineEx)
      .collect { case Some(e) => e }
      .withAttributes(ActorAttributes.supervisionStrategy(decider))

  val filter: Flow[Event, Event, NotUsed] = Flow[Event].filter(_.state == filterState)

  val serialize: Flow[Event, ByteString, NotUsed] = Flow[Event].map(event => ByteString(event.toJson.compactPrint))

  implicit val system       = ActorSystem()
  implicit val ec           = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val composedFlow: Flow[ByteString, ByteString, NotUsed] =
    frame
      .via(parse)
      .via(filter)
      .via(serialize)

  val runnableGraph: RunnableGraph[Future[IOResult]] = source.via(composedFlow).toMat(sink)(Keep.right)

  runnableGraph.run().foreach { result =>
    println(s"Wrote ${result.count} bytes to '$outputFile")
    system.terminate()
  }
}
