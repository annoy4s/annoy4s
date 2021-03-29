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

import annoy4s.Converters.KeyConverter
import better.files._
import org.scalatest._

import scala.collection.immutable
import scala.io.Source
import scala.util.Random

class AnnoySpec extends FlatSpec with Matchers {

  type InputVectors = Seq[String]

  private val euclideanInputLines = Seq(
    "10 1.0 1.0",
    "11 2.0 1.0",
    "12 2.0 2.0",
    "13 3.0 2.0"
  )

  private val angularInputLines = Seq(
    "10 2.0 0.0",
    "11 1.0 1.0",
    "12 0.0 3.0",
    "13 -5.0 0.0"
  )

  private val stringAngularInputLines = Seq(
    "a 2.0 0.0",
    "b 1.0 1.0",
    "c 0.0 3.0",
    "d -5.0 0.0"
  )

  private val manhattanInputLines = Seq(
    "10 2.0 0.0",
    "11 1.0 1.0",
    "12 0.0 3.0",
    "13 -5.0 0.0"
  )

  private val hammingInputLines = Seq(
    "a 1 0 0 0",
    "b 1 1 0 0",
    "c 1 1 0 1",
    "d 0 1 1 1"
  )

  private def getInputDimension(inputVectors: InputVectors): Int = inputVectors.head.split(" ").length - 1

  private def getIds(inputVectors: InputVectors): Seq[String] = inputVectors.map(_.split(" ").head)

  private def checkAnnoy[T](annoy: Annoy[T], inputVectors: InputVectors, metric: Metric)(implicit converter: KeyConverter[T]): Unit = {
    annoy.ids shouldBe getIds(inputVectors).map(key => converter.convert(key))
    annoy.dimension shouldBe getInputDimension(inputVectors)
    annoy.metric shouldBe metric
  }

  private def getTestInputFile(inputVectors: InputVectors): File = {
    val inputFile = File.newTemporaryFile()
    inputFile.toJava.deleteOnExit()
    inputFile.appendLines(inputVectors: _*)
    inputFile
  }


  def checkEuclideanResult(res: Option[Seq[(Int, Float)]]) = {
    res.get.map(_._1) shouldBe Seq(10, 11, 12, 13)
    res.get.map(_._2).zip(Seq(0.0f, 1.0f, 1.414f, 2.236f)).foreach {
      case (a, b) => a shouldBe b +- 0.001f
    }
  }

  "Annoy" should "create/load and query Euclidean file index" in {
    val inputFile = getTestInputFile(euclideanInputLines)

    val outputDir = File.newTemporaryDirectory()

    val annoy = Annoy.create[Int](inputFile.pathAsString, 10, outputDir.pathAsString, Euclidean)
    checkEuclideanResult(annoy.query(10, 4))
    checkAnnoy(annoy, euclideanInputLines, Euclidean)

    annoy.close()

    val annoyReload = Annoy.load[Int](outputDir.pathAsString)
    checkAnnoy(annoyReload, euclideanInputLines, Euclidean)
    checkEuclideanResult(annoyReload.query(10, 4))

    annoyReload.close()
    outputDir.delete()
  }

  it should "create and query Euclidean memory index" in {
    val inputFile = getTestInputFile(euclideanInputLines)

    val annoy = Annoy.create[Int](inputFile.pathAsString, 10, metric = Euclidean)
    checkEuclideanResult(annoy.query(10, 4))
  }

  def checkAngularResult(res: Option[Seq[(Int, Float)]]) = {
    res.get.map(_._1) shouldBe Seq(10, 11, 12, 13)
    res.get.map(_._2).zip(Seq(0.0f, 0.765f, 1.414f, 2.0f)).foreach {
      case (a, b) => a shouldBe b +- 0.001f
    }
  }

  it should "create/load and query Angular file index" in {
    val inputFile = getTestInputFile(angularInputLines)

    val outputDir = File.newTemporaryDirectory()

    val annoy = Annoy.create[Int](inputFile.pathAsString, 10, outputDir.pathAsString, Angular)
    checkAngularResult(annoy.query(10, 4))
    checkAnnoy(annoy, angularInputLines, Angular)
    annoy.close()

    val annoyReload = Annoy.load[Int](outputDir.pathAsString)
    checkAnnoy(annoyReload, angularInputLines, Angular)
    checkAngularResult(annoyReload.query(10, 4))

    annoyReload.close()
    outputDir.delete()
  }

