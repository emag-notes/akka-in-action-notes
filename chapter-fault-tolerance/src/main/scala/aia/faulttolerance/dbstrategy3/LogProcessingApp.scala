package aia.faulttolerance.dbstrategy3

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{
  Actor,
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
import scala.concurrent.duration.DurationInt

object LogProcessingApp extends App {

  val sources = Vector("file:///source1/", "file:///source2/")
  val databaseUrl = "http://mydatabase"

  val writerProps = Props(new DbWriter(databaseUrl))
  val dbSuperProps = Props(new DbSupervisor(writerProps))
  val logProcSuperProps = Props(new LogProcSuperVisor(dbSuperProps))
  val topLevelProps = Props(new FileWatcherSuperVisor(sources, logProcSuperProps))

  val system = ActorSystem("logprocessing")
  system.actorOf(topLevelProps)
}

class FileWatcherSuperVisor(sources: Vector[String], logProcSuperProps: Props) extends Actor {
  var fileWatchers: Vector[ActorRef] = sources.map { source =>
    val logProcSuperVisor = context.actorOf(logProcSuperProps)
    val fileWatcher = context.actorOf(Props(new FileWatcher(source, logProcSuperVisor)))
    context.watch(fileWatcher)
    fileWatcher
  }

  override def supervisorStrategy: SupervisorStrategy = AllForOneStrategy() { case _: DisError =>
    Stop
  }

  override def receive: Receive = { case Terminated(fileWatcher) =>
    fileWatchers = fileWatchers.filterNot(w => w == fileWatcher)
    if (fileWatchers.isEmpty) self ! PoisonPill
  }
}

class FileWatcher(sourceUrl: String, logProcSuperVisor: ActorRef) extends Actor with FileWatchingAbilities {
  register(sourceUrl)

  import FileWatchingProtocol._
  import LogProcessingProtocol._

  override def receive: Receive = {
    case NewFile(file, _) =>
      logProcSuperVisor ! LogFile(file)

    case SourceAbandoned(uri) if uri == sourceUrl =>
      self ! PoisonPill
  }
}

class LogProcSuperVisor(dbSupervisorProps: Props) extends Actor {
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() { case _: CorruptedFileException =>
    Resume
  }

  val dbSupervisor: ActorRef = context.actorOf(dbSupervisorProps)
  val logProcProps: Props = Props(new LogProcessor(dbSupervisor))
  val logProcessor: ActorRef = context.actorOf(logProcProps)

  override def receive: Receive = { case m =>
    logProcessor forward m
  }
}

class LogProcessor(dbSupervisor: ActorRef) extends Actor with LogParsing {
  import LogProcessingProtocol._

  override def receive: Receive = { case LogFile(file) =>
    val lines = parse(file)
    lines.foreach(dbSupervisor ! _)
  }
}

class DbImpatientSuperVisor(writerProps: Props) extends Actor {
  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 60.seconds) { case _: DbBrokenConnectionException =>
      Restart
    }

  val writer: ActorRef = context.actorOf(writerProps)

  override def receive: Receive = { case m =>
    writer forward m
  }
}

class DbSupervisor(writerProps: Props) extends Actor {
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() { case _: DbBrokenConnectionException =>
    Restart
  }

  val writer: ActorRef = context.actorOf(writerProps)

  override def receive: Receive = { case m =>
    writer forward m
  }
}

class DbWriter(databaseUrl: String) extends Actor {
  val connection = new DbCon(databaseUrl)

  import LogProcessingProtocol._

  override def receive: Receive = { case Line(time, message, messageType) =>
    connection.write(Map(Symbol("time") -> time, Symbol("message") -> message, Symbol("messageType") -> messageType))
  }
}

class DbCon(url: String) {
  def write(map: Map[Symbol, Any]): Unit = ()
  def close(): Unit = ()
}

class DisError(msg: String) extends Error(msg)
class CorruptedFileException(msg: String, val file: File) extends Exception(msg)
class DbBrokenConnectionException(msg: String) extends Exception(msg)

trait LogParsing {
  import LogProcessingProtocol._
  def parse(file: File): Vector[Line] = Vector.empty[Line]
}

object FileWatchingProtocol {
  case class NewFile(file: File, timeAdded: Long)
  case class SourceAbandoned(uri: String)
}
trait FileWatchingAbilities {
  def register(uri: String): Unit = ()
}

object LogProcessingProtocol {
  case class LogFile(file: File)
  case class Line(time: Long, message: String, messageType: String)
}
