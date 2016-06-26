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
  "Annoy" should "create index and query the vectors" in {
    val inputFile = File.newTemporaryFile()
    inputFile.toJava.deleteOnExit()
    inputFile.appendLines(Seq(
      "10 1.0 1.0",
      "11 2.0 1.0",
      "12 2.0 2.0",
      "13 3.0 2.0"
    ):_*)
    
    val outputDir = File.newTemporaryDirectory()
    outputDir.toJava.deleteOnExit()
    
    val annoy = Annoy.create(inputFile.pathAsString, 10, outputDir.pathAsString, Euclidean)
    val res = annoy.query(10, 4)
    res.get.map(_._1) shouldBe Seq(10, 11, 12, 13)
    res.get.map(_._2).zip(Seq(0.0f, 1.0f, 1.414f, 2.236f)).foreach{
      case (a, b) => a shouldBe b +- 0.1f
    }
  }
}