  it should "create and query Angular memory index" in {
    val inputFile = getTestInputFile(angularInputLines)

    val annoy = Annoy.create[Int](inputFile.pathAsString, 10, metric = Angular)
    checkAnnoy(annoy, angularInputLines, Angular)
    checkAngularResult(annoy.query(10, 4))
  }

  def checkStringAngularResult(res: Option[Seq[(String, Float)]]) = {
    res.get.map(_._1) shouldBe Seq("a", "b", "c", "d")
    res.get.map(_._2).zip(Seq(0.0f, 0.765f, 1.414f, 2.0f)).foreach {
      case (a, b) => a shouldBe b +- 0.001f
    }
  }

  it should "create/load and query Angular file index with a String as a key" in {
    val inputFile = getTestInputFile(stringAngularInputLines)

    val outputDir = File.newTemporaryDirectory()

    val annoy = Annoy.create[String](inputFile.pathAsString, 10, outputDir.pathAsString, Angular)
    checkStringAngularResult(annoy.query("a", 4))
    checkAnnoy(annoy, stringAngularInputLines, Angular)
    annoy.close()

    val annoyReload = Annoy.load[String](outputDir.pathAsString)
    checkStringAngularResult(annoyReload.query("a", 4))
    checkAnnoy(annoyReload, stringAngularInputLines, Angular)
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

    index.query(1, maxReturnSize = 10, searchK = 2).get.map(_._1) shouldBe List(1, 69, 39, 87, 54, 29, 62, 55, 21, 35)
    index.query(1, maxReturnSize = 10, searchK = -1).get.map(_._1) shouldBe List(1, 69, 39, 87, 54, 36, 29, 43, 97, 21)

  }

  def checkManhattanResult(res: Option[Seq[(Int, Float)]]) = {
    res.get.map(_._1) shouldBe Seq(10, 11, 12, 13)
    res.get.map(_._2).zip(Seq(0.0f, 2.0f, 5.0f, 7.0f)).foreach {
      case (a, b) => a shouldBe b +- 0.001f
    }
  }

  it should "create/load and query Manhattan file index" in {
    val inputFile = getTestInputFile(manhattanInputLines)

    val outputDir = File.newTemporaryDirectory()

    val annoy = Annoy.create[Int](inputFile.pathAsString, 10, outputDir.pathAsString, Manhattan)
    checkManhattanResult(annoy.query(10, 4))
    checkAnnoy(annoy, manhattanInputLines, Manhattan)
    annoy.close()

    val annoyReload = Annoy.load[Int](outputDir.pathAsString)
    checkManhattanResult(annoyReload.query(10, 4))
    checkAnnoy(annoyReload, manhattanInputLines, Manhattan)

    annoyReload.close()
    outputDir.delete()
  }

  it should "return the vector for a given, previously loaded, id" in {

    def randomVector: immutable.Seq[Float] = (0 until 30).map(_ => Random.nextFloat())

    val map: Map[Int, immutable.Seq[Float]] = (0 until 10).map(id => id -> randomVector).toMap

    val inputFile = File.newTemporaryFile()
    inputFile.toJava.deleteOnExit()
    inputFile.appendLines(map.toSeq.map {
      case (id, vector) => s"$id ${vector.mkString(" ")}"
    }: _*)

    val index = Annoy.create[Int](inputFile.pathAsString, numOfTrees = 10)

    map.foreach {
      case (id, vector) => index.getItem(id) shouldBe Some(vector)
    }

    index.getItem(-1) shouldBe None

  }

  // Hamming tests

  def checkHammingResult(res: Option[Seq[(String, Float)]]) = {
    res.get.map(_._1) shouldBe Seq("a", "b", "c", "d")
    res.get.map(_._2).zip(Seq(0.0f, 1.0f, 2.0f, 4.0f)).foreach {
      case (a, b) => a shouldBe b
    }
  }

  it should "create/load and query Hamming file index" in {

    val inputFile = getTestInputFile(hammingInputLines)

    val outputDir = File.newTemporaryDirectory()

    val annoy = Annoy.create[String](inputFile.pathAsString, 10, outputDir.pathAsString, Hamming)
    checkHammingResult(annoy.query("a", 4))
    checkAnnoy(annoy, hammingInputLines, Hamming)
    annoy.close()

    val annoyReload = Annoy.load[String](outputDir.pathAsString)
    checkHammingResult(annoyReload.query("a", 4))
    checkAnnoy(annoyReload, hammingInputLines, Hamming)

    annoyReload.close()
    outputDir.delete()

  }

}
