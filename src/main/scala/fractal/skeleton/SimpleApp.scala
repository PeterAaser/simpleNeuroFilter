package fractal.skeleton

import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.{ Duration, FiniteDuration }
import java.io.File
import java.nio.file.{ Path, Paths }
import scala.io.Source


object neuroHM {

  def printMatrix(m: Matrix[FiniteDuration]): Unit = {
    println(m.map(_.map(_.toMillis.toInt).mkString("\n["," ","]")).mkString(""))
  }

  def printPart(m: List[FiniteDuration], n: Int): Unit = {
    println(m.take(n).map(_.toMillis.toInt).mkString("","ms ","ms"))
  }

  type Matrix[A] = List[List[A]]

  def collectByTimestamp(
    start: FiniteDuration,
    interval: FiniteDuration,
    timestamps: List[FiniteDuration]): (List[FiniteDuration], List[FiniteDuration]) = {

    val viable = timestamps.dropWhile(_ < start)
    val (a,b) = (
      viable.takeWhile(ts => (ts < (start + interval))),
      viable.dropWhile(ts => (ts < (start + interval))))

    (a.map(x => x - start), b)
  }


  def collectAllChannelsByTimestamp(
    start: FiniteDuration,
    interval: FiniteDuration,
    channels: Matrix[FiniteDuration]): (Matrix[FiniteDuration], Matrix[FiniteDuration]) = {

    val durations = channels.map(channel => collectByTimestamp(start, interval, channel))
    val (as, bs) = (durations.map(_._1), durations.map(_._2))

    (as, bs)
  }


  def collectChannelMatrixes(
    interval: FiniteDuration,
    index: Int,
    channels: Matrix[FiniteDuration]): List[Matrix[FiniteDuration]] = {

    channels(index).foldLeft((List[Matrix[FiniteDuration]](), channels)){ case((accumulator, remainder), stamp) =>
      val(inRange, nextRemainder) = collectAllChannelsByTimestamp(stamp, interval, remainder)
      (inRange :: accumulator, nextRemainder)
    }._1.reverse
  }


  def collectMatrix: Matrix[FiniteDuration] = {
    Source.fromFile("/home/peter/Downloads/bigbig.csv").getLines
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
             .map(_.map(x => (x*1000*1000).toLong))
             .map(_.map(x => Duration(x, NANOSECONDS)))
             .toList
      )
      .transpose
      .map(_.flatten)
  }

  def main(args: Array[String]): Unit = {
    val ins = collectMatrix
    val testan = collectChannelMatrixes(Duration(5, MILLISECONDS), 0, ins)
    printMatrix(testan.head)
  }
}
