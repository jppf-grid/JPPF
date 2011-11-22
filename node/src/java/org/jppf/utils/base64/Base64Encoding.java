/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
import static org.jppf.utils.base64.Base64.*;

/**
 * 
 * @author Laurent Cohen
 */
public final class Base64Encoding
{
	/* ********  E N C O D I N G   M E T H O D S  ******** */    
  
  /**
   * Encodes up to the first three bytes of array <var>threeBytes</var> and returns a four-byte array in Base64 notation.
   * The actual number of significant bytes in your array is given by <var>numSigBytes</var>.
   * The array <var>threeBytes</var> needs only be as big as <var>numSigBytes</var>.
   * Code can reuse a byte array by passing a four-byte array as <var>b4</var>.
   * @param b4 A reusable byte array to reduce array instantiation
   * @param threeBytes the array to convert
   * @param numSigBytes the number of significant bytes in your array
   * @param options the options.
   * @return four byte array in Base64 notation.
   * @since 1.5.1
   */
  static byte[] encode3to4( byte[] b4, byte[] threeBytes, int numSigBytes, int options ) {
      encode3to4( threeBytes, 0, numSigBytes, b4, 0, options );
      return b4;
  }   // end encode3to4
  
  /**
   * <p>Encodes up to three bytes of the array <var>source</var> and writes the resulting four Base64 bytes to <var>destination</var>.
   * The source and destination arrays can be manipulated anywhere along their length by specifying <var>srcOffset</var> and <var>destOffset</var>.
   * This method does not check to make sure your arrays are large enough to accomodate <var>srcOffset</var> + 3 for
   * the <var>source</var> array or <var>destOffset</var> + 4 for the <var>destination</var> array.
   * The actual number of significant bytes in your array is given by <var>numSigBytes</var>.
   * <p>This is the lowest level of the encoding methods with all possible parameters.
   * @param source the array to convert
   * @param srcOffset the index where conversion begins
   * @param numSigBytes the number of significant bytes in your array
   * @param destination the array to hold the conversion
   * @param destOffset the index where output will be put
   * @param options the options.
   * @return the <var>destination</var> array
   * @since 1.3
   */
  static byte[] encode3to4(byte[] source, int srcOffset, int numSigBytes, byte[] destination, int destOffset, int options ) {
  	byte[] ALPHABET = getAlphabet( options ); 

      //           1         2         3  
      // 01234567890123456789012345678901 Bit position
      // --------000000001111111122222222 Array position from threeBytes
      // --------|    ||    ||    ||    | Six bit groups to index ALPHABET
      //          >>18  >>12  >> 6  >> 0  Right shift necessary
      //                0x3f  0x3f  0x3f  Additional AND
      
      // Create buffer with zero-padding if there are only one or two
      // significant bytes passed in the array.
      // We have to shift left 24 in order to flush out the 1's that appear
      // when Java treats a value as negative that is cast from a byte to an int.
      int inBuff =   ( numSigBytes > 0 ? ((source[ srcOffset     ] << 24) >>>  8) : 0 )
                   | ( numSigBytes > 1 ? ((source[ srcOffset + 1 ] << 24) >>> 16) : 0 )
                   | ( numSigBytes > 2 ? ((source[ srcOffset + 2 ] << 24) >>> 24) : 0 );
      switch( numSigBytes )
      {
          case 3:
              destination[ destOffset     ] = ALPHABET[ (inBuff >>> 18)        ];
              destination[ destOffset + 1 ] = ALPHABET[ (inBuff >>> 12) & 0x3f ];
              destination[ destOffset + 2 ] = ALPHABET[ (inBuff >>>  6) & 0x3f ];
              destination[ destOffset + 3 ] = ALPHABET[ (inBuff       ) & 0x3f ];
              return destination;
          case 2:
              destination[ destOffset     ] = ALPHABET[ (inBuff >>> 18)        ];
              destination[ destOffset + 1 ] = ALPHABET[ (inBuff >>> 12) & 0x3f ];
              destination[ destOffset + 2 ] = ALPHABET[ (inBuff >>>  6) & 0x3f ];
              destination[ destOffset + 3 ] = EQUALS_SIGN;
              return destination;
          case 1:
              destination[ destOffset     ] = ALPHABET[ (inBuff >>> 18)        ];
              destination[ destOffset + 1 ] = ALPHABET[ (inBuff >>> 12) & 0x3f ];
              destination[ destOffset + 2 ] = EQUALS_SIGN;
              destination[ destOffset + 3 ] = EQUALS_SIGN;
              return destination;
          default:
              return destination;
      }   // end switch
  }   // end encode3to4

