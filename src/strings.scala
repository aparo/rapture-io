/**********************************************************************************************\
* Rapture I/O Library                                                                          *
* Version 0.9.0                                                                                *
*                                                                                              *
* The primary distribution site is                                                             *
*                                                                                              *
*   http://rapture.io/                                                                         *
*                                                                                              *
* Copyright 2010-2013 Propensive Ltd.                                                          *
*                                                                                              *
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file    *
* except in compliance with the License. You may obtain a copy of the License at               *
*                                                                                              *
*   http://www.apache.org/licenses/LICENSE-2.0                                                 *
*                                                                                              *
* Unless required by applicable law or agreed to in writing, software distributed under the    *
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,    *
* either express or implied. See the License for the specific language governing permissions   *
* and limitations under the License.                                                           *
\**********************************************************************************************/
package rapture.io

import rapture.core._
import java.net._

class StringMethods(s: String) {
  @inline def urlEncode(implicit encoding: Encoding = Encodings.`UTF-8`) =
    URLEncoder.encode(s, encoding.name)
  @inline def urlDecode(implicit encoding: Encoding = Encodings.`UTF-8`) =
    URLDecoder.decode(s, encoding.name)

  def /(s2: String) = new RelativePath(0, Array(s, s2), Map())
}