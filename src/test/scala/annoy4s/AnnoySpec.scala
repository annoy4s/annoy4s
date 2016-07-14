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

import org.scalatest._
import better.files._

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
    
    val annoy = Annoy.create(inputFile.pathAsString, 10, outputDir.pathAsString, Euclidean)
    checkEuclideanResult(annoy.query(10, 4))
    
    annoy.close()
    
    val annoyReload = Annoy.load(outputDir.pathAsString)
    checkEuclideanResult(annoyReload.query(10, 4))

    annoyReload.close()
    outputDir.delete()
  }
  
  it should "create and query Euclidean memory index" in {
    val inputFile = getEuclideanInputFile
    
    val annoy = Annoy.create(inputFile.pathAsString, 10, metric = Euclidean)
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
    
    val annoy = Annoy.create(inputFile.pathAsString, 10, outputDir.pathAsString, Angular)
    checkAngularResult(annoy.query(10, 4))
    
    annoy.close()
    
    val annoyReload = Annoy.load(outputDir.pathAsString)
    checkAngularResult(annoyReload.query(10, 4))

    annoyReload.close()
    outputDir.delete()
  }
  
  it should "create and query Angular memory index" in {
    val inputFile = getAngularInputFile
    
    val annoy = Annoy.create(inputFile.pathAsString, 10, metric = Angular)
    checkAngularResult(annoy.query(10, 4))
  }
}