  /**
   * Performs Base64 encoding on the <code>raw</code> ByteBuffer, writing it to the <code>encoded</code> ByteBuffer.
   * This is an experimental feature. Currently it does not pass along any options (such as {@link #DO_BREAK_LINES} or {@link #GZIP}.
   * @param raw input buffer
   * @param encoded output buffer
   * @since 2.3
   */
  public static void encode( java.nio.ByteBuffer raw, java.nio.ByteBuffer encoded ){
      byte[] raw3 = new byte[3];
      byte[] enc4 = new byte[4];
      while( raw.hasRemaining() ){
          int rem = Math.min(3,raw.remaining());
          raw.get(raw3,0,rem);
          encode3to4(enc4, raw3, rem, Base64.NO_OPTIONS );
          encoded.put(enc4);
      }   // end input remaining
  }

  /**
   * Performs Base64 encoding on the <code>raw</code> ByteBuffer, writing it to the <code>encoded</code> CharBuffer.
   * This is an experimental feature. Currently it does not pass along any options (such as {@link #DO_BREAK_LINES} or {@link #GZIP}.
   * @param raw input buffer
   * @param encoded output buffer
   * @since 2.3
   */
  public static void encode( java.nio.ByteBuffer raw, java.nio.CharBuffer encoded ){
      byte[] raw3 = new byte[3];
      byte[] enc4 = new byte[4];
      while( raw.hasRemaining() ){
          int rem = Math.min(3,raw.remaining());
          raw.get(raw3,0,rem);
          encode3to4(enc4, raw3, rem, Base64.NO_OPTIONS );
          for( int i = 0; i < 4; i++ ) encoded.put( (char)(enc4[i] & 0xFF) );
      }   // end input remaining
  }

  /**
   * Serializes an object and returns the Base64-encoded version of that serialized object.  
   * <p>As of v 2.3, if the object cannot be serialized or there is another error,
   * the method will throw an IOException. <b>This is new to v2.3!</b>
   * In earlier versions, it just returned a null value, but in retrospect that's a pretty poor way to handle it.</p>
   * The object is not GZip-compressed before being encoded.
   * @param serializableObject The object to encode
   * @return The Base64-encoded object
   * @throws IOException if there is an error
   * @throws NullPointerException if serializedObject is null
   * @since 1.4
   */
  public static String encodeObject( Serializable serializableObject ) throws IOException {
      return encodeObject( serializableObject, NO_OPTIONS );
  }   // end encodeObject

  /**
   * Serializes an object and returns the Base64-encoded version of that serialized object.
   * <p>As of v 2.3, if the object cannot be serialized or there is another error,
   * the method will throw an IOException. <b>This is new to v2.3!</b>
   * In earlier versions, it just returned a null value, but in retrospect that's a pretty poor way to handle it.</p>
   * The object is not GZip-compressed before being encoded.
   * <p>Example options:<pre>
   *   GZIP: gzip-compresses object before encoding it.
   *   DO_BREAK_LINES: break lines at 76 characters
   * </pre>
   * <p>Example: <code>encodeObject( myObj, Base64.GZIP )</code> or
   * <p>Example: <code>encodeObject( myObj, Base64.GZIP | Base64.DO_BREAK_LINES )</code>
   * @param serializableObject The object to encode
   * @param options Specified options
   * @return The Base64-encoded object
   * @see Base64#GZIP
   * @see Base64#DO_BREAK_LINES
   * @throws IOException if there is an error
   * @since 2.0
   */
  public static String encodeObject( Serializable serializableObject, int options ) throws IOException {
      if( serializableObject == null ){
          throw new NullPointerException( "Cannot serialize a null object." );
      }   // end if: null
      // Streams
      ByteArrayOutputStream  baos  = null; 
      OutputStream           b64os = null;
      java.util.zip.GZIPOutputStream gzos  = null;
      ObjectOutputStream     oos   = null;
      try {
          // ObjectOutputStream -> (GZIP) -> Base64 -> ByteArrayOutputStream
          baos  = new ByteArrayOutputStream();
          b64os = new Base64OutputStream( baos, ENCODE | options );
          if( (options & GZIP) != 0 ){
              // Gzip
              gzos = new java.util.zip.GZIPOutputStream(b64os);
              oos = new ObjectOutputStream( gzos );
          } else {
              // Not gzipped
              oos = new ObjectOutputStream( b64os );
          }
          oos.writeObject( serializableObject );
      }   // end try
      catch( IOException e ) {
          // Catch it and then throw it immediately so that
          // the finally{} block is called for cleanup.
          throw e;
      }   // end catch
      finally {
          try{ oos.close();   } catch( Exception e ){}
          try{ gzos.close();  } catch( Exception e ){}
          try{ b64os.close(); } catch( Exception e ){}
          try{ baos.close();  } catch( Exception e ){}
      }   // end finally
      // Return value according to relevant encoding.
      try {
          return new String( baos.toByteArray(), PREFERRED_ENCODING );
      }   // end try
      catch (UnsupportedEncodingException uue){
          // Fall back to some Java default
          return new String( baos.toByteArray() );
      }   // end catch
  }   // end encode

