package aia.faulttolerance.dbstrategy2

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{
  Actor,
  ActorLogging,
  ActorRef,
  ActorSystem,
  AllForOneStrategy,
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

  val databaseUrls = Vector(
    "http://mydatabase1",
    "http://mydatabase2",
    "http://mydatabase3"
  )

  system.actorOf(
    LogProcessingSuperVisor.props(sources, databaseUrls),
    LogProcessingSuperVisor.name
  )
}

object LogProcessingSuperVisor {
  def props(sources: Vector[String], databaseUrls: Vector[String]): Props = Props(
    new LogProcessingSuperVisor(sources, databaseUrls)
  )
  def name: String = "file-watcher-supervisor"
}

class LogProcessingSuperVisor(sources: Vector[String], databaseUrls: Vector[String]) extends Actor with ActorLogging {
  override def supervisorStrategy: SupervisorStrategy = AllForOneStrategy() { case _: DiskError => Stop }

  var fileWatchers: Seq[ActorRef] = sources.map { source =>
    val fileWatcher = context.actorOf(
      FileWatcher.props(source, databaseUrls),
      FileWatcher.name
    )
    context.watch(fileWatcher)
    fileWatcher
  }

  override def receive: Receive = { case Terminated(fileWatcher) =>
    if (fileWatchers.contains(fileWatcher)) {
      fileWatchers = fileWatchers.filterNot(_ == fileWatcher)
      if (fileWatchers.isEmpty) {
        log.info("Shutting down, all file watchers have failed.")
        context.system.terminate()
      }
    }
  }
}

object FileWatcher {
  def props(source: String, databaseUrls: Vector[String]): Props = Props(new FileWatcher(source, databaseUrls))
  def name = s"file-watcher-${UUID.randomUUID.toString}"

  case class NewFile(file: File, timeAdded: Long)
  case class SourceAbandoned(uri: String)
}

class FileWatcher(source: String, databaseUrls: Vector[String])
    extends Actor
    with ActorLogging
    with FileWatchingAbilities {
  register(source)

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() { case _: CorruptedFileException =>
    Resume
  }

  val logProcessor: ActorRef = context.actorOf(
    LogProcessor.props(databaseUrls),
    LogProcessor.name
  )
  context.watch(logProcessor)

  import FileWatcher._

  override def receive: Receive = {
    case NewFile(file, _) =>
      logProcessor ! LogProcessor.LogFile(file)

    case SourceAbandoned(uri) if uri == source =>
      log.info(s"$uri abandoned, stopping file watcher.")
      self ! PoisonPill

    case Terminated(logProcessor) =>
      log.info(s"Log processor terminated, stopping file watcher.")
      self ! PoisonPill
  }
}

object LogProcessor {
  def props(databaseUrls: Vector[String]): Props = Props(new LogProcessor(databaseUrls))
  def name = s"log_processor_${UUID.randomUUID.toString}"

  case class LogFile(file: File)
}

class LogProcessor(databaseUrls: Vector[String]) extends Actor with ActorLogging with LogParsing {
  require(databaseUrls.nonEmpty)

  val initialDatabaseUrl: String = databaseUrls.head
  var alternateDatabases: Seq[String] = databaseUrls.tail

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: DbBrokenConnectionException => Restart
    case _: DbNodeDownException         => Stop
  }

  var dbWriter: ActorRef = context.actorOf(
    DbWriter.props(initialDatabaseUrl),
    DbWriter.name(initialDatabaseUrl)
  )
  context.watch(dbWriter)

  import LogProcessor._

  override def receive: Receive = {
    case LogFile(file) =>
      val lines: Vector[DbWriter.Line] = parse(file)
      lines.foreach(dbWriter ! _)

    case Terminated(_) =>
      if (alternateDatabases.nonEmpty) {
        val newDatabaseUrl = alternateDatabases.head
        alternateDatabases = alternateDatabases.tail
        dbWriter = context.actorOf(
          DbWriter.props(newDatabaseUrl),
          DbWriter.name(newDatabaseUrl)
        )
        context.watch(dbWriter)
      } else {
        log.error("All Db nodes broken, stopping.")
        self ! PoisonPill
      }
  }
}

class DbWatcher(dbWriter: ActorRef) extends Actor with ActorLogging {
  context.watch(dbWriter)

  override def receive: Receive = { case Terminated(actorRef) =>
    log.warning(s"Actor $actorRef terminated")
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
