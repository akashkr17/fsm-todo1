package edu.knoldus


import akka.actor.{ActorSystem, FSM, Props}

import scala.collection.immutable
import scala.collection.immutable.Queue
import scala.concurrent.duration._

case class Message(n: Int)
case object Flush2

sealed trait NewState
case object Waiting extends NewState
case object ActiveState extends NewState

case class StateData(queue: immutable.Queue[Message])

class SizeBasedThrottler extends FSM[NewState, StateData] {
  val startTime: Long = System.currentTimeMillis()

  def currentTime: String = {
    val time = (System.currentTimeMillis() - startTime) / 1000f
    f"[$time%3.2f]"
  }

  startWith(Waiting, StateData(Queue.empty))

  onTransition {
    case Waiting -> ActiveState =>

      nextStateData match {
        case StateData(queue) =>
          for(x <- queue) yield println(s"$currentTime processing ${x.n} ")
          Thread.sleep(2000L) //
      }
  }

  when(ActiveState) {
    case Event(msg: Message, _) =>
      println(s"$currentTime at Active $msg" )

      goto(Waiting) using StateData(Queue(msg))

    case Event(_, _) => stay()
  }

  when(Waiting, stateTimeout = 2.seconds){
    case Event(msg: Message, StateData(oldQueue)) =>
      val newQueue = oldQueue :+ msg
      println(s"$currentTime at Idle $newQueue")
      stay() using StateData(newQueue)

    case Event(Flush2, StateData(queue)) => goto(ActiveState) using StateData(queue)

    case Event(StateTimeout, StateData(queue)) => goto(ActiveState) using StateData(queue)

    case Event(_, StateData(queue)) => stay() using StateData(queue)

  }

  initialize()
}

object demo extends App  {
  val threshold = 3
  val requests = 20
  val actorSystem = ActorSystem("system")
  val actor = actorSystem.actorOf(Props(classOf[SizeBasedThrottler]))
  for{
    i <- 1 to requests
    _ = println(s"Sending $i")
    _ = actor ! Message(i)
    _ = if(i % threshold == 0) actor ! Flush2
  } yield {}

}