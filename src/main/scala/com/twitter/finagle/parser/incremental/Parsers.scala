package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferIndexFinder, ChannelBuffer}
import com.twitter.finagle.parser.util._
import com.twitter.finagle.ParseException


object Parsers {
  def readTo(choices: String*) = {
    new ConsumingDelimiterParser(AlternateMatcher(choices))
  }

  def readUntil(choices: String*) = {
    new DelimiterParser(AlternateMatcher(choices))
  }

  val readLine = readTo("\r\n", "\n")

  def fail(err: ParseException) = new FailParser(err)

  def const[T](t: T) = new ConstParser(t)

  val unit = const(())

  def readBytes(size: Int) = new FixedBytesParser(size)

  def skipBytes(size: Int) = readBytes(size) flatMap unit

  def guard[T](choices: String*)(parser: Parser[T]) = {
    val g = new ConsumingMatchParser(AlternateMatcher(choices))
    g flatMap parser
  }

  def choice[T](choices: (String, Parser[T])*) = {
    SwitchParser.stringMatchers(choices: _*)
  }

  def repeatTo[T](choices: String*)(parser: Parser[T]): Parser[List[T]] = {
    val end = guard(choices: _*) { const[List[T]](Nil) }

    def go(): Parser[List[T]] = {
      end or (for (t <- parser; ts <- go) yield { t :: ts })
    }

    go()
  }

  def times[T](total: Int)(parser: Parser[T]) = {
    def go(i: Int, prev: List[T]): Parser[Seq[T]] = {
      if (i == total) {
        const(prev.reverse)
      } else {
        parser flatMap { rv =>
          go(i + 1, rv :: prev)
        }
      }
    }

    go(0, Nil)
  }


  private[parser] abstract class PrimitiveParser[Out] extends Parser[Out] {
    protected val continue = Continue(this)
  }

  // integral primitives

  val readByte = new PrimitiveParser[Byte] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 1) Return(buffer.readByte) else continue
    }
  }

  val readShort = new PrimitiveParser[Short] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 2) Return(buffer.readShort) else continue
    }
  }

  val readMedium = new PrimitiveParser[Int] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 3) Return(buffer.readMedium) else continue
    }
  }

  val readInt = new PrimitiveParser[Int] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 4) Return(buffer.readInt) else continue
    }
  }

  val readLong = new PrimitiveParser[Long] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 8) Return(buffer.readLong) else continue
    }
  }


  // Unsigned integral primitives

  val readUnsignedByte = new PrimitiveParser[Short] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 1) Return(buffer.readUnsignedByte) else continue
    }
  }

  val readUnsignedShort = new PrimitiveParser[Int] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 2) Return(buffer.readUnsignedShort) else continue
    }
  }

  val readUnsignedMedium = new PrimitiveParser[Int] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 3) Return(buffer.readUnsignedMedium) else continue
    }
  }

  val readUnsignedInt = new PrimitiveParser[Long] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 4) Return(buffer.readUnsignedInt) else continue
    }
  }


  // non-integral primitives

  val readChar = new PrimitiveParser[Char] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 2) Return(buffer.readChar) else continue
    }
  }

  val readDouble = new PrimitiveParser[Double] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 8) Return(buffer.readDouble) else continue
    }
  }

  val readFloat = new PrimitiveParser[Float] {
    def decode(buffer: ChannelBuffer) = {
      if (buffer.readableBytes >= 4) Return(buffer.readFloat) else continue
    }
  }
}
