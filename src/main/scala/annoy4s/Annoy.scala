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

import com.sun.jna._
import better.files._
import scala.io.Source

class Annoy(
  idToIndex: Map[Int, Int],
  indexToId: Seq[Int],
  annoyIndex: Pointer,
  dimension: Int
) {
  def close() = {
    Annoy.annoyLib.deleteIndex(annoyIndex)
  }

  def query(vector: Seq[Float], maxReturnSize: Int) = {
    val result = Array.fill(maxReturnSize)(-1)
    val distances = Array.fill(maxReturnSize)(-1.0f)
    Annoy.annoyLib.getNnsByVector(annoyIndex, vector.toArray, maxReturnSize, -1, result, distances)
    result.toList.filter(_ != -1).map(indexToId.apply).zip(distances.toSeq)
  }

  def query(id: Int, maxReturnSize: Int) = {
    idToIndex.get(id).map { index =>
      val result = Array.fill(maxReturnSize)(-1)
      val distances = Array.fill(maxReturnSize)(-1.0f)
      Annoy.annoyLib.getNnsByItem(annoyIndex, index, maxReturnSize, -1, result, distances)
      result.toList.filter(_ != -1).map(indexToId.apply).zip(distances.toSeq)
    }
  }
}

object Annoy {
  val annoyLib = Native.loadLibrary("annoy", classOf[AnnoyLibrary]).asInstanceOf[AnnoyLibrary]
  
  def create(
    inputFile: String,
    numOfTrees: Int,
    outputDir: String = null,
    metric: Metric = Angular,
    verbose: Boolean = false
  ): Annoy = {
    val diskMode = outputDir != null
    
    if (diskMode) {
      require(File(outputDir).notExists || File(outputDir).isEmpty, "Output directory is not empty.")
      File(outputDir).createIfNotExists(true)
    }
    
    def inputLines = Source.fromFile(inputFile).getLines

    val dimension = inputLines.next.split(" ").tail.size
    val annoyIndex = metric match {
      case Angular => annoyLib.createAngular(dimension)
      case Euclidean => annoyLib.createEuclidean(dimension)
    }
    
    annoyLib.verbose(annoyIndex, verbose)
    
    inputLines
      .map(_.split(" "))
      .zipWithIndex
      .foreach {
        case (seq, index) =>
          val id = seq.head.toInt
          val vector = seq.tail.map(_.toFloat)
          annoyLib.addItem(annoyIndex, index, vector)
      }
    
    annoyLib.build(annoyIndex, numOfTrees)
    
    if (diskMode) {
      (File(outputDir) / "ids").overwrite("").appendLines(inputLines.map(_.split(" ").head).toSeq:_*)
      (File(outputDir) / "dimension").overwrite(dimension.toString)
      (File(outputDir) / "metric").overwrite {
        metric match {
          case Angular => "Angular"
          case Euclidean => "Euclidean"
        }
      }
      annoyLib.save(annoyIndex, (File(outputDir) / "annoy-index").pathAsString)
      annoyLib.deleteIndex(annoyIndex)
      load(outputDir)
    } else {
      new Annoy(
        inputLines.map(_.split(" ").head.toInt).zipWithIndex.toMap,
        inputLines.map(_.split(" ").head.toInt).toSeq,
        annoyIndex,
        dimension
      )
    }
  }

  def load(annoyDir: String): Annoy = {
    val ids = (File(annoyDir) / "ids")
    val idToIndex = ids.lines.map(_.toInt).toSeq.zipWithIndex.toMap
    val indexToId = ids.lines.map(_.toInt).toSeq
    
    val dimension = (File(annoyDir) / "dimension").lines.head.toInt
    val annoyIndex = (File(annoyDir) / "metric").lines.head match {
      case "Angular" => annoyLib.createAngular(dimension)
      case "Euclidean" => annoyLib.createEuclidean(dimension)
    }
    annoyLib.load(annoyIndex, (File(annoyDir) / "annoy-index").pathAsString)
    
    new Annoy(idToIndex, indexToId, annoyIndex, dimension)
  }
}

sealed trait Metric
case object Angular extends Metric
case object Euclidean extends Metric
