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
    println(s"from ${filenameDateString.takeWhile(x => x != 'M')} I got\t$ts")
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
}
