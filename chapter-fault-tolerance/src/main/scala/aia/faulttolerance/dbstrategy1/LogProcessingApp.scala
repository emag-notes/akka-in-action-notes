package aia.faulttolerance.dbstrategy1

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{
  Actor,
  ActorLogging,
  ActorRef,
  ActorSystem,
  OneForOneStrategy,
  PoisonPill,
  Props,
  SupervisorStrategy,
  Terminated
}

import java.io.File
import java.util.UUID

object LogProcessingApp extends App {
  val sources = Vector("file:///source1/", "file:///source2/")
  val system = ActorSystem("logprocessing")

  val databaseUrl = "http://mydatabase1"

  system.actorOf(
    LogProcessingSuperVisor.props(sources, databaseUrl),
    LogProcessingSuperVisor.name
  )
}

object LogProcessingSuperVisor {
  def props(sources: Vector[String], databaseUrl: String): Props = Props(
    new LogProcessingSuperVisor(sources, databaseUrl)
  )
  def name: String = "file-watcher-supervisor"
}

class LogProcessingSuperVisor(sources: Vector[String], databaseUrl: String) extends Actor with ActorLogging {
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: CorruptedFileException      => Resume
    case _: DbBrokenConnectionException => Restart
    case _: DiskError                   => Stop
  }

  var fileWatchers: Seq[ActorRef] = sources.map { source =>
    val dbWriter = context.actorOf(
      DbWriter.props(databaseUrl),
      DbWriter.name(databaseUrl)
    )

    val logProcessor = context.actorOf(
      LogProcessor.props(dbWriter),
      LogProcessor.name
    )

    val fileWatcher = context.actorOf(
      FileWatcher.props(source, logProcessor),
      FileWatcher.name
    )

    context.watch(fileWatcher)
    fileWatcher
  }
  override def receive: Receive = { case Terminated(actorRef) =>
    if (fileWatchers.contains(actorRef)) {
      fileWatchers = fileWatchers.filterNot(_ == actorRef)
      if (fileWatchers.isEmpty) {
        log.info("Shutting down, all file watchers have failed.")
        context.system.terminate()
      }
    }
  }
}

object FileWatcher {
  def props(source: String, logProcessor: ActorRef): Props = Props(new FileWatcher(source, logProcessor))
  def name = s"file-watcher-${UUID.randomUUID.toString}"

  case class NewFile(file: File, timeAdded: Long)
  case class SourceAbandoned(uri: String)
}

class FileWatcher(source: String, logProcessor: ActorRef) extends Actor with FileWatchingAbilities {
  register(source)

  import FileWatcher._

  override def receive: Receive = {
    case NewFile(file, _) =>
      logProcessor ! LogProcessor.LogFile(file)

    case SourceAbandoned(uri) if uri == source =>
      self ! PoisonPill
  }
}

object LogProcessor {
  def props(dbWriter: ActorRef): Props = Props(new LogProcessor(dbWriter))
  def name = s"log_processor_${UUID.randomUUID.toString}"

  case class LogFile(file: File)
}

class LogProcessor(dbWriter: ActorRef) extends Actor with ActorLogging with LogParsing {
  import LogProcessor._
  override def receive: Receive = { case LogFile(file) =>
    val lines: Vector[DbWriter.Line] = parse(file)
    lines.foreach(dbWriter ! _)
  }
}

object DbWriter {
  def props(databaseUrl: String): Props = Props(new DbWriter(databaseUrl))
  def name(databaseUrl: String) = s"""db-writer-${databaseUrl.split("/").last}"""

  case class Line(time: Long, message: String, messageType: String)
}

class DbWriter(databaseUrl: String) extends Actor {
  val connection = new DbCon(databaseUrl)

  import DbWriter._

  override def postStop(): Unit = connection.close()

  override def receive: Receive = { case Line(time, message, messageType) =>
    connection.write(Map(Symbol("time") -> time, Symbol("message") -> message, Symbol("messageType") -> messageType))
  }
}

class DbCon(url: String) {
  def write(map: Map[Symbol, Any]): Unit = {}
  def close(): Unit = {}
}

class DiskError(msg: String) extends Error(msg)
class CorruptedFileException(msg: String, val file: File) extends Exception(msg)
class DbBrokenConnectionException(msg: String) extends Exception(msg)
class DbNodeDownException(msg: String) extends Exception(msg)

trait LogParsing {
  import DbWriter._
  def parse(file: File): Vector[Line] = Vector.empty[Line]
}

trait FileWatchingAbilities {
  def register(uri: String): Unit = {}
}
