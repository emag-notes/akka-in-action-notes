package aia.structure.scattergather

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.Date
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.control.NoStackTrace

class AggregatorTest extends TestKit(ActorSystem("AggregatorTest")) with AnyWordSpecLike with BeforeAndAfterAll {
  val timeout: FiniteDuration = 2.seconds

  protected override def afterAll(): Unit = system.terminate()

  "The Aggregator" must {
    "aggregate two messages" in {
      val endProbe = TestProbe()
      val actorRef = system.actorOf(Props(new Aggregator(timeout, endProbe.ref)))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)
      val msg1 = PhotoMessage(id = "id1", photo = photoStr, creationTime = Some(new Date()), speed = None)
      actorRef ! msg1

      val msg2 = PhotoMessage(id = "id1", photo = photoStr, creationTime = None, speed = Some(60))
      actorRef ! msg2

      val combinedMsg = PhotoMessage(id = "id1", photo = photoStr, creationTime = msg1.creationTime, speed = msg2.speed)

      endProbe.expectMsg(combinedMsg)
    }
    "send message after timeout" in {
      val endProbe = TestProbe()
      val actorRef = system.actorOf(Props(new Aggregator(timeout, endProbe.ref)))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)
      val msg1 = PhotoMessage(id = "id1", photo = photoStr, creationTime = Some(new Date()), speed = None)
      actorRef ! msg1

      endProbe.expectMsg(msg1)
    }
    "aggregate two messages when restarting" in {
      val endProbe = TestProbe()
      val actorRef = system.actorOf(Props(new Aggregator(timeout, endProbe.ref)))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)

      val msg1 = PhotoMessage(id = "id1", photo = photoStr, creationTime = Some(new Date()), speed = None)
      actorRef ! msg1

      actorRef ! new IllegalStateException("restart") with NoStackTrace

      val msg2 = PhotoMessage(id = "id1", photo = photoStr, creationTime = None, speed = Some(60))
      actorRef ! msg2

      val combinedMsg = PhotoMessage(id = "id1", photo = photoStr, creationTime = msg1.creationTime, speed = msg2.speed)

      endProbe.expectMsg(combinedMsg)
    }
  }
}
