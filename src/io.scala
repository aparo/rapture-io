/**************************************************************************************************
Rapture I/O Library
Version 0.8.0

The primary distribution site is

  http://www.propensive.com/

Copyright 2010-2013 Propensive Ltd.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.
***************************************************************************************************/

package rapture

import implementation._

import language.implicitConversions
import language.higherKinds

import scala.reflect.ClassTag
import scala.concurrent._
import scala.concurrent.duration._

import java.io._
import java.net._

import annotation.implicitNotFound

/** Combines different elements of the I/O framework.  This class provides implementations of
  * type class objects which should be given higher priority than the defaults.  This allows
  * methods which stream from URLs which have alternative means of being read to favour one type
  * of stream over another without explicitly specifying a type parameter.  Specifically,
  * `FileUrl`s should be read and written and  `HttpUrl`s should be read as
  * byte-streams */
class BaseIo extends Paths with Streams with Urls with Files with Net with Sockets with
    Extractors with Accumulators with Wrappers with Uris with Mail with CollectionExtras with
    Multipart with JsonExtraction with Encryption with Codecs with Digests with Encodings with
    Generation with Ips with Logging with Mime with Misc with Services with Time with Linking with
    Classpath with Processes with CommandLine with TableFormatting with Exceptions with Finance with
    Hex with Ftp with Email with TestFramework {

  @implicitNotFound(msg = "No exception handler was available. Please import ")
  trait ExceptionHandler {
  
    type ![_ <: Exception, _]

    def except[E <: Exception, T](t: => T)(implicit mf: ClassTag[E]): ![E, T]
  }

  object strategy {
    implicit object ThrowExceptions extends ExceptionHandler {
      type ![E <: Exception, T] = T
      
      def except[E <: Exception, T](t: => T)(implicit mf: ClassTag[E]): T = t
    }

    implicit object ReturnEither extends ExceptionHandler {
      type ![E <: Exception, T] = Either[E, T]
      
      def except[E <: Exception, T](t: => T)(implicit mf: ClassTag[E]): Either[E, T] =
        try Right(t) catch {
          case e: E => Left(e)
          case e: Throwable => throw e
        }
      
    }
  }

  /** Type class object for reading `Byte`s from `FileUrl`s */
  implicit object FileStreamByteReader extends JavaInputStreamReader[FileUrl](f => new FileInputStream(f.javaFile))

  /** Type class object for reading `Byte`s from `HttpUrl`s */
  implicit object HttpStreamByteReader extends JavaInputStreamReader[HttpUrl](_.javaConnection.getInputStream)

  /** Type class object for writing `Byte`s to `FileUrl`s */
  implicit object FileStreamByteWriter extends StreamWriter[FileUrl, Byte] {
    def output(url: FileUrl)(implicit eh: ExceptionHandler): eh.![Exception, Output[Byte]] =
      eh.except(new ByteOutput(new BufferedOutputStream(new FileOutputStream(url.javaFile))))
  }

  implicit val procByteStreamReader =
    new JavaInputStreamReader[Proc](_.process.getInputStream)

  implicit val procByteStreamWriter =
    new JavaOutputStreamWriter[Proc](_.process.getOutputStream)

  implicit object FileStreamByteAppender extends StreamAppender[FileUrl, Byte] {
    def appendOutput(url: FileUrl)(implicit eh: ExceptionHandler): eh.![Exception, Output[Byte]] =
      eh.except(new ByteOutput(new BufferedOutputStream(new FileOutputStream(url.javaFile, true))))
  }

  class JavaInputStreamReader[T](val getInputStream: T => InputStream) extends
      StreamReader[T, Byte] {
    def input(t: T)(implicit eh: ExceptionHandler): eh.![Exception, Input[Byte]] =
      eh.except(new ByteInput(new BufferedInputStream(getInputStream(t))))
  }

  class JavaOutputStreamWriter[T](val getOutputStream: T => OutputStream) extends
      StreamWriter[T, Byte] {
    def output(t: T)(implicit eh: ExceptionHandler): eh.![Exception, Output[Byte]] =
      eh.except(new ByteOutput(new BufferedOutputStream(getOutputStream(t))))
  }

  class JavaOutputStreamAppender[T](val getOutputStream: T => OutputStream) extends
      StreamAppender[T, Byte] {
    def appendOutput(t: T)(implicit eh: ExceptionHandler): eh.![Exception, Output[Byte]] =
      eh.except(new ByteOutput(new BufferedOutputStream(getOutputStream(t))))
  }

  implicit def stdoutWriter[Data] = new StreamWriter[Stdout[Data], Data] {
    override def doNotClose = true
    def output(stdout: Stdout[Data])(implicit eh: ExceptionHandler): eh.![Exception, Output[Data]] =
      eh.except[Exception, Output[Data]](stdout.output)
  }

  implicit def stderrWriter[Data] = new StreamWriter[Stderr[Data], Data] {
    override def doNotClose = true
    def output(stderr: Stderr[Data])(implicit eh: ExceptionHandler): eh.![Exception, Output[Data]] =
      eh.except[Exception, Output[Data]](stderr.output)
  }

  implicit def stdin[Data] = new StreamReader[Stdin[Data], Data] {
    override def doNotClose = true
    def input(stdin: Stdin[Data])(implicit eh: ExceptionHandler): eh.![Exception, Input[Data]] =
      eh.except[Exception, Input[Data]](stdin.input)
  }

  implicit class urlCodec(s: String) {
    @inline def urlEncode(implicit encoding: Encoding = Encodings.`UTF-8`) =
      URLEncoder.encode(s, encoding.name)
    @inline def urlDecode(implicit encoding: Encoding = Encodings.`UTF-8`) =
      URLDecoder.decode(s, encoding.name)
  }

  // FIXME Move to misc
  @inline implicit class NullableExtras[T](t: T) {
    def fromNull = if(t == null) None else Some(t)
  }

  def randomGuid() = java.util.UUID.randomUUID().toString

  def DevNull[T] = new Output[T] {
    def close() = ()
    def flush() = ()
    def write(t: T) = ()
  }

  object JavaResources {
    import language.reflectiveCalls
    
    type StructuralReadable = { def getInputStream(): InputStream }
    type StructuralWritable = { def getOutputStream(): OutputStream }
    
    implicit val structuralReader =
      new JavaInputStreamReader[StructuralReadable](_.getInputStream())
    
    implicit val structuralWriter =
      new JavaOutputStreamWriter[StructuralWritable](_.getOutputStream())
  
    implicit val javaFileReader = new JavaInputStreamReader[java.io.File](f =>
        new java.io.FileInputStream(f))
    
    implicit val javaFileWriter = new JavaOutputStreamWriter[java.io.File](f =>
        new java.io.FileOutputStream(f))
    
    implicit val javaFileAppender = new JavaOutputStreamAppender[java.io.File](f =>
        new java.io.FileOutputStream(f, true))
  }

}

object io extends BaseIo

/*class Iof(implicit ec: ExecutionContext) extends BaseIo {

  type ![E <: Exception, T] = Future[T]
  @inline protected def except[E <: Exception, T](t: => T)(implicit mf: ClassTag[E]): Future[T] =
    Future { t }
}*/
