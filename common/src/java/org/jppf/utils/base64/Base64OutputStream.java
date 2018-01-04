/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.utils.base64;

import java.io.*;

/**
 * A {@link Base64OutputStream} will write data to another <tt>OutputStream</tt>, given in the constructor,
 * and encode/decode to/from Base64 notation on the fly.
 * @see Base64
 * @since 1.3
 */
public class Base64OutputStream extends FilterOutputStream {
  /**
   * 
   */
  private boolean encode;
  /**
   * 
   */
  private int position;
  /**
   * 
   */
  private byte[] buffer;
  /**
   * 
   */
  private int bufferLength;
  /**
   * 
   */
  private int lineLength;
  /**
   * 
   */
  private boolean breakLines;
  /**
   * Scratch used in a few places.
   */
  private byte[] b4;
  /**
   * 
   */
  private boolean suspendEncoding;
  /**
   * Record for later.
   */
  private int     options;
  /**
   * Local copies to avoid extra method calls.
   */
  private byte[]  decodabet;

  /**
   * Constructs a {@link Base64OutputStream} in ENCODE mode.
   * @param out the <tt>OutputStream</tt> to which data will be written.
   * @since 1.3
   */
  public Base64OutputStream( final OutputStream out ) {
    this( out, Base64.ENCODE );
  }   // end constructor

  /**
   * Constructs a {@link Base64OutputStream} in
   * either ENCODE or DECODE mode.
   * <p>
   * Valid options:<pre>
   *   ENCODE or DECODE: Encode or Decode as data is read.
   *   DO_BREAK_LINES: don't break lines at 76 characters
   *     (only meaningful when encoding)</i>
   * </pre>
   * <p> Example: <code>new Base64.OutputStream( out, Base64.ENCODE )</code>
   * @param out the <tt>OutputStream</tt> to which data will be written.
   * @param options Specified options.
   * @see Base64#ENCODE
   * @see Base64#DECODE
   * @see Base64#DO_BREAK_LINES
   * @since 1.3
   */
  public Base64OutputStream( final OutputStream out, final int options ) {
    super( out );
    this.breakLines   = (options & Base64.DO_BREAK_LINES) != 0;
    this.encode       = (options & Base64.ENCODE) != 0;
    this.bufferLength = encode ? 3 : 4;
    this.buffer       = new byte[ bufferLength ];
    this.position     = 0;
    this.lineLength   = 0;
    this.suspendEncoding = false;
    this.b4           = new byte[4];
    this.options      = options;
    this.decodabet    = Base64.getDecodabet(options);
  }   // end constructor

  /**
   * Writes the byte to the output stream after converting to/from Base64 notation.
   * When encoding, bytes are buffered three at a time before the output stream actually gets a write() call.
   * When decoding, bytes are buffered four at a time.
   * @param theByte the byte to write
   * @throws IOException if any I/O error occurs.
   * @since 1.3
   */
  @Override
  public void write(final int theByte) throws IOException {
    // Encoding suspended?
    if( suspendEncoding ) {
      this.out.write( theByte );
      return;
    }   // end if: supsended
    // Encode?
    if( encode ) {
      buffer[ position++ ] = (byte)theByte;
      if( position >= bufferLength ) { // Enough to encode.

        this.out.write( Base64Encoding.encode3to4( b4, buffer, bufferLength, options ) );

        lineLength += 4;
        if( breakLines && lineLength >= Base64.MAX_LINE_LENGTH ) {
          this.out.write( Base64.NEW_LINE );
          lineLength = 0;
        }   // end if: end of line

        position = 0;
      }   // end if: enough to output
    }   // end if: encoding
    // Else, Decoding
    else {
      // Meaningful Base64 character?
      if( decodabet[ theByte & 0x7f ] > Base64.WHITE_SPACE_ENC ) {
        buffer[ position++ ] = (byte)theByte;
        if( position >= bufferLength ) { // Enough to output.
          final int len = Base64Decoding.decode4to3( buffer, 0, b4, 0, options );
          out.write( b4, 0, len );
          position = 0;
        }   // end if: enough to output
      }   // end if: meaningful base64 character
      else if( decodabet[ theByte & 0x7f ] != Base64.WHITE_SPACE_ENC ) throw new IOException( "Invalid character in Base64 data." );
    }   // end else: decoding
  }   // end write

  /**
   * Calls {@link #write(int)} repeatedly until <var>len</var>  bytes are written.
   * @param theBytes array from which to read bytes
   * @param off offset for array
   * @param len max number of bytes to read into array
   * @throws IOException if any I/O error occurs.
   * @since 1.3
   */
  @Override
  public void write( final byte[] theBytes, final int off, final int len ) throws IOException {
    // Encoding suspended?
    if( suspendEncoding ) {
      this.out.write( theBytes, off, len );
      return;
    }   // end if: supsended
    for( int i = 0; i < len; i++ ) write( theBytes[ off + i ] );
  }   // end write

  /**
   * Method added by PHIL. [Thanks, PHIL. -Rob]. This pads the buffer without closing the stream.
   * @throws IOException  if there's an error.
   */
  public void flushBase64() throws IOException  {
    if( position > 0 ) {
      if( encode ) {
        out.write( Base64Encoding.encode3to4( b4, buffer, position, options ) );
        position = 0;
      }   // end if: encoding
      else throw new IOException( "Base64 input not properly padded." );
    }   // end if: buffer partially full
  }   // end flush

  /**
   * Flushes and closes (I think, in the superclass) the stream.
   * @throws IOException if any I/O error occurs.
   * @since 1.3
   */
  @Override
  public void close() throws IOException {
    // 1. Ensure that pending characters are written
    flushBase64();
    // 2. Actually close the stream
    // Base class both flushes and closes.
    super.close();
    buffer = null;
    out    = null;
  }   // end close

  /**
   * Suspends encoding of the stream. May be helpful if you need to embed a piece of base64-encoded data in a stream.
   * @throws IOException  if there's an error flushing
   * @since 1.5.1
   */
  public void suspendEncoding() throws IOException  {
    flushBase64();
    this.suspendEncoding = true;
  }   // end suspendEncoding

  /**
   * Resumes encoding of the stream.
   * May be helpful if you need to embed a piece of
   * base64-encoded data in a stream.
   *
   * @since 1.5.1
   */
  public void resumeEncoding() {
    this.suspendEncoding = false;
  }   // end resumeEncoding
}
