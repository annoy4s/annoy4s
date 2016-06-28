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
import org.mapdb._
import scala.io.Source

class Annoy(
  db: DB,
  idToIndex: HTreeMap[Integer, Integer],
  indexToId: HTreeMap[Integer, Integer],
  annoyIndex: Pointer,
  dimension: Int
) {
  def close() = {
    db.close()
    Annoy.annoyLib.deleteIndex(annoyIndex)
  }

  def query(vector: Seq[Float], maxReturnSize: Int) = {
    val result = Array.fill(maxReturnSize)(-1)
    val distances = Array.fill(maxReturnSize)(-1.0f)
    Annoy.annoyLib.getNnsByVector(annoyIndex, vector.toArray, maxReturnSize, -1, result, distances)
    result.toList.filter(_ != -1).map(indexToId.get).map(_.toInt).zip(distances.toSeq)
  }

  def query(id: Int, maxReturnSize: Int) = {
    if (idToIndex.containsKey(id)) {
      val index = idToIndex.get(id)
      val result = Array.fill(maxReturnSize)(-1)
      val distances = Array.fill(maxReturnSize)(-1.0f)
      Annoy.annoyLib.getNnsByItem(annoyIndex, index, maxReturnSize, -1, result, distances)
      Some(result.toList.filter(_ != -1).map(indexToId.get).map(_.toInt).zip(distances.toSeq))
    } else None
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
    val memoryMode = outputDir == null
    
    if (!memoryMode) {
      require(File(outputDir).notExists || File(outputDir).isEmpty, "Output directory is not empty.")
      File(outputDir).createIfNotExists(true)
    }
    
    def inputLines = Source.fromFile(inputFile).getLines
    
    val db = if (!memoryMode) {
      DBMaker.fileDB((File(outputDir) / "mapping").toJava).make()
    } else {
      DBMaker.memoryDB().make()
    }
    
    val idToIndex = db.hashMap("idToIndex", Serializer.INTEGER, Serializer.INTEGER).create()
    inputLines.map(_.split(" ").head.toInt).zipWithIndex.foreach {
      case (id, index) =>
        if (verbose) {
          print(s"idToIndex: ${index}\r")
        }
        idToIndex.put(id, index)
    }
    db.commit()
    if (verbose) {
      println()
    }
    
    val indexToId = db.hashMap("indexToId", Serializer.INTEGER, Serializer.INTEGER).create()
    inputLines.map(_.split(" ").head.toInt).zipWithIndex.foreach {
      case (id, index) =>
        if (verbose) {
          print(s"indexToId: ${index}\r")
        }
        indexToId.put(index, id)
    }
    db.commit()

    val dimension = inputLines.next.split(" ").tail.size
    val annoyIndex = metric match {
      case Angular => annoyLib.createAngular(dimension)
      case Euclidean => annoyLib.createEuclidean(dimension)
    }
    
    annoyLib.verbose(annoyIndex, verbose)
    
    inputLines
      .map(_.split(" "))
      .foreach { seq =>
        val id = seq.head.toInt
        val index = idToIndex.get(id)
        val vector = seq.tail.map(_.toFloat)
        annoyLib.addItem(annoyIndex, index, vector)
      }
    
    annoyLib.build(annoyIndex, numOfTrees)
    
    if (memoryMode) {
      new Annoy(db, idToIndex, indexToId, annoyIndex, dimension)
    } else {
      db.close()
      annoyLib.save(annoyIndex, (File(outputDir) / "annoy-index").pathAsString)
      annoyLib.deleteIndex(annoyIndex)
      (File(outputDir) / "dimension").overwrite(dimension.toString)
      load(outputDir, metric)
    }
  }

  def load(
    annoyDir: String,
    metric: Metric = Angular
  ): Annoy = {
    val db = DBMaker.fileDB((File(annoyDir) / "mapping").toJava).readOnly().closeOnJvmShutdown().make()
    val idToIndex = db.hashMap("idToIndex", Serializer.INTEGER, Serializer.INTEGER).open()
    val indexToId = db.hashMap("indexToId", Serializer.INTEGER, Serializer.INTEGER).open()
    
    val dimension = (File(annoyDir) / "dimension").lines.head.toInt
    val annoyIndex = metric match {
      case Angular => annoyLib.createAngular(dimension)
      case Euclidean => annoyLib.createEuclidean(dimension)
    }
    annoyLib.load(annoyIndex, (File(annoyDir) / "annoy-index").pathAsString)
    
    new Annoy(db, idToIndex, indexToId, annoyIndex, dimension)
  }
}

sealed trait Metric
case object Angular extends Metric
case object Euclidean extends Metric
