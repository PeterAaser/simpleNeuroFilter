package neuroFilter

import fs2.{ Pull, Pure, Segment, Stream }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.concurrent.duration._
import scala.io.Source
import cats.implicits._
import fs2._

import java.nio.file.{ Path, Paths }

object main {

  type Matrix[A] = List[List[A]]
  type Channel = Int
  type SpikeT = Int

  // This ends up not being that much data, so it's OK

  def notmain(args: Array[String]): Unit = {

    import fileIO._
    val recordings = getRecordingsMap

    import filters._
    import scala.io.AnsiColor._

    val timeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH-mm-ss")
    val AIDS_DAY = DateTime.parse("2017-09-11T00-00-00", timeFormat)

    import scala.collection.JavaConversions._

    import org.deeplearning4j.datasets.iterator._
    import org.deeplearning4j.datasets.iterator.impl._
    import org.deeplearning4j.nn.api._
    import org.deeplearning4j.nn.multilayer._
    import org.deeplearning4j.nn.graph._
    import org.deeplearning4j.nn.conf._
    import org.deeplearning4j.nn.conf.inputs._
    import org.deeplearning4j.nn.conf.layers._
    import org.deeplearning4j.nn.weights._
    import org.deeplearning4j.optimize.listeners._
    import org.deeplearning4j.datasets.datavec.RecordReaderMultiDataSetIterator
    import org.deeplearning4j.eval.Evaluation

    import org.nd4j.linalg.learning.config._ // for different updaters like Adam, Nesterovs, etc.
    import org.nd4j.linalg.activations.Activation // defines different activation functions like RELU, SOFTMAX, etc.
    import org.nd4j.linalg.lossfunctions.LossFunctions // mean squared error, multiclass cross entropy, etc.

    import org.deeplearning4j.datasets.iterator.impl.EmnistDataSetIterator

    val batchSize = 16 // how many examples to simultaneously train in the network
    val emnistSet = EmnistDataSetIterator.Set.BALANCED
    val emnistTrain = new EmnistDataSetIterator(emnistSet, batchSize, true)
    val emnistTest = new EmnistDataSetIterator(emnistSet, batchSize, false)

    val outputNum = EmnistDataSetIterator.numLabels(emnistSet) // total output classes
    val rngSeed = 123 // integer for reproducability of a random number generator
    val numRows = 28 // number of "pixel rows" in an mnist digit
    val numColumns = 28

    println(s"outputNum, $outputNum")

    val conf = new NeuralNetConfiguration.Builder()
      .seed(rngSeed)
      .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
      .updater(new Adam())
      .l2(1e-4)
      .list()
      .layer(new DenseLayer.Builder()
               .nIn(numRows * numColumns) // Number of input datapoints.
               .nOut(1000) // Number of output datapoints.
               .activation(Activation.RELU) // Activation function.
               .weightInit(WeightInit.XAVIER) // Weight initialization.
               .build())
      .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
               .nIn(1000)
               .nOut(outputNum)
               .activation(Activation.SOFTMAX)
               .weightInit(WeightInit.XAVIER)
               .build())
      .pretrain(false).backprop(true)
      .build()

    // create the MLN
    val network = new MultiLayerNetwork(conf)
    network.init()

    // pass a training listener that reports score every 10 iterations
    val eachIterations = 5
    network.addListeners(new ScoreIterationListener(eachIterations))

    // fit a dataset for a single epoch
    network.fit(emnistTrain)

    // fit for multiple epochs
    // val numEpochs = 2
    // network.fit(new MultipleEpochsIterator(numEpochs, emnistTrain)

    val eval = network.evaluate(emnistTest)
    println(s"accuracy: ${eval.accuracy()}")
    println(s"precision: ${eval.precision()}")
    println(s"recall: ${eval.recall()}")
    eval.precision()
    eval.recall()

    // evaluate ROC and calculate the Area Under Curve
    val roc = network.evaluateROCMultiClass(emnistTest)

    roc.calculateAverageAUC()

    val classIndex = 0
    roc.calculateAUC(classIndex)

    print(eval.stats())
    print(roc.stats())
  }
}
