package org.apache.spark.examples.h2o

import java.io.File

import hex.deeplearning.DeepLearning
import hex.deeplearning.DeepLearningModel.DeepLearningParameters
import org.apache.spark.{SparkFiles, SparkContext}
import org.apache.spark.examples.h2o.DemoUtils.configure
import org.apache.spark.examples.h2o.DemoUtils.addFiles
import org.apache.spark.h2o.{DoubleHolder, H2OContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import water.fvec.DataFrame


object DeepLearningDemo {

  def main(args: Array[String]): Unit = {
    // Create Spark context which will drive computation.
    val conf = configure("Sparkling Water: Deep Learning on Airlines data")
    val sc = new SparkContext(conf)
    addFiles(sc, "examples/smalldata/allyears2k_headers.csv.gz")

    // Run H2O cluster inside Spark cluster
    val h2oContext = new H2OContext(sc).start()
    import h2oContext._

    //
    // Load H2O from CSV file (i.e., access directly H2O cloud)
    // Use super-fast advanced H2O CSV parser !!!
    val airlinesData = new DataFrame(new File(SparkFiles.get("allyears2k_headers.csv.gz")))

    //
    // Use H2O to RDD transformation
    //
    val airlinesTable : RDD[Airlines] = asRDD[Airlines](airlinesData)
    println(s"\n===> Number of all flights via RDD#count call: ${airlinesTable.count()}\n")
    println(s"\n===> Number of all flights via H2O#Frame#count: ${airlinesData.numRows()}\n")

    //
    // Filter data with help of Spark SQL
    //

    val sqlContext = new SQLContext(sc)
    import sqlContext._ // import implicit conversions
    airlinesTable.registerTempTable("airlinesTable")

    // Select only interesting columns and flights with destination in SFO
    val query = "SELECT * FROM airlinesTable WHERE Dest LIKE 'SFO'"
    val result = sql(query) // Using a registered context and tables
    println(s"\n===> Number of flights with destination in SFO: ${result.count()}\n")

    //
    // Run Deep Learning
    //

    println("\n====> Running DeepLearning on the result of SQL query\n")
    // Configure Deep Learning algorithm
    val dlParams = new DeepLearningParameters()
    // Use result of SQL query
    // Note: there is implicit conversion from RDD->DataFrame->Key
    dlParams._train = result( 'Year, 'Month, 'DayofMonth, 'DayOfWeek, 'CRSDepTime, 'CRSArrTime,
                              'UniqueCarrier, 'FlightNum, 'TailNum, 'CRSElapsedTime, 'Origin, 'Dest,
                              'Distance, 'IsDepDelayed )
    dlParams._response_column = 'IsDepDelayed
    //dlParams.classification = true


    val dl = new DeepLearning(dlParams)
    val dlModel = dl.trainModel.get

    //
    // Use model for scoring
    //
    println("\n====> Making prediction with help of DeepLearning model\n")
    val predictionH2OFrame = dlModel.score(result)('predict)
    val predictionsFromModel = asRDD[DoubleHolder](predictionH2OFrame).collect.map ( _.result.getOrElse("NaN") )
    println(predictionsFromModel.mkString("\n===> Model predictions: ", ", ", ", ...\n"))

    // Stop Spark cluster and destroy all executors
    if (System.getProperty("spark.ext.h2o.preserve.executors")==null) {
      sc.stop()
    }
    // This will block in cluster mode since we have H2O launched in driver
  }

}
