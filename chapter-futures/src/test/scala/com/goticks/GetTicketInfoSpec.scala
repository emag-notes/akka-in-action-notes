package com.goticks

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{ExecutionContext, Future}

class GetTicketInfoSpec extends AnyWordSpecLike with Matchers with ScalaFutures {
  object TicketInfoService extends TicketInfoService with MockWebServiceCalls
  import TicketInfoService._

  "getTicketInfo" must {
    "return a complete ticket info when all futures are successful" in {
      // When
      val ticketInfo = getTicketInfo("1234", Location(1d, 2d)).futureValue

      // Then
      ticketInfo.event.isEmpty must be(false)
      ticketInfo.event.foreach(_.name must be("Quasimoto"))
      ticketInfo.travelAdvice.isEmpty must be(false)
      ticketInfo.suggestions.map(_.name) must be(Seq("Madlib", "OhNo", "Flying Lotus"))
    }

    "return an incomplete ticket info when getEvent fails" in {
      // When
      val ticketInfo = getTicketInfo("4321", Location(1d, 2d)).futureValue

      ticketInfo.event.isEmpty must be(true)
      ticketInfo.travelAdvice.isEmpty must be(true)
      ticketInfo.suggestions.isEmpty must be(true)
    }
  }
}

trait MockWebServiceCalls extends WebServiceCalls {
  import com.github.nscala_time.time.Imports._

  import ExecutionContext.Implicits.global

  override def getEvent(ticketNr: String, location: Location): Future[TicketInfo] = Future {
    if (ticketNr == "1234") {
      TicketInfo(
        ticketNr,
        location,
        event = Some(Event("Quasimoto", Location(4.324218908d, 53.12311144d), new DateTime(2013, 10, 1, 22, 30)))
      )
    } else throw new Exception("crap")
  }

  override def callWeatherXService(ticketInfo: TicketInfo): Future[Option[Weather]] = Future(
    Some(Weather(30, precipitation = false))
  )

  override def callWeatherYService(ticketInfo: TicketInfo): Future[Option[Weather]] = Future(
    Some(Weather(30, precipitation = false))
  )

  override def callTrafficService(
      origin: Location,
      destination: Location,
      time: DateTime
  ): Future[Option[RouteByCar]] = Future(
    Some(RouteByCar("route1", time - 35.minutes, origin, destination, 30.minutes, 5.minutes))
  )

  override def callPublicTransportService(
      origin: Location,
      destination: Location,
      time: DateTime
  ): Future[Option[PublicTransportAdvice]] = Future(
    Some(PublicTransportAdvice("public transport route 1", time - 20.minutes, origin, destination, 20.minutes))
  )

  override def callSimilarArtistsService(event: Event): Future[Seq[Artist]] = Future(
    Seq(
      Artist("Madlib", "madlib.com/calendar"),
      Artist("OhNo", "ohno.com/calendar"),
      Artist("Flying Lotus", "fly.lo/calendar")
    )
  )

  override def callArtistCalendarService(artist: Artist, nearLocation: Location): Future[Event] = Future(
    Event(artist.name, Location(1d, 1d), DateTime.now)
  )
}
