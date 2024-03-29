package com.goticks

import akka.actor.{ActorIdentity, ActorPath, Identify}
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import akka.util.Timeout

import scala.concurrent.duration.DurationInt

class ClientServerSpecMultiJvmFrontend extends ClientServerSpec
class ClientServerSpecMultiJvmBackend extends ClientServerSpec

class ClientServerSpec extends MultiNodeSpec(ClientServerConfig) with STMultiNodeSpec with ImplicitSender {
  import ClientServerConfig._

  val backendNode: ActorPath = node(backend)

  override def initialParticipants: Int = roles.size

  "A Client Server configured app" must {
    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "be able to create an event and sell a ticket" in {
      runOn(backend) {
        system.actorOf(BoxOffice.props(Timeout(1.second)), "boxOffice")
        enterBarrier("deployed")
      }

      runOn(frontend) {
        enterBarrier("deployed")

        val path = node(backend) / "user" / "boxOffice"
        val actorSelection = system.actorSelection(path)

        actorSelection.tell(Identify(`path`), testActor)

        val actorRef = expectMsgPF() { case ActorIdentity(`path`, Some(ref)) =>
          ref
        }

        import BoxOffice._
        import TicketSeller._

        actorRef ! CreateEvent("RHCP", 20000)
        expectMsg(EventCreated(Event("RHCP", 20000)))

        actorRef ! GetTickets("RHCP", 1)
        expectMsg(Tickets("RHCP", Vector(Ticket(1))))
      }

      enterBarrier("finished")
    }
  }
}
