package neuroFilter

import cats.effect._
import fs2._
import java.io.File
import java.nio.file.{ Path, Paths }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.collection.immutable.Queue
import scala.concurrent.duration._

object fileIO {


  def getListOfFiles(dir: String): List[File] =
    (new File(dir)).listFiles.filter(_.isFile).toList

  def getListOfFiles(dir: Path): List[File] =
    dir.toFile().listFiles.filter(_.isFile).toList


  def getListOfFolders(dir: String): List[File] =
    (new File(dir)).listFiles.filter(_.isDirectory).toList

  def getListOfFolders(dir: Path): List[File] =
    dir.toFile().listFiles.filter(_.isDirectory).toList


  def parseDate(filenameDateString: String): DateTime = {
    val fsTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH-mm-ss")
    val month = filenameDateString.drop(5).take(2).toInt
    val ts = DateTime.parse(filenameDateString.takeWhile(x => x != 'M'), fsTimeFormat)
    val ts2 = ts.withMonthOfYear(month)
    ts2

  }

  def parseMEAnum(filename: String): Int = {
    filename.dropWhile(x => x != 'A')
      .drop(1)
      .takeWhile(x => (x != ' ') && (x != '.'))
      .toInt
  }

  def getRecordings: List[(DateTime, Int, File)] = {
    getListOfFiles("/home/peteraa/SpikeData")
      .map(f => (parseDate(f.getName.toString()), parseMEAnum(f.getName.toString()), f))
  }

  def getRecordingsMap: Map[Int, List[(DateTime, File)]] = {

    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

    val huh = getRecordings.groupBy(_._2).toList
    val hur = huh.map(x => (x._1, x._2.sortBy(_._1).map(x => (x._1, x._3))))
    hur.toMap
  }


  def readNeuroFile(filename: String): List[(Int, FiniteDuration)] = {
    println(s"reading $filename")
    scala.io.Source.fromFile(filename).getLines
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


  def say(word: Any)(implicit filename: sourcecode.File, line: sourcecode.Line): Unit = {
    val fname = filename.value.split("/").last
    println(Console.YELLOW + s"[${fname}: ${sourcecode.Line()}]" + Console.RESET + s" - $word")
  }
}