  /**
   * Encodes a byte array into Base64 notation. Does not GZip-compress data.
   * @param source The data to convert
   * @return The data in Base64-encoded form
   * @throws NullPointerException if source array is null
   * @since 1.4
   */
  public static String encodeBytes( byte[] source ) {
      // Since we're not going to have the GZIP encoding turned on, we're not going to have an IOException thrown, so
      // we should not force the user to have to catch it.
      String encoded = null;
      try {
          encoded = encodeBytes(source, 0, source.length, NO_OPTIONS);
      } catch (IOException ex) {
          assert false : ex.getMessage();
      }   // end catch
      assert encoded != null;
      return encoded;
  }   // end encodeBytes

  /**
   * Encodes a byte array into Base64 notation.
   * <p>Example options:<pre>
   *   GZIP: gzip-compresses object before encoding it.
   *   DO_BREAK_LINES: break lines at 76 characters
   *     <i>Note: Technically, this makes your encoding non-compliant.</i>
   * </pre>
   * <p>Example: <code>encodeBytes( myData, Base64.GZIP )</code> or
   * <p>Example: <code>encodeBytes( myData, Base64.GZIP | Base64.DO_BREAK_LINES )</code>
   * <p>As of v 2.3, if there is an error with the GZIP stream, the method will throw an IOException. <b>This is new to v2.3!</b>
   * In earlier versions, it just returned a null value, but in retrospect that's a pretty poor way to handle it.</p>
   * @param source The data to convert
   * @param options Specified options
   * @return The Base64-encoded data as a String
   * @see Base64#GZIP
   * @see Base64#DO_BREAK_LINES
   * @throws IOException if there is an error
   * @throws NullPointerException if source array is null
   * @since 2.0
   */
  public static String encodeBytes( byte[] source, int options ) throws IOException {
      return encodeBytes( source, 0, source.length, options );
  }   // end encodeBytes
  
  /**
   * Encodes a byte array into Base64 notation. Does not GZip-compress data.
   * <p>As of v 2.3, if there is an error, the method will throw an IOException. <b>This is new to v2.3!</b>
   * In earlier versions, it just returned a null value, but in retrospect that's a pretty poor way to handle it.</p>
   * @param source The data to convert
   * @param off Offset in array where conversion should begin
   * @param len Length of data to convert
   * @return The Base64-encoded data as a String
   * @throws NullPointerException if source array is null
   * @throws IllegalArgumentException if source array, offset, or length are invalid
   * @since 1.4
   */
  public static String encodeBytes( byte[] source, int off, int len ) {
      // Since we're not going to have the GZIP encoding turned on, we're not going to have an IOException thrown, so
      // we should not force the user to have to catch it.
      String encoded = null;
      try {
          encoded = encodeBytes( source, off, len, NO_OPTIONS );
      } catch (IOException ex) {
          assert false : ex.getMessage();
      }   // end catch
      assert encoded != null;
      return encoded;
  }   // end encodeBytes

  /**
   * Encodes a byte array into Base64 notation.
   * <p>Example options:<pre>
   *   GZIP: gzip-compresses object before encoding it.
   *   DO_BREAK_LINES: break lines at 76 characters
   *     <i>Note: Technically, this makes your encoding non-compliant.</i>
   * </pre>
   * <p>Example: <code>encodeBytes( myData, Base64.GZIP )</code> or
   * <p>Example: <code>encodeBytes( myData, Base64.GZIP | Base64.DO_BREAK_LINES )</code>
   * <p>As of v 2.3, if there is an error with the GZIP stream, the method will throw an IOException. <b>This is new to v2.3!</b>
   * In earlier versions, it just returned a null value, but in retrospect that's a pretty poor way to handle it.</p>
   * @param source The data to convert
   * @param off Offset in array where conversion should begin
   * @param len Length of data to convert
   * @param options Specified options
   * @return The Base64-encoded data as a String
   * @see Base64#GZIP
   * @see Base64#DO_BREAK_LINES
   * @throws IOException if there is an error
   * @throws NullPointerException if source array is null
   * @throws IllegalArgumentException if source array, offset, or length are invalid
   * @since 2.0
   */
  public static String encodeBytes( byte[] source, int off, int len, int options ) throws IOException {
      byte[] encoded = encodeBytesToBytes( source, off, len, options );
      // Return value according to relevant encoding.
      try {
          return new String( encoded, PREFERRED_ENCODING );
      }   // end try
      catch (UnsupportedEncodingException uue) {
          return new String( encoded );
      }   // end catch
  }   // end encodeBytes

