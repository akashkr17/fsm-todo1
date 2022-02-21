package edu.knoldus

import akka.actor.{Actor, ActorRef, ActorSystem, FSM, Props}

import scala.concurrent.duration._
import scala.collection._
import scala.io.StdIn

// received events
final case class SetTarget(ref: ActorRef)
final case class Queue(obj: Any)
case object Flush

// sent events
final case class Batch(obj: immutable.Seq[Any])

// states
sealed trait State
case object Idle extends State
case object Active extends State

//data
sealed trait BuncherData
case object Uninitialized extends BuncherData
final case class Todo(target: ActorRef, queue: immutable.Seq[Any]) extends BuncherData

class BuncherActor extends FSM[State, BuncherData] {

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(SetTarget(ref), Uninitialized) =>
      stay using Todo(ref, Vector.empty)
  }

  when(Active, stateTimeout = 1 second) {
    case Event(Flush | StateTimeout, t: Todo) =>
      goto(Idle) using t.copy(queue = Vector.empty)
  }

  whenUnhandled {
    // common code for both states
    case Event(Queue(obj), t @ Todo(_, v)) =>
      goto(Active) using t.copy(queue = v :+ obj)

    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  onTransition {
    case Active -> Idle =>
      stateData match {
        case Todo(ref, queue) => ref ! Batch(queue)
        case _                => // nothing to do
      }
  }

  initialize()
}


class BunchReceivingActor extends Actor {
  def receive: Receive = {
    case Batch(theBatchData) => {
      println(s"receiving the batch data $theBatchData")
    }
    case _ => println("unknown message")
  }
}

object Demo extends App {

  RunBuncherDemo

  def RunBuncherDemo() : Unit = {
    //create the actor system
    val system = ActorSystem("StateMachineSystem")

    val buncherActor =
      system.actorOf(Props(classOf[BuncherActor]),
        "demo-Buncher")

    val bunchReceivingActor =
      system.actorOf(Props(classOf[BunchReceivingActor]),
        "demo-BunchReceiving")

    buncherActor ! SetTarget(bunchReceivingActor)

    println("sending Queue(42)")
    buncherActor ! Queue(42)
    println("sending Queue(43)")
    buncherActor ! Queue(43)
    println("sending Queue(44)")
    buncherActor ! Queue(44)
    println("sending Flush")
    buncherActor ! Flush
    println("sending Queue(45)")
    buncherActor ! Queue(45)


    StdIn.readLine()

    //shutdown the actor system
    system.terminate()

    StdIn.readLine()
  }
}