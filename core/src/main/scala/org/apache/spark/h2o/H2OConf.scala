/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.spark.h2o

import org.apache.spark.SparkConf

/**
 * Just simple configuration holder which is representing
 * properties passed from user to H2O App.
 */
trait H2OConf {

  /* Require Spar config */
  private[spark] def sparkConf:SparkConf
  // Precondition
  require(sparkConf != null, "sparkConf was null")

  /* Initialize configuration */
  // Collect configuration properties
  def numH2OWorkers = sparkConf.getInt(PROP_CLUSTER_SIZE._1, PROP_CLUSTER_SIZE._2)
  def useFlatFile   = sparkConf.getBoolean(PROP_USE_FLATFILE._1, PROP_USE_FLATFILE._2)
  def basePort      = sparkConf.getInt(PROP_PORT_BASE._1, PROP_PORT_BASE._2)
  def incrPort      = sparkConf.getInt(PROP_PORT_INCR._1, PROP_PORT_INCR._2)
  def cloudTimeout  = sparkConf.getInt(PROP_CLOUD_TIMEOUT._1, PROP_CLOUD_TIMEOUT._2)
  def drddMulFactor = sparkConf.getInt(PROP_DUMMY_RDD_MUL_FACTOR._1, PROP_DUMMY_RDD_MUL_FACTOR._2)
  def numRddRetries = sparkConf.getInt(PROP_SPREADRDD_RETRIES._1, PROP_SPREADRDD_RETRIES._2)
  def cloudName     = sparkConf.get(PROP_CLOUD_NAME._1, PROP_CLOUD_NAME._2)
  def defaultCloudSize  = sparkConf.getInt(PROP_DEFAULT_CLUSTER_SIZE._1, PROP_DEFAULT_CLUSTER_SIZE._2)
  def h2oNodeLogLevel   = sparkConf.get(PROP_NODE_LOG_LEVEL._1, PROP_NODE_LOG_LEVEL._2)
  def h2oClientLogLevel = sparkConf.get(PROP_CLIENT_LOG_LEVEL._1, PROP_CLIENT_LOG_LEVEL._2)
  def networkMask   = sparkConf.getOption(PROP_NETWORK_MASK._1)
  def nthreads      = sparkConf.getInt(PROP_NTHREADS._1, PROP_NTHREADS._2)

  /* Configuration properties */

  /** Configuration property - use flatfile for H2O cloud formation. */
  val PROP_USE_FLATFILE = ( "spark.ext.h2o.flatfile", true)
  /** Configuration property - expected number of workers of H2O cloud.
    * Value -1 means automatic detection of cluster size.
    */
  val PROP_CLUSTER_SIZE = ( "spark.ext.h2o.cluster.size", -1 )
  /** Configuration property - base port used for individual H2O nodes configuration. */
  val PROP_PORT_BASE = ( "spark.ext.h2o.port.base", 54321 )
  /** Configuration property - increment added to base port to find available port. */
  val PROP_PORT_INCR = ( "spark.ext.h2o.port.incr", 2)
  /** Configuration property - timeout for cloud up. */
  val PROP_CLOUD_TIMEOUT = ("spark.ext.h2o.cloud.timeout", 60*1000)
  /** Configuration property - number of retries to create an RDD spreat over all executors */
  val PROP_SPREADRDD_RETRIES = ("spark.ext.h2o.spreadrdd.retries", 10)
  /** Configuration property - name of H2O cloud */
  val PROP_CLOUD_NAME = ("spark.ext.h2o.cloud.name", "sparkling-water-")
  /** Starting size of cluster in case that size is not explicitelly passed */
  val PROP_DEFAULT_CLUSTER_SIZE = ( "spark.ext.h2o.default.cluster.size,", 20)
  /* H2O internal log level for launched remote nodes. */
  val PROP_NODE_LOG_LEVEL = ("spark.ext.h2o.node.log.level", "INFO")
  /** H2O log leve for client running in Spark driver */
  val PROP_CLIENT_LOG_LEVEL = ("spark.ext.h2o.client.log.level", "WARN")
  /** Subnet selector for h2o if IP guess fail - useful if 'spark.ext.h2o.flatfile' is false
    * and we are trying to guess right IP on mi*/
  val PROP_NETWORK_MASK = ("spark.ext.h2o.network.mask", null.asInstanceOf[String])
  /* Limit for number of threads used by H2O, default -1 means unlimited */
  val PROP_NTHREADS = ("spark.ext.h2o.nthreads", -1)


  /** Configuration property - multiplication factor for dummy RDD generation.
    * Size of dummy RDD is PROP_CLUSTER_SIZE*PROP_DUMMY_RDD_MUL_FACTOR */
  val PROP_DUMMY_RDD_MUL_FACTOR = ("spark.ext.h2o.dummy.rdd.mul.factor", 10)

  /**
   * Produce arguments for H2O node based on this config.
   * @return array of H2O launcher command line arguments
   */
  def getH2ONodeArgs:Array[String] = (getH2OCommonOptions ++ Seq("-log_level", h2oNodeLogLevel)).toArray

  /**
   * Get arguments for H2O client.
   * @return array of H2O client arguments.
   */
  def getH2OClientArgs:Array[String] = (getH2OCommonOptions ++ Seq("-log_level", h2oClientLogLevel)).toArray

  private def getH2OCommonOptions:Seq[String] =
    Seq(
      ("-name", cloudName),
      ("-nthreads", if (nthreads>0) nthreads else null),
      ("-network", networkMask.getOrElse(null)))
      .filter(x => x._2 != null)
      .flatMap(x => Seq(x._1, x._2.toString))

  override def toString: String =
    s"""Sparkling Water configuration:
         |  workers      : $numH2OWorkers
         |  cloudName    : $cloudName
         |  flatfile     : $useFlatFile
         |  basePort     : $basePort
         |  incrPort     : $incrPort
         |  cloudTimeout : $cloudTimeout
         |  h2oNodeLog   : $h2oNodeLogLevel
         |  h2oClientLog : $h2oClientLogLevel
         |  nthreads     : $nthreads
         |  drddMulFactor: $drddMulFactor""".stripMargin

}
