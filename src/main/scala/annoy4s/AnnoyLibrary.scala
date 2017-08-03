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

trait AnnoyLibrary extends Library {
  def createAngular(f: Int): Pointer
  def createEuclidean(f: Int): Pointer
  def createManhattan(f: Int): Pointer
  def deleteIndex(ptr: Pointer): Unit
  def addItem(ptr: Pointer, item: Int, w: Array[Float]): Unit
  def build(ptr: Pointer, q: Int): Unit
  def save(ptr: Pointer, filename: String): Boolean
  def unload(ptr: Pointer): Unit
  def load(ptr: Pointer, filename: String): Boolean
  def getDistance(ptr: Pointer, i: Int, j: Int): Float
  def getNnsByItem(ptr: Pointer, item: Int, n: Int, searchK: Int, result: Array[Int], distances: Array[Float]): Unit
  def getNnsByVector(ptr: Pointer, w: Array[Float], n: Int, searchK: Int, result: Array[Int], distances: Array[Float]): Unit
  def getNItems(ptr: Pointer): Int
  def verbose(ptr: Pointer, v: Boolean): Unit
  def getItem(ptr: Pointer, item: Int, v: Array[Float]): Unit
}
