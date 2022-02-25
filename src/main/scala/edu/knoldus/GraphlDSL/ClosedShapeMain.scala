package edu.knoldus.GraphlDSL

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, Graph}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object ClosedShapeOne extends App {
  implicit val system: ActorSystem = ActorSystem()

  val graph: Graph[ClosedShape.type, NotUsed] = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
  import GraphDSL.Implicits._

    val input = builder.add(Source(1 to 1000))
    val adder = builder.add(Flow[Int].map(x => x + 1))

    val multiplier = builder.add(Flow[Int].map(x => x * 10))

    val output = builder.add(Sink.foreach[(Int,Int)](println))

    val broadcast = builder.add(Broadcast[Int](2)) //fan -out
    val zip = builder.add(Zip[Int,Int]) // fan -in

    input ~> broadcast
    broadcast.out(0) ~> adder ~> zip.in0
    broadcast.out(1) ~> multiplier ~> zip.in1
    zip.out ~> output
    ClosedShape
  }

  RunnableGraph.fromGraph(graph).run()

}

object ClosedShapeTwo extends App {

  implicit val system: ActorSystem = ActorSystem("closedShapeTwao")

  val graph = GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val input = builder.add(Source(1 to 10))
    val output = builder.add(Sink.foreach(println))
    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))
    val f1, f2, f3, f4 = Flow[Int].map(_ + 1)
    val f5 = Flow[Int].filter(x => x % 2 ==0)
    input ~> f1 ~> bcast ~> f2 ~> merge ~> f4  ~> f5 ~> output
    bcast ~> f3 ~> merge
    ClosedShape
  }
  RunnableGraph.fromGraph(graph).run()
  system.terminate
}

object ClosedShapeThree extends App {

  implicit val system = ActorSystem("system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val shoppingList: List[(String, Int)] = List(("Keyboard", 5), ("Mouse", 2), ("Monitor", 79), ("Memory", 32))

  val source = Source(shoppingList)
  val printItemSink = Sink.foreach[String](v => println("item: " + v))
  val printCostSink = Sink.foreach[Int](v => println("cost: " + v))

  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[(String, Int)](2))
    source ~> bcast
    bcast.out(0) ~> Flow[(String, Int)].map(_._1) ~> printItemSink
    bcast.out(1) ~> Flow[(String, Int)].map(_._2) ~> printCostSink
    ClosedShape
  })
  g.run()
}
