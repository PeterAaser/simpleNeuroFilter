package neuroFilter

import cats.effect._
import fs2._
import java.nio.file.{ Path, Paths }
import org.joda.time.DateTime
import scala.collection.immutable.Queue
import scala.concurrent.duration._


object filters {

  type Channel = Int
  type SpikeT = Int
  type Matrix[A] = List[List[A]]
  type Frame = Vector[Vector[Int]]
  type SpikePair = (Channel, FiniteDuration)


  // Buckets a stream into timesteps
  def applyBucketing[F[_]](bucketSize: FiniteDuration): Pipe[F, SpikePair, (Channel, SpikeT)] =
    _.mapChunks{c => c.map{ case(idx, dur) => (idx, (dur/bucketSize).toInt) }.toSegment }


  // For each spike returns the channel and stream of events following the spike
  def getFrameStreams[F[_]](interval: FiniteDuration, resolution: FiniteDuration
  ): Pipe[F, SpikePair, Stream[F,(Channel, Frame)]] = ins => {

    val framesize = (interval/resolution).toInt

    // Buckets the incoming stream and generates a frame depicting what happens just after
    // a spike occurred
    def toFrame(start: FiniteDuration, channel: Channel): Pipe[F,(Channel, FiniteDuration), (Channel, Frame)] = { ins =>

      // println(s"to frame called with start: $start and channel: $channel")
      val ay = ins.map{ case(c, t) =>
        (c, t - start)
      }
        .through(applyBucketing(resolution))

      val acc = Array.ofDim[Int](59, framesize)
      def go(s: Stream[F,(Int,Int)]): Pull[F,(Channel, Frame),Unit] = {
        s.pull.uncons.flatMap {
          case Some((seg, tl)) => {
            seg.force.foreach { case(channel, spikeT) =>
              acc(channel)(spikeT) += 1
            }
            go(tl)
          }
          case None => {
            Pull.output1((channel, acc.map(_.toVector).toVector))
          }
        }
      }

      go(ay).stream
    }

    def go(s: Stream[F,SpikePair]): Pull[F, Stream[F,(Channel,Frame)], Unit] = {
      s.pull.uncons1.flatMap {
        case Some((hd, tl)) => {
          Pull.output1(tl.takeWhile{ case(_, nextT) => nextT < hd._2 + interval }
            .through(toFrame(hd._2, hd._1))) >> go(tl)
        }
        case None => Pull.done
      }
    }

    go(ins).stream
  }


  def foldFrames[F[_]: Sync](framesize: Int): Pipe[F, Stream[F,(Channel, Frame)], Map[Channel, Frame]] = { ins =>

    // no idea why type inference is so fucked here
    def coalesce(a: Frame, b: Frame): Frame = {
      (a zip b).map{ case(a,b) => a zip b}.map(_.map{ case(a,b) => a+b})
    }

    val acc = Array.ofDim[Frame](59)
    for(ii <- 0 until 59)
      acc(ii) = Vector.fill(59)(Vector.fill(framesize)(0))

    import utilz._

    // side effecting bullshit
    val fill = ins.flatMap{ xs =>
      xs.map{ case(channel, frame) =>
        println(channel)
        acc(channel) = coalesce(acc(channel), frame)
      }
    }.compile.drain

    Stream.eval(fill) >>
      Stream(acc.toList.zipIndexLeft.toMap).covary[F]
  }


  def processRecording[F[_]: Sync](
    path: String,
    resolution: FiniteDuration,
    interval: FiniteDuration): Stream[F,Map[Channel, Frame]] = {

    val spikes = fileIO.readNeuroFile(path)
    val spikeStream: Stream[F, SpikePair] = Pull.output(fs2.Segment.seq(spikes)).stream

    spikeStream
      .through(getFrameStreams(300.millis, 30.millis))
      .through(foldFrames(10))
  }
}
