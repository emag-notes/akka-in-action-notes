package com.goticks

import com.github.nscala_time.time.Imports.DateTime

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.control.NonFatal

trait TicketInfoService extends WebServiceCalls {
  import scala.concurrent.ExecutionContext.Implicits.global

  def getTicketInfo(ticketNr: String, location: Location): Future[TicketInfo] = {
    val emptyTicketInfo = TicketInfo(ticketNr, location)
    val eventInfo       = getEvent(ticketNr, location).recover(withPrevious(emptyTicketInfo))

    eventInfo.flatMap { info =>
      val infoWithWeather = getWeather(info)

      val infoWithTravelAdvice = info.event
        .map { event =>
          getTravelAdvice(info, event)
        }
        .getOrElse(eventInfo)

      val suggestedEvents = info.event
        .map { event =>
          getSuggestions(event)
        }
        .getOrElse(Future.successful(Nil))

      val ticketInfos = Seq(infoWithTravelAdvice, infoWithWeather)

      val infoWithTravelAndWeather = Future.foldLeft(ticketInfos)(info) { (acc, elem) =>
        val (travelAdvice, weather) = (elem.travelAdvice, elem.weather)
        acc.copy(travelAdvice = travelAdvice.orElse(acc.travelAdvice), weather = weather.orElse(acc.weather))
      }

      for {
        info        <- infoWithTravelAndWeather
        suggestions <- suggestedEvents
        result = info.copy(suggestions = suggestions)
      } yield result
    }
  }

  type Recovery[T] = PartialFunction[Throwable, T]

  def withNone[T]: Recovery[Option[T]]                         = { case NonFatal(_) => None }
  def withEmptySeq[T]: Recovery[Seq[T]]                        = { case NonFatal(_) => Nil }
  def withPrevious(previous: TicketInfo): Recovery[TicketInfo] = { case NonFatal(_) => previous }

  def getWeather(ticketInfo: TicketInfo): Future[TicketInfo] = {
    val futureWeatherX = callWeatherXService(ticketInfo).recover(withNone)
    val futureWeatherY = callWeatherYService(ticketInfo).recover(withNone)

    Future.firstCompletedOf(Seq(futureWeatherX, futureWeatherY)).map { weatherResponse =>
      ticketInfo.copy(weather = weatherResponse)
    }
  }

  def getTravelAdvice(info: TicketInfo, event: Event): Future[TicketInfo] = {
    val futureRoute = callTrafficService(info.userLocation, event.location, event.time).recover(withNone)
    val futurePublicTransport =
      callPublicTransportService(info.userLocation, event.location, event.time).recover(withNone)

    for {
      (routeByCar, publicTransportAdvice) <- futureRoute.zip(futurePublicTransport)
      travelAdvice = TravelAdvice(routeByCar, publicTransportAdvice)
      ticketInfo   = info.copy(travelAdvice = Some(travelAdvice))
    } yield ticketInfo
  }

  def getSuggestions(event: Event): Future[Seq[Event]] = {
    val futureArtists = callSimilarArtistsService(event).recover(withEmptySeq)

    for {
      artists <- futureArtists.recover(withEmptySeq)
      events  <- getPlannedEvents(event, artists).recover(withEmptySeq)
    } yield events
  }

  def getPlannedEvents(event: Event, artists: Seq[Artist]): Future[Seq[Event]] = {
    val events: Seq[Future[Event]] = artists.map(artist => callArtistCalendarService(artist, event.location))
    Future.sequence(events)
  }
}

trait WebServiceCalls {
  def getEvent(ticketNr: String, location: Location): Future[TicketInfo]

  def callWeatherXService(ticketInfo: TicketInfo): Future[Option[Weather]]

  def callWeatherYService(ticketInfo: TicketInfo): Future[Option[Weather]]

  def callTrafficService(origin: Location, destination: Location, time: DateTime): Future[Option[RouteByCar]]

  def callPublicTransportService(origin: Location,
                                 destination: Location,
                                 time: DateTime): Future[Option[PublicTransportAdvice]]

  def callSimilarArtistsService(event: Event): Future[Seq[Artist]]

  def callArtistCalendarService(artist: Artist, nearLocation: Location): Future[Event]
}
