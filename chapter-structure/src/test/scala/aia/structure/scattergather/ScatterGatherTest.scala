package aia.structure.scattergather

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.Date
import scala.concurrent.duration.FiniteDuration

class ScatterGatherTest extends TestKit(ActorSystem("ScatterGatherTest")) with AnyWordSpecLike with BeforeAndAfterAll {
  val timeout: FiniteDuration = 2.seconds

  override def afterAll(): Unit = system.terminate()

  "The ScatterGather" must {
    "scatter the message and gather them again" in {
      val endProbe = TestProbe()
      val aggregateRef = system.actorOf(Props(new Aggregator(timeout, endProbe.ref)))
      val speedRef = system.actorOf(Props(new GetSpeed(aggregateRef)))
      val timeRef = system.actorOf(Props(new GetTime(aggregateRef)))
      val actorRef = system.actorOf(Props(new RecipientList(Seq(speedRef, timeRef))))

      val photoDate = new Date()
      val photoSpeed = 60
      val msg = PhotoMessage("id1", ImageProcessing.createPhotoString(photoDate, photoSpeed))

      actorRef ! msg

      val combinedMsg =
        PhotoMessage(id = msg.id, photo = msg.photo, creationTime = Some(photoDate), speed = Some(photoSpeed))

      endProbe.expectMsg(combinedMsg)
    }
  }
}
