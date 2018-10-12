package neuroFilter

object utilz {
  import featureProcessing._
  import java.io.File
  import org.joda.time.DateTime

    implicit class MEAops(mea: MEA){
      def getURIs: List[File] = {
        getInfo.map(_._2)
      }

      def getInfo: List[(DateTime, File)] = {
        import fileIO._
        val recordings = getRecordingsMap
        mea match {
          case MEA2  => recordings ( 2)
          case MEA13 => recordings (13)
          case MEA17 => recordings (17)
          case MEA18 => recordings (18)
          case MEA8  => recordings ( 8)
          case MEA15 => recordings (15)
          case MEA16 => recordings (16)
        }
      }
    }

    implicit class SeqOps[A](a: Seq[A]) {
      def zipIndexLeft: Seq[(Int, A)] = a.zipWithIndex.map{ case(a,b) => (b,a) }
    }

    implicit class MapOps[K,V](m: Map[K,V]) {
      def intersectKeys[V2](that: Map[K,V2]): Map[K,(V,V2)] = {
        for {
          (a, b) <- m
          c <- that.get(a)
        } yield {
          a -> (b,c)
        }
      }
    }

    import scala.io.AnsiColor._
    def toHeat(frame: Seq[Int], treshhold: Int): Seq[Double] = {
      frame.map(_.toDouble/frame.sum.toDouble).map{ d =>
        if(frame.sum < treshhold)
          0.0
        else
          d
      }
    }

    def printPercentageColor(d: Double): String = {
      // println(d)
      if((d == 0.0) || d.isNaN()){
        s"${WHITE_B}${WHITE} ${RESET}"
      }
      else if(d > .40){
        s"${RED_B}${RED} ${RESET}"
      }
      else if(d > .30){
        s"${YELLOW_B}${YELLOW} ${RESET}"
      }
      else
        s"${GREEN_B}${GREEN} ${RESET}"
    }
}
