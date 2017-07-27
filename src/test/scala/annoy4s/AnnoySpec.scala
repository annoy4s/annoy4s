// Copyright (c) 2016 pishen
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package annoy4s

import better.files._
import org.scalatest._

import scala.io.Source

class AnnoySpec extends FlatSpec with Matchers {
  
  def getEuclideanInputFile = {
    val inputFile = File.newTemporaryFile()
    inputFile.toJava.deleteOnExit()
    inputFile.appendLines(Seq(
      "10 1.0 1.0",
      "11 2.0 1.0",
      "12 2.0 2.0",
      "13 3.0 2.0"
    ):_*)
    
    inputFile
  }
  
  def checkEuclideanResult(res: Option[Seq[(Int, Float)]]) = {
    res.get.map(_._1) shouldBe Seq(10, 11, 12, 13)
    res.get.map(_._2).zip(Seq(0.0f, 1.0f, 1.414f, 2.236f)).foreach{
      case (a, b) => a shouldBe b +- 0.001f
    }
  }
  
  "Annoy" should "create/load and query Euclidean file index" in {
    val inputFile = getEuclideanInputFile
    
    val outputDir = File.newTemporaryDirectory()
    
    val annoy = Annoy.create[Int](inputFile.pathAsString, 10, outputDir.pathAsString, Euclidean)
    checkEuclideanResult(annoy.query(10, 4))

    annoy.close()

    val annoyReload = Annoy.load[Int](outputDir.pathAsString)
    checkEuclideanResult(annoyReload.query(10, 4))

    annoyReload.close()
    outputDir.delete()
  }

  it should "create and query Euclidean memory index" in {
    val inputFile = getEuclideanInputFile

    val annoy = Annoy.create[Int](inputFile.pathAsString, 10, metric = Euclidean)
    checkEuclideanResult(annoy.query(10, 4))
  }

  def getAngularInputFile = {
    val inputFile = File.newTemporaryFile()
    inputFile.toJava.deleteOnExit()
    inputFile.appendLines(Seq(
      "10 2.0 0.0",
      "11 1.0 1.0",
      "12 0.0 3.0",
      "13 -5.0 0.0"
    ):_*)

    inputFile
  }

  def checkAngularResult(res: Option[Seq[(Int, Float)]]) = {
    res.get.map(_._1) shouldBe Seq(10, 11, 12, 13)
    res.get.map(_._2).zip(Seq(0.0f, 0.765f, 1.414f, 2.0f)).foreach{
      case (a, b) => a shouldBe b +- 0.001f
    }
  }

  it should "create/load and query Angular file index" in {
    val inputFile = getAngularInputFile

    val outputDir = File.newTemporaryDirectory()

    val annoy = Annoy.create[Int](inputFile.pathAsString, 10, outputDir.pathAsString, Angular)
    checkAngularResult(annoy.query(10, 4))

    annoy.close()

    val annoyReload = Annoy.load[Int](outputDir.pathAsString)
    checkAngularResult(annoyReload.query(10, 4))

    annoyReload.close()
    outputDir.delete()
  }

  it should "create and query Angular memory index" in {
    val inputFile = getAngularInputFile

    val annoy = Annoy.create[Int](inputFile.pathAsString, 10, metric = Angular)
    checkAngularResult(annoy.query(10, 4))
  }

  def getStringAngularInputFile = {
    val inputFile = File.newTemporaryFile()
    inputFile.toJava.deleteOnExit()
    inputFile.appendLines(Seq(
      "a 2.0 0.0",
      "b 1.0 1.0",
      "c 0.0 3.0",
      "d -5.0 0.0"
    ):_*)

    inputFile
  }

  def checkStringAngularResult(res: Option[Seq[(String, Float)]]) = {
    res.get.map(_._1) shouldBe Seq("a", "b", "c", "d")
    res.get.map(_._2).zip(Seq(0.0f, 0.765f, 1.414f, 2.0f)).foreach{
      case (a, b) => a shouldBe b +- 0.001f
    }
  }

  it should "create/load and query Angular file index with a String as a key" in {
    val inputFile = getStringAngularInputFile

    val outputDir = File.newTemporaryDirectory()

    val annoy = Annoy.create[String](inputFile.pathAsString, 10, outputDir.pathAsString, Angular)
    checkStringAngularResult(annoy.query("a", 4))

    annoy.close()

    val annoyReload = Annoy.load[String](outputDir.pathAsString)
    checkStringAngularResult(annoyReload.query("a", 4))

    annoyReload.close()
    outputDir.delete()
  }

  it should "return more accurate results for a higher search_K" in {

    val tmpFile = File.newTemporaryFile()
    tmpFile.toJava.deleteOnExit()

    tmpFile
      .appendLines(Source
        .fromInputStream(getClass.getResourceAsStream("/searchk-test-vector"))
        .getLines().toSeq: _*)

    val index = Annoy.create[Int](tmpFile.pathAsString, numOfTrees = 2)

    index.query(1, maxReturnSize = 10, searchK = 2).get.map(_._1) shouldBe List(1, 54, 55, 60, 76, 8, 32, 33)
    index.query(1, maxReturnSize = 10, searchK = -1).get.map(_._1) shouldBe List(1, 69, 39, 87, 54, 29, 62, 55, 21, 35)

  }

}
