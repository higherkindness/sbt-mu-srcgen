/*
 * Copyright 2020-2023 47 Degrees <https://www.47deg.com>
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

package higherkindness.mu.rpc.srcgen.service

/** A fully qualified Scala type, including _root_ prefix */
final case class FullyQualified(tpe: String)

sealed abstract class RequestParam(val tpe: FullyQualified)

object RequestParam {

  /** A request parameter with a name, a la Avro */
  case class Named(name: String, override val tpe: FullyQualified) extends RequestParam(tpe)

  /** A request parameter with no name specified, a la Protobuf */
  case class Anon(override val tpe: FullyQualified) extends RequestParam(tpe)
}

final case class MethodDefn(
    name: String,
    in: RequestParam,
    out: FullyQualified,
    clientStreaming: Boolean,
    serverStreaming: Boolean,
    comment: Option[String] = None
)

final case class ServiceDefn(
    name: String,
    fullName: String,
    methods: List[MethodDefn]
)
