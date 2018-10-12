package neuroFilter

import java.io.File
import org.joda.time.DateTime
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import scala.concurrent.duration.FiniteDuration

import utilz._

/**
  Here we take neurodata and put them into feature vectors etc
  */
object featureProcessing {

  sealed trait MEA
  case object MEA2 extends MEA
  case object MEA13 extends MEA
  case object MEA17 extends MEA
  case object MEA18 extends MEA

  sealed trait AIDS
  case object MEA8 extends MEA with AIDS
  case object MEA15 extends MEA with AIDS
  case object MEA16 extends MEA with AIDS

  val MEAs = List(MEA2, MEA13, MEA17, MEA18, MEA8, MEA15, MEA16)


  import filters._

  // frames is a list where idx = channel and inner idx is steps after spike for Channel
  case class ProcessedChannels(
    mea: MEA,
    date: DateTime,
    frames: Map[Channel, Seq[Seq[Int]]]){

    def catenated: Map[Channel, Seq[Int]] = {
      val init = frames.values.toList.head
      frames.values.toList.tail.foldLeft(init){ case(acc, channels) =>
        (acc zip channels).map{ case (a,b) =>
          a ++ b
        }
      }.zipIndexLeft.toMap
    }

    def catenatedMeans: Map[Channel, Double] =
      this.catenated.mapValues(xs => xs.sum.toDouble/xs.size.toDouble)

    def catenatedVariances: Map[Channel, Double] = {
      this.catenated.intersectKeys(catenatedMeans)
        .mapValues { case(xs, mean) =>
          xs.foldLeft(0.0){ case(acc, n) =>
            math.pow(n.toDouble - mean, 2) + acc
          }/(xs.size.toDouble)
        }
    }

    def printMe: Unit = {
      println("frames")
      println(frames(0).map(_.mkString("\t")).mkString("\n"))
      println("---")
      println(frames(1).map(_.mkString("\t")).mkString("\n"))
      println("---")
      println(frames(2).map(_.mkString("\t")).mkString("\n"))
      println("---")

      println("means")
      println(catenatedMeans(0))
      println(catenatedMeans(1))
      println(catenatedMeans(2))

      println("variances")
      println(catenatedVariances(0))
      println(catenatedVariances(1))
      println(catenatedVariances(2))
    }
  }
  object ProcessedChannels {
    def apply(m: MEA, d: DateTime, f: List[List[List[Int]]]): ProcessedChannels =
      ProcessedChannels(m,d,f.zipIndexLeft.toMap)
  }


  class experiment(interval: FiniteDuration, resolution: FiniteDuration) {
    val recordings: Map[MEA, List[(DateTime, File)]] = MEAs.map(m => (m, m.getInfo)).toMap

    import cats.implicits._
    import cats.effect.IO

    def getFirst(m: MEA): ProcessedChannels = {
      val info = recordings(m).head
      val hurr = filters.processRecording[IO](info._2.toString(), resolution, interval)
      .compile.toList.unsafeRunSync().head

      ProcessedChannels(m, DateTime.now(), hurr)
    }

    def getLast(m: MEA): ProcessedChannels = {
      val info = recordings(m).last
      val hurr = filters.processRecording[IO](info._2.toString(), resolution, interval)
        .compile.toList.unsafeRunSync().head

      ProcessedChannels(m, DateTime.now(), hurr)
    }
  }
}
