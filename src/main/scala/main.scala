package neuroFilter

import fs2.{ Pull, Pure, Segment, Stream }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.concurrent.duration._
import scala.io.Source
import cats.implicits._

import java.nio.file.{ Path, Paths }

object main {


  type Matrix[A] = List[List[A]]
  type Channel = Int
  type SpikeT = Int



  // This ends up not being that much data, so it's OK
  def readNeuroFile(filename: String): List[(Int, FiniteDuration)] = {
    println(s"reading $filename")
    Source.fromFile(filename).getLines
      .filter(!_.isEmpty())
      .toList.tail
      .map(x => (x ++ "0").split(","))
      .map(_.dropRight(1))
      .map(_.map{ x =>
             if(x.isEmpty()) None
             else Some(x)
           }
             .map(_.map(_.stripMargin))
             .map(_.map(_.toDouble))
             .map(_.map(x => (x*1000*1000*1000).toLong))
             .map(_.map(x => Duration(x, NANOSECONDS)))
             .zipWithIndex
             .map{ case (x,idx) => x.map(d => (idx, d)) }
             .toList
      )
      .transpose
      .flatMap(_.flatten)
      .filterNot{ case(idx, ts) => idx >= 59 }
      .sortBy(_._2)
  }


  def main(args: Array[String]): Unit = {


    import fileIO._
    val recordings = getRecordingsMap

    import filters._
    import frameFilter._
    import scala.io.AnsiColor._

    // diseased
    val mea8 = 8
    val mea15 = 15
    val mea16 = 16

    // control
    val mea2 = 2
    val mea18 = 18
    val mea13 = 13
    val mea17 = 17

    val timeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH-mm-ss")
    val AIDS_DAY = DateTime.parse("2017-09-11T00-00-00", timeFormat)

    println("Insert MEA number\nDiseased:\t[8, 14(detached), 15, 16]\nContol:\t[2, 13, 17, 18]")
    val mea = readLine("").toInt
    println()
    val bucketSize = readLine("How many milliseconds per interval?").toInt.millis
    println()
    val interval = readLine(s"Input interval. With input 5 the interval will be ${5*bucketSize}ms").toInt

    for(i <- 0 until 100)
      println("########################")

    recordings(mea).map(x => (x._1, x._2.toString())) foreach { case(t, recording) =>

      if(AIDS_DAY isBefore t){
        println(s"${scala.io.AnsiColor.RED_B}---------------------------------------")
        println(s"${scala.io.AnsiColor.RED_B}---------------------------------------")
        println(s"${scala.io.AnsiColor.RED_B}---------------------------------------")
        println(s"${scala.io.AnsiColor.RED_B}---------------------------------------")
        println(s"${scala.io.AnsiColor.RESET}")
      }
      else{
        println("---------------------------------------")
        println("---------------------------------------")
        println("---------------------------------------")
        println("---------------------------------------")
      }

      val huh: Stream[Pure,(Int,FiniteDuration)] = Pull.output(Segment.seq(readNeuroFile(recording))).stream

      val monthString = t.toString("MMM")
      println(s"${t.getDayOfMonth} $monthString:")

      val what = huh
        .through(applyBucketing(bucketSize))
        .through(toFrames).chunkLimit(1024).flatMap(Stream.chunk)
        .through(toSteps)
        .through(foldFrames(interval)).toList.head

      what.foreach { w =>
        println(w.map(x => toHeat(x, 3)).transpose.map(_.map(printPercentageColor).mkString).mkString("\n"))
        println(s"${RESET}\n")
      }

    }
  }
}
