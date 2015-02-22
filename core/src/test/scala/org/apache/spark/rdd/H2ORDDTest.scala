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
package org.apache.spark.rdd

import org.apache.spark.SparkContext
import org.apache.spark.h2o.util.SharedSparkTestContext
import org.apache.spark.h2o.{DoubleHolder, IntHolder, StringHolder}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import water.fvec.{DataFrame, Vec}
import water.parser.{Categorical, ValueString}

/**
 * Testing schema for h2o schema rdd transformation.
 */
// FIXME this should be only trait but used in different SparkContext
@RunWith(classOf[JUnitRunner])
class H2ORDDTest extends FunSuite with SharedSparkTestContext {

  override def createSparkContext: SparkContext = new SparkContext("local[*]", "test-local")

  test("RDD[IntHolder] to DataFrame and back") {

    val rdd = sc.parallelize(1 to 1000, 100).map( v => IntHolder(Some(v)))
    val dataFrame:DataFrame = hc.createDataFrame(rdd)

    assertBasicInvariants(rdd, dataFrame, (row, vec) => {
      val row1 = row + 1
      val value = vec.at8(row) // value stored at row-th
      assert (row1 == value, "The DataFrame values should match row numbers+1")
    })
    // Clean up
    dataFrame.delete()
    rdd.unpersist()
  }

  test("RDD[DoubleHolder] to DataFrame and back") {

    val rdd = sc.parallelize(1 to 1000, 100).map( v => DoubleHolder(Some(v)))
    val dataFrame:DataFrame = hc.createDataFrame(rdd)

    assertBasicInvariants(rdd, dataFrame, (row, vec) => {
      val row1 = row + 1
      val value = vec.at(row) // value stored at row-th
      // Using == since int should be mapped strictly to doubles
      assert (row1 == value, "The DataFrame values should match row numbers+1")
    })
    // Clean up
    dataFrame.delete()
    rdd.unpersist()
  }

  test("RDD[StringHolder] to DataFrame[Enum] and back") {

    val rdd = sc.parallelize(1 to 1000, 100).map( v => StringHolder(Some(v.toString)))
    val dataFrame:DataFrame = hc.createDataFrame(rdd)

    assert (dataFrame.vec(0).isEnum, "The vector type should be enum")
    assert (dataFrame.vec(0).domain().length == 1000, "The vector domain should be 1000")

    assertBasicInvariants(rdd, dataFrame, (row, vec) => {
      val dom = vec.domain()
      val value = dom(vec.at8(row).asInstanceOf[Int]) // value stored at row-th
      // Using == since int should be mapped strictly to doubles
      assert (row+1 == value.toInt, "The DataFrame values should match row numbers")
    })
    // Clean up
    dataFrame.delete()
    rdd.unpersist()
  }

  test("RDD[StringHolder] to DataFrame[String] and back") {

    val rdd = sc.parallelize(1 to (Categorical.MAX_ENUM_SIZE + 1), 100).map( v => StringHolder(Some(v.toString)))
    val dataFrame:DataFrame = hc.createDataFrame(rdd)

    assert (dataFrame.vec(0).isString, "The vector type should be string")
    assert (dataFrame.vec(0).domain() == null, "The vector should have null domain")
    val valueString = new ValueString()
    assertBasicInvariants(rdd, dataFrame, (row, vec) => {
      val row1 = (row + 1).toString
      val value = vec.atStr(valueString, row) // value stored at row-th
      // Using == since int should be mapped strictly to doubles
      assert (row1.equals(value.toString), "The DataFrame values should match row numbers+1")
    })
    // Clean up
    dataFrame.delete()
    rdd.unpersist()
  }

  private type RowValueAssert = (Long, Vec) => Unit

  private def assertBasicInvariants[T<:Product](rdd: RDD[T], df: DataFrame, rowAssert: RowValueAssert): Unit = {
    assertHolderProperties(df)
    assert (rdd.count == df.numRows(), "Number of rows in DataFrame and RDD should match")
    // Check numbering
    val vec = df.vec(0)
    var row = 0
    while(row < df.numRows()) {
      assert (!vec.isNA(row), "The DataFrame should not contain any NA values")
      rowAssert (row, vec)
      row += 1
    }
  }

  private def assertHolderProperties(df: DataFrame): Unit = {
    assert (df.numCols() == 1, "DataFrame should contain single column")
    assert (df.names().length == 1, "DataFrame column names should have single value")
    assert (df.names()(0).equals("result"),
      "DataFrame column name should be 'result' since Holder object was used to define RDD")
  }
}
