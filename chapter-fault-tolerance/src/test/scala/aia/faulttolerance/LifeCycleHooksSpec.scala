package aia.faulttolerance

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class LifeCycleHooksSpec extends TestKit(ActorSystem("LiceCycleTest")) with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The Child" must {
    "log lifecycle hooks" in {
      import LifeCycleHooks._

      val testActorRef = system.actorOf(Props[LifeCycleHooks], "LifeCycleHooks")
      watch(testActorRef)
      testActorRef ! ForceRestart
      testActorRef.tell(SampleMessage, testActor)
      expectMsg(SampleMessage)
      system.stop(testActorRef)
      expectTerminated(testActorRef)
    }
  }

}
