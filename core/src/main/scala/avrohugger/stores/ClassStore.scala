/*
 * Copyright 2020-2021 47 Degrees <https://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package avrohugger
package stores

import org.apache.avro.Schema

import treehugger.forest.Symbol

import java.util.concurrent.ConcurrentHashMap

class ClassStore {

  val generatedClasses: scala.collection.concurrent.Map[Schema, Symbol] =
    scala.collection.convert.Wrappers.JConcurrentMapWrapper(new ConcurrentHashMap[Schema, Symbol]())

  def accept(schema: Schema, caseClassDef: Symbol) = {
    if (!generatedClasses.contains(schema)) {
      val _ = generatedClasses += schema -> caseClassDef
    }
  }
}
