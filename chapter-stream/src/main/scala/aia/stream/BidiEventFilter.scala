package aia.stream

import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import spray.json._

import scala.concurrent.Future

object BidiEventFilter extends App with EventMarshalling {

  val maxLine       = 10240
  val maxJsonObject = 102400

  val inputFile  = FileArg.shellExpanded(args(2))
  val outputFile = FileArg.shellExpanded(args(3))

  val filterState = args(4) match {
    case State(state) => state
    case unknown =>
      System.err.println(s"Unknown state $unknown, exiting.")
      System.exit(1)
  }

  val inFlow: Flow[ByteString, Event, NotUsed] =
    if (args(0).toLowerCase == "json") {
      JsonFraming
        .objectScanner(maxJsonObject)
        .map(_.decodeString(StandardCharsets.UTF_8).parseJson.convertTo[Event])
    } else {
      Framing
        .delimiter(ByteString("\n"), maxLine)
        .map(_.decodeString(StandardCharsets.UTF_8))
        .map(LogStreamProcessor.parseLineEx)
        .collect { case Some(e) => e }
    }

  val outFlow: Flow[Event, ByteString, NotUsed] =
    if (args(1).toLowerCase == "json") {
      Flow[Event].map(event => ByteString(event.toJson.compactPrint))
    } else {
      Flow[Event].map(event => ByteString(LogStreamProcessor.logLine(event)))
    }

  val bidiFlow: BidiFlow[ByteString, Event, Event, ByteString, NotUsed] = BidiFlow.fromFlows(inFlow, outFlow)

  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(inputFile)
  val sink: Sink[ByteString, Future[IOResult]]     = FileIO.toPath(outputFile, Set(CREATE, WRITE, APPEND))

  val filter: Flow[Event, Event, NotUsed] = Flow[Event].filter(_.state == filterState)

  val flow: Flow[ByteString, ByteString, NotUsed] = bidiFlow.join(filter)

  implicit val system       = ActorSystem()
  implicit val ec           = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val runnableGraph: RunnableGraph[Future[IOResult]] = source.via(flow).toMat(sink)(Keep.right)

  runnableGraph.run().foreach { result =>
    println(s"Wrote ${result.count} bytes to '$outputFile")
    system.terminate()
  }
}
