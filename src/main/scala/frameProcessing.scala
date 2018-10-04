package neuroFilter

import filters._

object frameFilter {

  import scala.io.AnsiColor._


  def toHeat(frame: Seq[Int], treshhold: Int): Seq[Double] = {
    frame.map(_.toDouble/frame.sum.toDouble).map{ d =>
      if(frame.sum < treshhold)
        0.0
      else
        d
    }
  }

  // unfuck me plz
  def printPercentageColor(d: Double): String = {
    // println(d)
    if((d == 0.0) || d.isNaN()){
      s"${WHITE_B}${WHITE} ${RESET}"
    }
    else if(d > .35){
      s"${RED_B}${RED} ${RESET}"
    }
    else if(d > .25){
      s"${YELLOW_B}${YELLOW} ${RESET}"
    }
    else
      s"${GREEN_B}${GREEN} ${RESET}"
  }
}
