/*
 * This file has been adapted from the great library avro-hugger:
 *
 * https://github.com/julianpeeters/avrohugger/blob/ea19172/avrohugger-core/src/main/scala/stores/ClassStore.scala
 *
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
