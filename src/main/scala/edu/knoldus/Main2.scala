package edu.knoldus

import play.api.libs.json.{Format, Json, OFormat}
import ai.x.play.json.Jsonx
import ai.x.play.json.Encoders._
object Main2 extends App {



      case class SampleBiggerClass(
      a1:String,a2:String,a3:String,a4:String,a5:String,a6:String,a7:String,a8:String,a9:String,a10:String,a11:String,a12:String,a13:String,a14:String,a15:String,a16:String,a17:String,a18:String,a19:String,a20:String,a21:String,a22:String,a23:String,a24:String,a25:String,a26:String)


      val sampleBiggerClass = SampleBiggerClass("a1","a2","a3","a4","a5","a6","a7","a8","a9","a10","a11","a12","a13","a14","a15","a16","a17","a18","a19","a20","a21","a22","a23","a24","a25","a26")


      //Create implicit formatter
      implicit val sampleBiggerClassFormatter : Format[SampleBiggerClass] = Jsonx.formatCaseClass[SampleBiggerClass]
      val sampleJsonData = Json.toJson(sampleBiggerClass)
      assert(sampleBiggerClass == sampleJsonData.as[SampleBiggerClass] )
      case class SampleClass(s: String, i: Int = 9)
      implicit lazy val format: OFormat[SampleClass] = Jsonx.formatCaseClassUseDefaults[SampleClass]
      println(assert(SampleClass("akaak",9) == Json.parse("""{"s":"akaaka"}""").validate[SampleClass].get))

}