  /**
   * Similar to {@link #encodeBytes(byte[])} but returnsa byte array instead of instantiating a String.
   * This is more efficient if you're working with I/O streams and have large data sets to encode.
   * @param source The data to convert
   * @return The Base64-encoded data as a byte[] (of ASCII characters)
   * @throws NullPointerException if source array is null
   * @since 2.3.1
   */
  public static byte[] encodeBytesToBytes( byte[] source ) {
      byte[] encoded = null;
      try {
          encoded = encodeBytesToBytes( source, 0, source.length, Base64.NO_OPTIONS );
      } catch( IOException ex ) {
          assert false : "IOExceptions only come from GZipping, which is turned off: " + ex.getMessage();
      }
      return encoded;
  }

  /**
   * Similar to {@link #encodeBytes(byte[], int, int, int)} but returns a byte array instead of instantiating a String.
   * This is more efficient if you're working with I/O streams and have large data sets to encode.
   * @param source The data to convert
   * @param off Offset in array where conversion should begin
   * @param len Length of data to convert
   * @param options Specified options
   * @return The Base64-encoded data as a String
   * @see Base64#GZIP
   * @see Base64#DO_BREAK_LINES
   * @throws IOException if there is an error
   * @throws NullPointerException if source array is null
   * @throws IllegalArgumentException if source array, offset, or length are invalid
   * @since 2.3.1
   */
  public static byte[] encodeBytesToBytes( byte[] source, int off, int len, int options ) throws IOException {
      if( source == null ) throw new NullPointerException( "Cannot serialize a null array." );
      if( off < 0 ) throw new IllegalArgumentException( "Cannot have negative offset: " + off );
      if( len < 0 ) throw new IllegalArgumentException( "Cannot have length offset: " + len );
      if( off + len > source.length  ) throw new IllegalArgumentException(String.format( "Cannot have offset of %d and length of %d with array of length %d", off,len,source.length));
      // Compress?
      if( (options & GZIP) != 0 ) {
          ByteArrayOutputStream  baos  = null;
          java.util.zip.GZIPOutputStream gzos  = null;
          Base64OutputStream            b64os = null;

          try {
              // GZip -> Base64 -> ByteArray
              baos = new ByteArrayOutputStream();
              b64os = new Base64OutputStream( baos, ENCODE | options );
              gzos  = new java.util.zip.GZIPOutputStream( b64os );
              gzos.write( source, off, len );
              gzos.close();
          }   // end try
          catch( IOException e ) {
              // Catch it and then throw it immediately so that the finally{} block is called for cleanup.
              throw e;
          }   // end catch
          finally {
              try{ gzos.close();  } catch( Exception e ){}
              try{ b64os.close(); } catch( Exception e ){}
              try{ baos.close();  } catch( Exception e ){}
          }   // end finally
          return baos.toByteArray();
      }   // end if: compress
      // Else, don't compress. Better not to use streams at all then.
      else {
          boolean breakLines = (options & DO_BREAK_LINES) != 0;
          //int    len43   = len * 4 / 3;
          //byte[] outBuff = new byte[   ( len43 )                      // Main 4:3
          //                           + ( (len % 3) > 0 ? 4 : 0 )      // Account for padding
          //                           + (breakLines ? ( len43 / MAX_LINE_LENGTH ) : 0) ]; // New lines
          // Try to determine more precisely how big the array needs to be.
          // If we get it right, we don't have to do an array copy, and we save a bunch of memory.
          int encLen = ( len / 3 ) * 4 + ( len % 3 > 0 ? 4 : 0 ); // Bytes needed for actual encoding
          if( breakLines ){
              encLen += encLen / MAX_LINE_LENGTH; // Plus extra newline characters
          }
          byte[] outBuff = new byte[ encLen ];
          int d = 0;
          int e = 0;
          int len2 = len - 2;
          int lineLength = 0;
          for( ; d < len2; d+=3, e+=4 ) {
              encode3to4( source, d+off, 3, outBuff, e, options );
              lineLength += 4;
              if( breakLines && lineLength >= MAX_LINE_LENGTH )
              {
                  outBuff[e+4] = NEW_LINE;
                  e++;
                  lineLength = 0;
              }   // end if: end of line
          }   // en dfor: each piece of array
          if( d < len ) {
              encode3to4( source, d+off, len - d, outBuff, e, options );
              e += 4;
          }   // end if: some padding needed

          // Only resize array if we didn't guess it right.
          if( e <= outBuff.length - 1 ){
              // If breaking lines and the last byte falls right at the line length (76 bytes per line), there will be
              // one extra byte, and the array will need to be resized. Not too bad of an estimate on array size, I'd say.
              byte[] finalOut = new byte[e];
              System.arraycopy(outBuff,0, finalOut,0,e);
              //System.err.println("Having to resize array from " + outBuff.length + " to " + e );
              return finalOut;
          }
          else return outBuff;
      }   // end else: don't compress
  }   // end encodeBytesToBytes
}
