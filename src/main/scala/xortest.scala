package neuroFilter

import org.deeplearning4j.eval.Evaluation
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.distribution.UniformDistribution
import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Sgd
import org.nd4j.linalg.lossfunctions.LossFunctions

/**
  * This basic example shows how to manually create a DataSet and train it to an
  * basic Network.
  * <p>
  * The network consists in 2 input-neurons, 1 hidden-layer with 4
  * hidden-neurons, and 2 output-neurons.
  * <p>
  * I choose 2 output neurons, (the first fires for false, the second fires for
  * true) because the Evaluation class needs one neuron per classification.
  *
  * @author Peter Großmann
  */
object XorExample {
  def notmain(args: Array[String]) {

    // def putScalarAt(indices: Int*): Double => INDArray => INDArray = { a => nd =>
    //   val indiceArray = indices.toArray
    //   nd.putScalar(indiceArray, a)
    // }

    implicit class NDext(nd: INDArray) {
      def putScalarAtIndex(indices: Int*): Double => Unit = d =>
        nd.putScalar(indices.toArray, d)
    }


    // list off input values, 4 training samples with data for 2
    // input-neurons each
    val input = Nd4j.zeros(4.toLong, 2.toLong)

    // correspondending list with expected output values, 4 training samples
    // with data for 2 output-neurons each
    val labels = Nd4j.zeros(4.toLong, 2.toLong)

    // create first dataset
    // when first input=0 and second input=0
    input.putScalarAtIndex(0,0)(0.0)
    input.putScalarAtIndex(0,1)(0.0)
    labels.putScalarAtIndex(0,0)(1.0)
    labels.putScalarAtIndex(0,1)(0.0)

    input.putScalarAtIndex(1,0)(1.0)
    input.putScalarAtIndex(1,1)(0.0)
    labels.putScalarAtIndex(1,0)(0.0)
    labels.putScalarAtIndex(1,1)(1.0)

    input.putScalarAtIndex(2,0)(0.0)
    input.putScalarAtIndex(2,1)(1.0)
    labels.putScalarAtIndex(2,0)(0.0)
    labels.putScalarAtIndex(2,1)(1.0)

    input.putScalarAtIndex(3,0)(1.0)
    input.putScalarAtIndex(3,1)(1.0)
    labels.putScalarAtIndex(3,0)(1.0)
    labels.putScalarAtIndex(3,1)(0.0)

    // create dataset object
    val ds = new DataSet(input, labels)

    // Set up network configuration
    val builder = new NeuralNetConfiguration.Builder
    builder.updater(new Sgd(0.1))
    builder.seed(123)
    builder.biasInit(0)
    builder.miniBatch(false)


    ///???
    builder.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)

    // create a multilayer network with 2 layers (including the output
    // layer, excluding the input payer)
    val listBuilder = builder.list

    val hiddenLayerBuilder = new DenseLayer.Builder
    hiddenLayerBuilder.nIn(2)
    hiddenLayerBuilder.nOut(4)
    hiddenLayerBuilder.activation(Activation.SIGMOID)
    hiddenLayerBuilder.weightInit(WeightInit.DISTRIBUTION)
    hiddenLayerBuilder.dist(new UniformDistribution(0, 1))

    listBuilder.layer(0, hiddenLayerBuilder.build)

    val outputLayerBuilder: OutputLayer.Builder = new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
    outputLayerBuilder.nIn(4)
    outputLayerBuilder.nOut(2)
    outputLayerBuilder.activation(Activation.SOFTMAX)
    outputLayerBuilder.weightInit(WeightInit.DISTRIBUTION)
    outputLayerBuilder.dist(new UniformDistribution(0, 1))
    listBuilder.layer(1, outputLayerBuilder.build)

    // no pretrain phase for this network
    listBuilder.pretrain(false)

    // seems to be mandatory
    // according to agibsonccc: You typically only use that with
    // pretrain(true) when you want to do pretrain/finetune without changing
    // the previous layers finetuned weights that's for autoencoders and
    // rbms
    listBuilder.backprop(true)

    // build and init the network, will check if everything is configured
    // correct
    val conf = listBuilder.build
    val net = new MultiLayerNetwork(conf)
    net.init()

    // add an listener which outputs the error every 100 parameter updates
    net.setListeners(new ScoreIterationListener(100))

    // C&P from GravesLSTMCharModellingExample
    // Print the number of parameters in the network (and for each layer)
    val layers = net.getLayers
    var totalNumParams = 0
    for (i <- layers.indices) {
      val nParams: Int = layers(i).numParams
      println("Number of parameters in layer " + i + ": " + nParams)
      totalNumParams += nParams
    }
    println("Total number of network parameters: " + totalNumParams)

    // here the actual learning takes place
    for(i <- 0 until 10000)
      net.fit(ds)


    // create output for every training sample
    val output: INDArray = net.output(ds.getFeatures)

    println("input:")
    println(ds.getFeatures)
    println("output:")
    println(output)

    // let Evaluation prints stats how often the right output had the
    // highest value
    val eval: Evaluation = new Evaluation(2)
    eval.eval(ds.getLabels, output)
    println("eval stats")
    println(eval.stats)
  }

}
