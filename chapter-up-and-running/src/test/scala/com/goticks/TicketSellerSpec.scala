package com.goticks

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{FunSuite, MustMatchers, WordSpecLike}
import TicketSeller._

class TicketSellerSpec extends TestKit(ActorSystem("testTickets"))
  with WordSpecLike
  with MustMatchers
  with ImplicitSender
  with StopSystemAfterAll {

  "The TicketSeller" must {

    "Sell tickets until they are sold out" in {

      def mkTickets = (1 to 10).map(Ticket).toVector
      val event = "RHCP"
      val ticketingActor = system.actorOf(TicketSeller.props(event))

      ticketingActor ! Add(mkTickets)
      ticketingActor ! Buy(1)

      expectMsg(Tickets(event, Vector(Ticket(1))))

      val nrs = 2 to 10
      nrs.foreach(_ => ticketingActor ! Buy(1))

      val tickets = receiveN(9)
      tickets.zip(nrs).foreach {
        case (Tickets(`event`, Vector(Ticket(id))), ix) => id must be(ix)
      }

      ticketingActor ! Buy(1)
      expectMsg(Tickets(event))
    }

    "Sell tickets in batches until they are sold out" in {
      val firstBatchSize = 10

      def mkTickets = (1 to (10 * firstBatchSize)).map(Ticket).toVector

      val event = "Madlib"
      val ticketingActor = system.actorOf(TicketSeller.props(event))

      ticketingActor ! Add(mkTickets)
      ticketingActor ! Buy(firstBatchSize)
      val bought = (1 to firstBatchSize).map(Ticket).toVector

      expectMsg(Tickets(event, bought))

      val secondBatchSize = 5
      val nrBatches = 18

      val batches = 1 to nrBatches * secondBatchSize
      batches.foreach(_ => ticketingActor ! Buy(secondBatchSize))

      val tickets = receiveN(nrBatches)

      tickets.zip(batches).foreach {
        case (Tickets(event, bought), ix) =>
          bought.size must equal(secondBatchSize)
          val last = ix * secondBatchSize + firstBatchSize
          val first = ix * secondBatchSize + firstBatchSize - (secondBatchSize - 1)
          bought.map(_.id) must equal((first to last).toVector)
      }

      ticketingActor ! Buy(1)
      expectMsg(Tickets(event))

      ticketingActor ! Buy(10)
      expectMsg(Tickets(event))
    }

  }

}
