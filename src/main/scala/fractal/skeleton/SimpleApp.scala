package fractal.skeleton

import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.{ Duration, FiniteDuration }
import java.io.File
import java.nio.file.{ Path, Paths }
import scala.io.Source


object neuroHM {

  type Matrix[A] = List[List[A]]

  def collectByTimestamp(
    start: FiniteDuration,
    interval: FiniteDuration,
    timestamps: List[FiniteDuration]): (List[FiniteDuration], List[FiniteDuration]) = {

    val viable = timestamps.dropWhile(_ < start)
    val (a,b) = (
      viable.takeWhile(ts => (ts < (start + interval))),
      viable.dropWhile(ts => (ts < (start + interval))))

    (a.map(x => x - start), viable)
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
    Source.fromFile("/home/peteraa/Downloads/bigbig.csv").getLines
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


  def bucketByInterval(interval: FiniteDuration, bucketInterval: FiniteDuration): Matrix[FiniteDuration] => Matrix[Int] = {
    val nBuckets = (interval/bucketInterval).toInt

    def bucketChannel(channel: List[FiniteDuration]): List[Int] = {
      val buckets = Array.ofDim[Int](nBuckets)
      channel.map(x => (x/bucketInterval).toInt).foreach(idx => buckets(idx) = buckets(idx) + 1)
      buckets.toList
    }


    matrix => matrix.map(bucketChannel)
  }


  def collapseSeries(interval: FiniteDuration, bucketInterval: FiniteDuration, series: List[Matrix[FiniteDuration]]): Matrix[Int] = {
    val nBuckets = (interval/bucketInterval).toInt
    val temp = Array.ofDim[Int](59, nBuckets)

    series.map(bucketByInterval(interval, bucketInterval)).foreach { m =>
      for(ii <- 0 until 59){
        for(jj <- 0 until nBuckets){
          temp(ii)(jj) = temp(ii)(jj) + m(ii)(jj)
        }
      }
    }
    temp.map(_.toList).toList
  }


  def main(args: Array[String]): Unit = {
    val ins = collectMatrix

    val interval = Duration(5, MILLISECONDS)
    val bucketInterval = Duration(1, MILLISECONDS)

    println(s"With BucketSize: interval: ${interval.toMillis}ms, bucketSize: ${bucketInterval.toMillis}ms, channel: 0")
    for(i <- 0 until 59){
      println(s"channel $i")
      val testan = collectChannelMatrixes(interval, i, ins)
      val collapsed = collapseSeries(interval, bucketInterval, testan).transpose
      val s = collapsed.map(_.map(x => f"${x}%4d")).map(_.mkString(",")).mkString("\n")
      println(s)
    }
  }
}
