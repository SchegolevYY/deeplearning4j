package org.deeplearning4j.spark.ml.impl

import java.util

import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util._
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.sql.{DataFrame, Row}
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.optimize.api.IterationListener
import org.deeplearning4j.spark.impl.multilayer.SparkDl4jMultiLayer
import org.deeplearning4j.spark.ml.utils.{DatasetFacade, ParamSerializer}
import org.nd4j.linalg.api.ndarray.INDArray


final class SparkDl4jNetwork(
                            override val multiLayerConfiguration: MultiLayerConfiguration,
                            override val numLabels: Int,
                            override val trainingMaster: ParamSerializer,
                            override val epochs : Int,
                            override val listeners: util.Collection[IterationListener],
                            override val collectStats: Boolean = false,
                            override val uid: String = Identifiable.randomUID("dl4j"))
    extends SparkDl4jNetworkWrapper[Vector, SparkDl4jNetwork, SparkDl4jModel](
        uid, multiLayerConfiguration, numLabels, trainingMaster, epochs, listeners, collectStats
    ) {

    def this(multiLayerConfiguration: MultiLayerConfiguration, numLabels: Int, trainingMaster: ParamSerializer, epochs: Int,
             listeners: util.Collection[IterationListener]) {
        this(multiLayerConfiguration, numLabels, trainingMaster, epochs, listeners, false, Identifiable.randomUID("dl4j"))
    }

    def this(multiLayerConfiguration: MultiLayerConfiguration, numLabels: Int, trainingMaster: ParamSerializer, epochs: Int,
             listeners: util.Collection[IterationListener], collectStats: Boolean) {
        this(multiLayerConfiguration, numLabels, trainingMaster, epochs, listeners, collectStats, Identifiable.randomUID("dl4j"))
    }

    override val mapVectorFunc: (Row) => LabeledPoint = row => new LabeledPoint(row.getAs[Double]($(labelCol)), row.getAs[Vector]($(featuresCol)))

    override def train(dataset: DataFrame): SparkDl4jModel = {
        val spn = trainer(DatasetFacade.dataRows(dataset))
        new SparkDl4jModel(uid, spn)
    }
}

class SparkDl4jModel(override val uid: String, network: SparkDl4jMultiLayer)
    extends SparkDl4jModelWrapper[Vector, SparkDl4jModel](uid, network) {

    override def copy(extra: ParamMap) : SparkDl4jModel = copyValues(new SparkDl4jModel(uid, network)).setParent(parent)

    override def predict(features: Vector) : Double = predictor(features)

    override def outputFlattenedTensor(vector: Vector): Vector = super.outputFlattenedTensor(vector)

    override def outputTensor(vector: Vector) : INDArray = super.outputTensor(vector)

}

object SparkDl4jModel extends SparkDl4jModelWrap
