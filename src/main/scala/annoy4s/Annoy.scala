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

import java.util.UUID

import annoy4s.Converters.KeyConverter
import better.files._
import com.sun.jna._

import scala.io.Source

class Annoy[T](
  idToIndex: Map[T, Int],
  indexToId: Seq[T],
  annoyIndex: Pointer,
  dimension: Int
) {
  def close() = {
    Annoy.annoyLib.deleteIndex(annoyIndex)
  }

  def query(vector: Seq[Float], maxReturnSize: Int): Seq[(T, Float)] = query(vector, maxReturnSize, -1)

  def query(vector: Seq[Float], maxReturnSize: Int, searchK: Int) = {
    val result = Array.fill(maxReturnSize)(-1)
    val distances = Array.fill(maxReturnSize)(-1.0f)
    Annoy.annoyLib.getNnsByVector(annoyIndex, vector.toArray, maxReturnSize, searchK, result, distances)
    result.toList.filter(_ != -1).map(indexToId.apply).zip(distances.toSeq)
  }

  def query(id: T, maxReturnSize: Int): Option[Seq[(T, Float)]] = query(id, maxReturnSize, -1)

  def query(id: T, maxReturnSize: Int, searchK: Int) = {
    idToIndex.get(id).map { index =>
      val result = Array.fill(maxReturnSize)(-1)
      val distances = Array.fill(maxReturnSize)(-1.0f)
      Annoy.annoyLib.getNnsByItem(annoyIndex, index, maxReturnSize, searchK, result, distances)
      result.toList.filter(_ != -1).map(indexToId.apply).zip(distances.toSeq)
    }
  }
}

object Converters {

  trait KeyConverter[T] {
    def convert(key: String): T
  }

  object KeyConverter {

    implicit val intConverter: KeyConverter[Int] = new KeyConverter[Int] {
      override def convert(key: String): Int = key.toInt
    }

    implicit val stringConverter: KeyConverter[String] = new KeyConverter[String] {
      override def convert(key: String): String = key
    }

    implicit val uuidConverter: KeyConverter[UUID] = new KeyConverter[UUID] {
      override def convert(key: String): UUID = UUID.fromString(key)
    }
  }

}

object Annoy {

  val annoyLib = Native.loadLibrary("annoy", classOf[AnnoyLibrary]).asInstanceOf[AnnoyLibrary]

  def create[T](
    inputFile: String,
    numOfTrees: Int,
    outputDir: String = null,
    metric: Metric = Angular,
    verbose: Boolean = false
  )(implicit converter: KeyConverter[T]): Annoy[T] = {
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
          val vector = seq.tail.map(_.toFloat)
          annoyLib.addItem(annoyIndex, index, vector)
      }

    annoyLib.build(annoyIndex, numOfTrees)

    if (diskMode) {
      (File(outputDir) / "ids").printLines(inputLines.map(_.split(" ").head))
      (File(outputDir) / "dimension").overwrite(dimension.toString)
      (File(outputDir) / "metric").overwrite {
        metric match {
          case Angular => "Angular"
          case Euclidean => "Euclidean"
        }
      }
      annoyLib.save(annoyIndex, (File(outputDir) / "annoy-index").pathAsString)
      annoyLib.deleteIndex(annoyIndex)
      load[T](outputDir)
    } else {
      val keys = inputLines.map(entry => converter.convert(entry.split(" ").head)).toSeq
      new Annoy[T](
        keys.zipWithIndex.toMap,
        keys,
        annoyIndex,
        dimension
      )
    }
  }

  def load[T](annoyDir: String)(implicit converter: KeyConverter[T]): Annoy[T] = {
    val ids = File(annoyDir) / "ids"
    val keys = ids.lineIterator.toSeq.map(converter.convert)
    val idToIndex: Map[T, Int] = keys .zipWithIndex.toMap
    val indexToId: Seq[T] = keys
    val dimension = (File(annoyDir) / "dimension").lines.head.toInt
    val annoyIndex = (File(annoyDir) / "metric").lines.head match {
      case "Angular" => annoyLib.createAngular(dimension)
      case "Euclidean" => annoyLib.createEuclidean(dimension)
    }
    annoyLib.load(annoyIndex, (File(annoyDir) / "annoy-index").pathAsString)
    new Annoy[T](idToIndex, indexToId, annoyIndex, dimension)
  }
}

sealed trait Metric
case object Angular extends Metric
case object Euclidean extends Metric
