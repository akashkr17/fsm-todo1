//package edu.knoldus.GraphlDSL
//
//import java.nio.file.{Path, Paths}
//
//import akka.NotUsed
//import akka.stream.FlowShape
//import akka.stream.alpakka.file.scaladsl.Directory
//import akka.stream.scaladsl.{FileIO, Flow, GraphDSL, Merge, Partition}
//import io.circe._
//import io.circe.generic.auto._
//import io.circe.parser._
//import io.circe.syntax._
//import spray.json.{DefaultJsonProtocol, RootJsonFormat}
//
//import spray.json._
//object FileCombiner extends App with DefaultJsonProtocol {
//  sealed trait ProcessableFile
//  case class Document1(metadata: Metadata, text: Text)
//  case class Metadata(id: String) extends ProcessableFile
//  case class Text(text: String) extends ProcessableFile
//
//
//  implicit val metadata: RootJsonFormat[Metadata] = jsonFormat1(Metadata)
//  implicit val text: RootJsonFormat[Text] = jsonFormat1(Text)
//  implicit val document: RootJsonFormat[Document1] = jsonFormat2(Document1)
//
//
//  val directory = "/documents/"
//
//  val files = Directory
//    .ls(Paths.get(directory))
//    .flatMapConcat(Directory.ls)
//
//  def constructDocumentFromPaths: Flow[Path, Document, NotUsed] = {
//    Flow.fromGraph(GraphDSL.create() { implicit builder =>
//      import GraphDSL.Implicits._
//
//      val filesSplitter = builder.add(Partition[Path](3, partitionFiles))
//      val merge = builder.add(Merge[Option[ProcessableFile]](3))
//
//      // Process metadata files
//      filesSplitter
//        .out(1) ~> extractFileContentFromPath ~> converter ~> merge.in(0)
//
//      // Process text files
//      filesSplitter
//        .out(1) ~> extractFileContentFromPath ~> parseJson[Text].map(Some(_)) ~> merge.in(1)
//
//      // Simply ignore and merge in any file that isn't a text extraction result
//      filesSplitter
//        .out(2)
//        .log("SkippingFile")
//        .map(_ => None) ~> merge.in(2)
//
//      val documents = merge.out
//        .scan(DocumentAccumulator(None, None))((document, result) => {
//          result match {
//            case Some(metadata: Metadata) =>
//              document.copy(metadata = Some(metadata))
//            case Some(text: Text) =>
//              document.copy(text = Some(text))
//            case _ =>
//              document
//          }
//        })
//        .filter(_.isComplete)
//        .map(Document.apply)
//
//      FlowShape(filesSplitter.in, documents.outlet)
//    })
//  }
//
//  private def converter: Flow[String, Metadata, NotUsed] = Flow[String].map {
//    data => data.parseJson.convertTo[Metadata]
//  }
//  private def extractFileContentFromPath: Flow[Path, String, NotUsed] =
//    Flow[Path]
//      .flatMapConcat(path => {
//        FileIO.fromPath(path).reduce((a, b) => a ++ b)
//      })
//      .map(_.utf8String)
//  case class DocumentAccumulator(metadata: Option[Metadata], text: Option[Text]) {
//    def isComplete: Boolean = metadata.isDefined && text.isDefined
//  }
//
//  case class Document(metadata: Metadata, text: Text)
//
//  object Document {
//    def apply(accumulator: DocumentAccumulator): Document = {
//      if (accumulator.metadata.isEmpty || accumulator.text.isEmpty) {
//        throw new RuntimeException("Unable to create Document from DocumentAccumulator with empty values")
//      }
//      new Document(accumulator.metadata.get, accumulator.text.get)
//    }
//  }
//
//  def partitionFiles(path: Path) = {
//    path.getFileName.toString match {
//      case "metadata.json" => 0
//      case "text.json"     => 1
//      case _               => 2
//    }
//  }
//
//
//}
