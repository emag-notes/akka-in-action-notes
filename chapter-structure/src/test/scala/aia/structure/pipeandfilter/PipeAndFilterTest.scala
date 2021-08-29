package aia.structure.pipeandfilter

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class PipeAndFilterTest extends TestKit(ActorSystem("PipeAndFilterTest")) with AnyWordSpecLike with BeforeAndAfterAll {
  val timeout: FiniteDuration = 1.second

  override protected def afterAll(): Unit = system.terminate()

  "The pipe and filter" must {
    "filter messages in configuration 1" in {
      val endProbe = TestProbe()
      val speedFilterRef = system.actorOf(Props(new SpeedFilter(50, endProbe.ref)))
      val licenseFilterRef = system.actorOf(Props(new LicenseFilter(speedFilterRef)))
      val msg = Photo("123xyz", 60)
      licenseFilterRef ! msg
      endProbe.expectMsg(msg)

      licenseFilterRef ! Photo("", 60)
      endProbe.expectNoMessage(timeout)

      licenseFilterRef ! Photo("123xyz", 49)
      endProbe.expectNoMessage(timeout)
    }
    "filter messages in configuration 2" in {
      val endProbe = TestProbe()
      val licenseFilterRef = system.actorOf(Props(new LicenseFilter(endProbe.ref)))
      val speedFilterRef = system.actorOf(Props(new SpeedFilter(50, licenseFilterRef)))
      val msg = Photo("123xyz", 60)
      speedFilterRef ! msg
      endProbe.expectMsg(msg)

      speedFilterRef ! Photo("", 60)
      endProbe.expectNoMessage(timeout)

      speedFilterRef ! Photo("123xyz", 49)
      endProbe.expectNoMessage(timeout)
    }
  }
}
