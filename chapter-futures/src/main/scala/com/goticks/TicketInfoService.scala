package com.goticks

import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait TicketInfoService extends WebServiceCalls {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  type Recovery[T] = PartialFunction[Throwable, T]
  def withNone[T]: Recovery[Option[T]] = { case NonFatal(_) => None }
  def withEmptySeq[T]: Recovery[Seq[T]] = { case NonFatal(_) => Seq() }
  def withPrevious(previous: TicketInfo): Recovery[TicketInfo] = { case NonFatal(_) => previous }

  def getTicketInfo(ticketNr: String, location: Location): Future[TicketInfo] = {
    val emptyTicketInfo = TicketInfo(ticketNr, location)
    val eventInfo = getEvent(ticketNr, location).recover(withPrevious(emptyTicketInfo))

    eventInfo.flatMap { info =>
      val infoWithTravelAdvice = info.event.map(getTravelAdvice(info, _)).getOrElse(eventInfo)
      val infoWithWeather = getWeather(info)
      val ticketInfos = Seq(infoWithTravelAdvice, infoWithWeather)
      val infoWithTravelAndWeather = Future.foldLeft(ticketInfos)(info) { (acc, elem) =>
        val (travelAdvice, weather) = (elem.travelAdvice, elem.weather)
        acc.copy(travelAdvice = travelAdvice.orElse(acc.travelAdvice), weather = weather.orElse(acc.weather))
      }

      val suggestedEvents = info.event.map(getSuggestions).getOrElse(Future.successful(Seq()))

      for {
        info <- infoWithTravelAndWeather
        suggestions <- suggestedEvents
      } yield info.copy(suggestions = suggestions)
    }
  }

  def getPlannedEvents(event: Event, artists: Seq[Artist]): Future[Seq[Event]] =
    Future.sequence(artists.map(callArtistCalendarService(_, event.location)))

  def getSuggestions(event: Event): Future[Seq[Event]] = {
    val futureArtists = callSimilarArtistsService(event).recover(withEmptySeq)
    for {
      artists <- futureArtists.recover(withEmptySeq)
      events <- getPlannedEvents(event, artists).recover(withEmptySeq)
    } yield events
  }

  def getTravelAdvice(info: TicketInfo, event: Event): Future[TicketInfo] = {
    val futureRoute = callTrafficService(info.userLocation, event.location, event.time).recover(withNone)
    val futurePublicTransport =
      callPublicTransportService(info.userLocation, event.location, event.time).recover(withNone)

    for {
      (routeByCar, publicTransportAdvice) <- futureRoute.zip(futurePublicTransport)
    } yield info.copy(travelAdvice = Some(TravelAdvice(routeByCar, publicTransportAdvice)))
  }

  def getWeather(ticketInfo: TicketInfo): Future[TicketInfo] = {
    val futureWeatherX = callWeatherXService(ticketInfo).recover(withNone)
    val futureWeatherY = callWeatherYService(ticketInfo).recover(withNone)
    Future.firstCompletedOf(Seq(futureWeatherX, futureWeatherY)).map { weatherResponse =>
      ticketInfo.copy(weather = weatherResponse)
    }
  }
}

trait WebServiceCalls {
  def getEvent(ticketNr: String, location: Location): Future[TicketInfo]

  def callWeatherXService(ticketInfo: TicketInfo): Future[Option[Weather]]

  def callWeatherYService(ticketInfo: TicketInfo): Future[Option[Weather]]

  def callTrafficService(origin: Location, destination: Location, time: DateTime): Future[Option[RouteByCar]]

  def callPublicTransportService(
      origin: Location,
      destination: Location,
      time: DateTime
  ): Future[Option[PublicTransportAdvice]]

  def callSimilarArtistsService(event: Event): Future[Seq[Artist]]

  def callArtistCalendarService(artist: Artist, nearLocation: Location): Future[Event]
}
