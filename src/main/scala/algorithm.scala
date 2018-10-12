package neuroFilter

import cats.effect.IO
import fs2.{ Pull, Pure, Segment, Stream }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.concurrent.duration._
import scala.io.Source
import cats.implicits._

import java.nio.file.{ Path, Paths }


object algorithm {

  def main(args: Array[String]): Unit = {

    import fileIO._
    val recordings = getRecordingsMap

    import filters._
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

    val spikes = fileIO.readNeuroFile( fileIO.getRecordingsMap(mea2).last._2.toString() )

    val spikeStream: Stream[IO, SpikePair] = Pull.output(fs2.Segment.seq(spikes)).stream

    println(spikeStream
      .through(getFrameStreams(300.millis, 30.millis))
      .through(foldFrames(10)).compile.toList.unsafeRunSync().map(_.map(_._2.mkString("\n")).mkString("\n\n")).mkString("\n")
    )

  }
}
