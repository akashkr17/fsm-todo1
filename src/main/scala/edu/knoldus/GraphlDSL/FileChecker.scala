package edu.knoldus.GraphlDSL

import java.io.File
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

  object FileChecker extends App {
    implicit val actorSystem: ActorSystem = ActorSystem("system")
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()


    /***
     * Graph Checks the file exits or not if exits print the absolute path
     */
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val source = Source(List("address.csv", "test2.txt", "test3.txt"))
      val mapper = Flow[String].map(new File(_))
      val existsFilter = Flow[File].filter(_.exists())
      val lengthZeroFilter = Flow[File].filter(_.length() != 0)
      val sink = Sink.foreach[File](f => println(s"Absolute path: ${f.getAbsolutePath}"))
      source ~> mapper ~> existsFilter ~> lengthZeroFilter ~> sink

      ClosedShape
    })

    graph.run()
  }