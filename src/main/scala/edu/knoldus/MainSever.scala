package edu.knoldus


import scala.concurrent.duration.DurationInt
import scala.util.Success
import scala.language.postfixOps
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.FSM
import akka.actor.PoisonPill
import akka.actor.Props

import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Tcp
import akka.util.ByteString
import akka.util.Timeout

/**
 * TCP client that connects to 127.0.0.1:9999, using a request/response pattern.
 *
 */
object MainSever extends App {
  implicit val system: ActorSystem = ActorSystem("Sys")

  import system.dispatcher

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val client = system.actorOf(Props[ClientActor])

  implicit val timeout: Timeout = Timeout(15 seconds)

  while (true) {
    val r = client ? (scala.io.StdIn.readLine() + "\n")
    r.onComplete {
      case Success(v: ByteString) => println(v.utf8String)
      case _@x => println(x)
    }
  }

  sealed trait State

  case object Disconnected extends State

  case object Connected extends State

  case object AwaitResult extends State

  sealed trait Data

  case object Empty extends Data

  case class Connection(sink: ActorRef) extends Data

  case class Transaction(connection: Connection, requestor: ActorRef) extends Data

  class ClientActor extends FSM[State, Data] {

    startWith(Disconnected, Empty)

    when(Disconnected) {
      case Event(s: String, Empty) =>
        println("inside disconnected")
        val c = makeConnection
        c.sink ! ByteString(s)
        println("after connected")
        goto(AwaitResult) using Transaction(c, sender())
    }

    when(Connected) {
      case Event(s: String, c: Connection) =>
        c.sink ! ByteString(s)
        println("inside connected")
        goto(AwaitResult) using Transaction(c, sender())
    }

    when(AwaitResult) {
      case Event(bytes: ByteString, Transaction(connection, requestor)) =>
        requestor ! bytes
        println("inside Await")
        goto(Connected) using connection
      case Event(s: String, Transaction(connection, _)) =>
        println("inside Await")
        connection.sink ! ByteString(s)
        stay using Transaction(connection, sender())
    }

    whenUnhandled {
      case Event(_, c: Connection) =>
        c.sink ! PoisonPill
        goto(Disconnected) using Empty
      case Event(_, Transaction(c, _)) =>
        c.sink ! PoisonPill
        goto(Disconnected) using Empty
    }

    private def makeConnection = {
      case object Done
      println("inside makeco")
      val connection = Tcp().outgoingConnection("127.0.0.1", 9999)
      val src = Source.actorRef[ByteString](10, OverflowStrategy.fail)
      val sink = Sink.actorRef(self, Done)
      Connection(src.via(connection).to(sink).run)
    }
    initialize()
  }
}

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

  def RunBuncherDemo : Unit = {
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