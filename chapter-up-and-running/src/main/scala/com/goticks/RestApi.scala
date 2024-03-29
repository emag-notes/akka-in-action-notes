package com.goticks

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future

class RestApi(system: ActorSystem, timeout: Timeout) extends RestRoutes {
  override implicit def requestTimeout: Timeout = timeout

  override def createBoxOffice(): ActorRef = system.actorOf(BoxOffice.props, BoxOffice.name)
}

trait RestRoutes extends BoxOfficeApi with EventMarshalling {

  import StatusCodes._

  def routes: Route = eventsRoute ~ eventRoute ~ ticketRoute

  def eventsRoute: Route = pathPrefix("events") {
    pathEndOrSingleSlash {
      get {
        // GET /events
        onSuccess(getEvents) { events =>
          complete(OK, events)
        }
      }
    }
  }

  def eventRoute: Route = pathPrefix("events" / Segment) { event =>
    pathEndOrSingleSlash {
      post {
        // POST /events/:event
        entity(as[EventDescription]) { ed =>
          onSuccess(createEvent(event, ed.tickets)) {
            case BoxOffice.EventCreated(event) => complete(Created, event)
            case BoxOffice.EventExists =>
              val err = Error(s"$event event exists already.")
              complete(BadRequest, err)
          }
        }
      } ~
        get {
          // GET /events/:event
          onSuccess(getEvent(event)) {
            _.fold(complete(NotFound))(complete(OK, _))
          }
        } ~
        delete {
          // DELETE /events/:event
          onSuccess(cancelEvent(event)) {
            _.fold(complete(NotFound))(complete(OK, _))
          }
        }
    }
  }

  def ticketRoute: Route = pathPrefix("events" / Segment / "tickets") { event =>
    post {
      pathEndOrSingleSlash {
        // POST /events/:event/tickets
        entity(as[TicketRequest]) { request =>
          onSuccess(requestTickets(event, request.tickets)) { tickets =>
            if (tickets.entries.isEmpty) complete(NotFound)
            else complete(Created, tickets)
          }
        }
      }
    }
  }

}

trait BoxOfficeApi {
  import BoxOffice._

  def createBoxOffice(): ActorRef

  implicit def requestTimeout: Timeout

  lazy val boxOffice: ActorRef = createBoxOffice()

  def createEvent(event: String, nrOfTickets: Int): Future[EventResponse] =
    boxOffice.ask(CreateEvent(event, nrOfTickets)).mapTo[EventResponse]

  def getEvents: Future[Events] = boxOffice.ask(GetEvents).mapTo[Events]

  def getEvent(event: String): Future[Option[Event]] = boxOffice.ask(GetEvent(event)).mapTo[Option[Event]]

  def cancelEvent(event: String): Future[Option[Event]] = boxOffice.ask(CancelEvent(event)).mapTo[Option[Event]]

  def requestTickets(event: String, tickets: Int): Future[TicketSeller.Tickets] =
    boxOffice.ask(GetTickets(event, tickets)).mapTo[TicketSeller.Tickets]
}
