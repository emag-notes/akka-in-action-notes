package aia.stream

import java.time.ZonedDateTime
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import spray.json._

trait EventMarshalling extends DefaultJsonProtocol {

  implicit val dateTimeFormat = new JsonFormat[ZonedDateTime] {
    override def write(dateTime: ZonedDateTime): JsValue = JsString(dateTime.format(DateTimeFormatter.ISO_INSTANT))

    override def read(value: JsValue): ZonedDateTime = value match {
      case JsString(str) =>
        try {
          ZonedDateTime.parse(str)
        } catch {
          case _: DateTimeParseException =>
            val msg = s"Could not deserialize $str to ZonedDateTime."
            deserializationError(msg)
        }
      case js =>
        val msg = s"Could not deserialize $js to ZonedDateTime."
        deserializationError(msg)
    }
  }

  implicit val stateFormat = new JsonFormat[State] {
    override def write(state: State): JsValue = JsString(State.norm(state))

    override def read(value: JsValue): State = value match {
      case JsString("ok")       => Ok
      case JsString("warning")  => Warning
      case JsString("error")    => Error
      case JsString("critical") => Critical
      case js =>
        val msg = s"Could not deserialize $js to State."
        deserializationError(msg)
    }
  }

  implicit val eventFormat = jsonFormat7(Event)
  implicit val logIdFormat = jsonFormat2(LogReceipt)
  implicit val errorFormat = jsonFormat2(ParseError)
}