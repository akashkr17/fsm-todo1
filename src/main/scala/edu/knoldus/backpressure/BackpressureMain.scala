package edu.knoldus.backpressure

import java.io.File

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, IOResult, OverflowStrategy, ThrottleMode}
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration._


object BackpressureMain extends App {
  implicit val system: ActorSystem = ActorSystem("backpressureSystem")

  val input = Source(1 to 1000)
  val flow = Flow[Int].map { x=>
    println(s"[Flow] $x")
    x
  }

  val output = Sink.foreach[Int] { x=>
    Thread.sleep(1000)
    println(s"$x")
  }

  val graph1 = input.via(flow).async.to(output)
  val graph2 = input.via(flow.buffer(10,OverflowStrategy.backpressure)).async.to(output)

  val graph3 = input.via(flow.buffer(10,OverflowStrategy.dropHead)).async.to(output)
  val graph4 = input.via(flow.buffer(10,OverflowStrategy.dropTail)).async.to(output)
  val graph5 = input.via(flow.buffer(10,OverflowStrategy.dropNew)).async.to(output).run

}

object StreamApp  {
  def lineSink(filename: String): Sink[String, Future[IOResult]] = {
    Flow[String]
      .alsoTo(Sink.foreach(s => println(s"$filename: $s")))
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toFile(new File(filename)))(Keep.right)
  }
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("streamapp")
    implicit val materializer = ActorMaterializer()
    val source: Source[Int, NotUsed] = Source(1 to 100)
    val factorials: Source[BigInt, NotUsed] =
      source.scan(BigInt(1))((acc, next) => acc * next)
    val sink1 = lineSink("factorial1.txt")
    val sink2 = lineSink("factorial2.txt")
    val slowSink2 = Flow[String].via(Flow[String]
      .throttle(1, 1.second, 1, ThrottleMode.shaping))
      .toMat(sink2)(Keep.right)
    val bufferedSink2 = Flow[String].buffer(50, OverflowStrategy.dropNew)
      .via(Flow[String]
        .throttle(1, 1.second, 1, ThrottleMode.shaping))
      .toMat(sink2)(Keep.right)
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val bcast = b.add(Broadcast[String](2))
      factorials.map(_.toString) ~> bcast.in
      bcast.out(0) ~> sink1
      bcast.out(1) ~> sink2
      ClosedShape
    })
    g.run()
  }
}