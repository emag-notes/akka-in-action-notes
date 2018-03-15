package ala.testdriven

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{MustMatchers, WordSpecLike}

class EchoActorSPec
    extends TestKit(ActorSystem("testsystem"))
    with WordSpecLike
    with MustMatchers
    with ImplicitSender
    with StopSystemAfterAll {

  // 何もせず受信したものと同じメッセージを返す
  "Reply with the same message it receives without ask" in {
    val echo = system.actorOf(Props[EchoActor], "echo")
    echo ! "some message"
    expectMsg("some message")
  }
}
