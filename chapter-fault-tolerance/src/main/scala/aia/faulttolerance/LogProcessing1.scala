package aia.faulttolerance
package dbstrategy1 {

  import java.io.File
  import java.util.UUID

  import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
  import akka.actor._

  object LogProcessingApp extends App {
    var sources = Vector("file:///source1/", "file:///source2/")
    val system  = ActorSystem("logprocessing")

    val databaseUrl = "http://mydatabase1"

    system.actorOf(
      LogProcessingSuperVisor.props(sources, databaseUrl),
      LogProcessingSuperVisor.name
    )
  }

  object LogProcessingSuperVisor {
    def props(sources: Vector[String], databaseUrl: String) = Props(new LogProcessingSuperVisor(sources, databaseUrl))
    def name                                                = "file-watcher-supervisor"
  }

  class LogProcessingSuperVisor(sources: Vector[String], databaseUrl: String) extends Actor with ActorLogging {

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: CorruptedFileException      => Resume
      case _: DbBrokenConnectionException => Restart
      case _: DiskError                   => Stop
    }

    var fileWatchers = sources.map { source =>
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

    override def receive: Receive = {
      case Terminated(actorRef) =>
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
    def props(source: String, logProcessor: ActorRef) = Props(new FileWatcher(source, logProcessor))
    def name                                          = s"file-watcher-${UUID.randomUUID().toString}"
    case class NewFile(file: File, timeAdded: Long)
    case class SourceAbandoned(uri: String)
  }

  class FileWatcher(source: String, logProcessor: ActorRef) extends Actor with FileWatchingAbilities {

    register(source)

    import FileWatcher._

    override def receive: Receive = {
      case NewFile(file, _)                      => logProcessor ! LogProcessor.LogFile(file)
      case SourceAbandoned(uri) if uri == source => self ! PoisonPill
    }

  }

  object LogProcessor {
    def props(dbWriter: ActorRef) = Props(new LogProcessor(dbWriter))
    def name                      = s"log-processor-${UUID.randomUUID().toString}"
    // 新しいログファイル
    case class LogFile(file: File)
  }

  class LogProcessor(dbWriter: ActorRef) extends Actor with ActorLogging with LogParsing {

    import LogProcessor._

    override def receive: Receive = {
      case LogFile(file) =>
        val lines: Vector[DbWriter.Line] = parse(file)
        lines.foreach(dbWriter ! _)
    }
  }

  object DbWriter {
    def props(databaseUrl: String) = Props(new DbWriter(databaseUrl))
    def name(databaseUrl: String)  = s"db-writer-${databaseUrl.split("/").last}"
    // LogProcessor アクターによって解析されるログファイルの行
    case class Line(time: Long, message: String, messageType: String)
  }

  class DbWriter(databaseUrl: String) extends Actor {
    val connection = new DbCon(databaseUrl)

    import DbWriter._
    override def receive: Receive = {
      case Line(time, message, messageType) =>
        connection.write(Map('time -> time, 'message -> message, 'messageType -> messageType))
    }

    override def postStop(): Unit = connection.close()
  }

  class DbCon(url: String) {

    /**
      * Writes a map to a database.
      * @param map the map to write to the database.
      * @throws DbBrokenConnectionException when the connection is broken. It might be back later
      * @throws DbNodeDownException when the database Node has been removed from the database cluster. It will never work again.
      */
    def write(map: Map[Symbol, Any]): Unit = {
      //
    }

    def close(): Unit = {
      //
    }
  }

  @SerialVersionUID(1L)
  class DiskError(msg: String) extends Error(msg) with Serializable

  @SerialVersionUID(1L)
  class CorruptedFileException(msg: String, val file: File) extends Exception(msg) with Serializable

  @SerialVersionUID(1L)
  class DbBrokenConnectionException(msg: String) extends Exception(msg) with Serializable

  @SerialVersionUID(1L)
  class DbNodeDownException(msg: String) extends Exception(msg) with Serializable

  trait LogParsing {
    import DbWriter._
    // ログファイルの解析。ログファイル内の行から行オブジェクトを作成する
    // ファイルが破損している場合、CorruptedFileExceptionをスローする
    def parse(file: File): Vector[Line] = {
      // ここにパーサーを実装、今はダミー値を返す
      Vector.empty[Line]
    }
  }

  trait FileWatchingAbilities {
    def register(uri: String): Unit = {}
  }

}
