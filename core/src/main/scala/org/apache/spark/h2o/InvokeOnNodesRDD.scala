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

import org.apache.spark.{TaskContext, Partition, SparkContext}
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.h2o.H2OContextUtils.NodeDesc

/** Special kind of RDD which is used to invoke code on all executors detected in cluster. */
private[h2o]
class InvokeOnNodesRDD(nodes:Seq[NodeDesc], sc: SparkContext) extends RDD[NodeDesc](sc, Nil) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[(String, String, Int)] = {
    Iterator.single(split.asInstanceOf[PartitionWithNodeInfo].nodeDesc)
  }

  override protected def getPartitions: Array[Partition] = nodes.zip(0 until nodes.length ).map( n =>
    new PartitionWithNodeInfo(n._1) {
      override def index: Int = n._2
    }).toArray


  override protected def getPreferredLocations(split: Partition): Seq[String] =
    Seq(split.asInstanceOf[PartitionWithNodeInfo].nodeDesc._2)

  abstract class PartitionWithNodeInfo(val nodeDesc: NodeDesc) extends Partition
}
