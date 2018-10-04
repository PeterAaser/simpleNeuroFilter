package neuroFilter

import cats.effect._
import fs2._
import java.nio.file.{ Path, Paths }
import scala.collection.immutable.Queue
import scala.concurrent.duration._

object filters {

  type Channel = Int
  type SpikeT = Int
  type Matrix[A] = List[List[A]]

  def applyBucketing[F[_]](bucketSize: FiniteDuration): Pipe[F,(Int,FiniteDuration), (Int,Int)] =
    _.mapChunks{c => c.map{ case(idx, dur) => (idx, (dur/bucketSize).toInt) }.toSegment }


  // Input ordered by SpikeT
  def toFrames[F[_]]: Pipe[F,(Channel,SpikeT),(SpikeT, Vector[Int])] = {
    var acc = Array.ofDim[Int](59)
    def go(s: Stream[F,(Channel,SpikeT)], ts: SpikeT): Pull[F,(SpikeT, Vector[Int]), Unit] = {
      s.pull.uncons1.flatMap {
        case Some(((idx, t), tl)) if t == ts => {
          acc(idx) = acc(idx) + 1; go(tl, ts)
        }
        case Some(((idx, t), tl)) => {
          acc(idx) = acc(idx) + 1
          val frame = acc.toVector; acc = Array.ofDim(59); Pull.output1((ts, frame)) >> go(tl, t)
        }
        case None => Pull.done
      }
    }
    in => go(in, 0).stream
  }

  def toSteps[F[_]]: Pipe[F,(SpikeT,Vector[Int]),Vector[Int]] = {
    val empty = Vector.fill(59)(0)
    def go(s: Stream[F,(SpikeT,Vector[Int])], ts: Int): Pull[F,Vector[Int],Unit] =
      s.pull.uncons1.flatMap {
        case Some(((t, spikes),tl)) if t <= ts => {
          Pull.output1(spikes) >> go(tl,ts + 1)
        }
        case Some(((t, spikes),tl)) => {
          Pull.output1(empty) >> go(tl.cons(Segment((t, spikes))),ts + 1)
        }
        case None => Pull.done
      }
    in => go(in, 0).stream
  }


  def foldFrames[F[_]](interval: Int): Pipe[F,Vector[Int], List[Matrix[Int]]] = {

    val acc = Array.ofDim[Int](59, 59, interval)

    def go(s: Stream[F,Queue[Vector[Int]]]): Pull[F,List[Matrix[Int]],Unit] = {
      s.pull.uncons1.flatMap {
        case Some((spikes,tl)) => {
          (spikes.head zipWithIndex).foreach{ case (spike, idx) if spike > 0 =>
            for(ii <- 0 until 59)
              for(kk <- 1 until interval){
                acc(idx)(ii)(kk) += spikes(kk)(ii)
              }
            case _ => {}
          }
          go(tl)
        }
        case None => Pull.output1(acc.map(_.map(_.tail.toList).toList).toList)
      }
    }

    in => go(in.sliding(interval)).stream
  }
}
