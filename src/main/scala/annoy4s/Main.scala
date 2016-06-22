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
import scala.util.Random

trait Annoy extends Library {
  def anCreateEuclidean(f: Int): Pointer
  def anAddItem(ptr: Pointer, item: Int, w: Array[Float])
  def anBuild(ptr: Pointer, q: Int)
  def anSave(ptr: Pointer, filename: String)
  def anLoad(ptr: Pointer, filename: String)
  def anGetNnsByItem(ptr: Pointer, item: Int, n: Int, searchK: Int, result: Array[Int], distances: Array[Float])
  def anGetNItems(ptr: Pointer): Int
  def anVerbose(ptr: Pointer, v: Boolean)
  def anGetItem(ptr: Pointer, item: Int, v: Array[Float])
}

class AnnoyWrapper(f: Int, annoyIndex: Pointer, annoy: Annoy) {
  def addItem(item: Int, w: Array[Float]) = annoy.anAddItem(annoyIndex, item, w)
  def build(q: Int) = annoy.anBuild(annoyIndex, q)
  def save(filename: String) = annoy.anSave(annoyIndex, filename)
  def load(filename: String) = annoy.anLoad(annoyIndex, filename)
  def getNnsByItem(item: Int, n: Int) = {
    val result = Array.fill(n)(-1)
    val distances = Array.fill(n)(-1.0f)
    annoy.anGetNnsByItem(annoyIndex, item, n, -1, result, distances)
    result.toSeq.filter(_ != -1).zip(distances.toSeq)
  }
  def getNItems() = annoy.anGetNItems(annoyIndex)
  def verbose(v: Boolean) = annoy.anVerbose(annoyIndex, v)
  def getItem(item: Int) = {
    val v = Array.fill(f)(0.0f)
    annoy.anGetItem(annoyIndex, item, v)
    v.toSeq
  }
}

object AnnoyWrapper {
  def createEuclidean(f: Int) = {
    val instance = Native.loadLibrary("annoy", classOf[Annoy]).asInstanceOf[Annoy]
    new AnnoyWrapper(f, instance.anCreateEuclidean(f), instance)
  }
}

object Main {
  def main(args: Array[String]) = {
    val annoyWrapper = AnnoyWrapper.createEuclidean(40)
    //println("setting verbose...")
    //annoyWrapper.verbose(true)
    // println("adding...")
    // (0 until 1000).foreach { i =>
    //   val w = Array.fill(40)(Random.nextGaussian().toFloat)
    //   annoyWrapper.addItem(i, w)
    // }
    // println("building...")
    // annoyWrapper.build(10)
    // println("saving...")
    // annoyWrapper.save("testing.annoy")
    
    val annoyWrapper2 = AnnoyWrapper.createEuclidean(40)
    println("loading...")
    annoyWrapper2.load("testing.annoy")
    println("getNItems: " + annoyWrapper2.getNItems())
    println("nns of zero: " + annoyWrapper2.getNnsByItem(0, 30).map(_._1).mkString(","))
  }
}
