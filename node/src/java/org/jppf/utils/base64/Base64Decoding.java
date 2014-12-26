/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import static org.jppf.utils.base64.Base64.*;

import java.io.*;

import org.jppf.io.IO;

/**
 * 
 * @author Laurent Cohen
 */
public final class Base64Decoding
{
  /* ********  D E C O D I N G   M E T H O D S  ******** */

  /**
   * Decodes four bytes from array <var>source</var> and writes the resulting bytes (up to three of them) to <var>destination</var>.
   * The source and destination arrays can be manipulated anywhere along their length by specifying <var>srcOffset</var> and <var>destOffset</var>.
   * This method does not check to make sure your arrays are large enough to accomodate <var>srcOffset</var> + 4 for
   * the <var>source</var> array or <var>destOffset</var> + 3 for the <var>destination</var> array.
   * This method returns the actual number of bytes that  were converted from the Base64 encoding.
   * <p>This is the lowest level of the decoding methods with all possible parameters.</p>
   * @param source the array to convert
   * @param srcOffset the index where conversion begins
   * @param destination the array to hold the conversion
   * @param destOffset the index where output will be put
   * @param options alphabet type is pulled from this (standard, url-safe, ordered)
   * @return the number of decoded bytes converted
   * @throws NullPointerException if source or destination arrays are null
   * @throws IllegalArgumentException if srcOffset or destOffset are invalid or there is not enough room in the array.
   * @since 1.3
   */
  static int decode4to3(
      final byte[] source, final int srcOffset,
      final byte[] destination, final int destOffset, final int options ) {
    // Lots of error checking and exception throwing
    if( source == null ) throw new NullPointerException( "Source array was null." );
    if( destination == null ) throw new NullPointerException( "Destination array was null." );
    if( srcOffset < 0 || srcOffset + 3 >= source.length )
      throw new IllegalArgumentException( String.format("Source array with length %d cannot have offset of %d and still process four bytes.", source.length, srcOffset ) );
    if( destOffset < 0 || destOffset +2 >= destination.length )
      throw new IllegalArgumentException( String.format("Destination array with length %d cannot have offset of %d and still store three bytes.", destination.length, destOffset ) );

    byte[] DECODABET = getDecodabet( options );
    // Example: Dk==
    if( source[ srcOffset + 2] == EQUALS_SIGN ) {
      // Two ways to do the same thing. Don't know which way I like best.
      //int outBuff =   ( ( DECODABET[ source[ srcOffset    ] ] << 24 ) >>>  6 )
      //              | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );
      int outBuff =   ( ( DECODABET[ source[ srcOffset    ] ] & 0xFF ) << 18 )
      | ( ( DECODABET[ source[ srcOffset + 1] ] & 0xFF ) << 12 );
      destination[ destOffset ] = (byte)( outBuff >>> 16 );
      return 1;
    }
    // Example: DkL=
    else if( source[ srcOffset + 3 ] == EQUALS_SIGN ) {
      // Two ways to do the same thing. Don't know which way I like best.
      //int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] << 24 ) >>>  6 )
      //              | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
      //              | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );
      int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] & 0xFF ) << 18 )
      | ( ( DECODABET[ source[ srcOffset + 1 ] ] & 0xFF ) << 12 )
      | ( ( DECODABET[ source[ srcOffset + 2 ] ] & 0xFF ) <<  6 );

      destination[ destOffset     ] = (byte)( outBuff >>> 16 );
      destination[ destOffset + 1 ] = (byte)( outBuff >>>  8 );
      return 2;
    }
    // Example: DkLE
    else {
      // Two ways to do the same thing. Don't know which way I like best.
      //int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] << 24 ) >>>  6 )
      //              | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
      //              | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )
      //              | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );
      int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] & 0xFF ) << 18 )
      | ( ( DECODABET[ source[ srcOffset + 1 ] ] & 0xFF ) << 12 )
      | ( ( DECODABET[ source[ srcOffset + 2 ] ] & 0xFF ) <<  6)
      | ( ( DECODABET[ source[ srcOffset + 3 ] ] & 0xFF )      );
      destination[ destOffset     ] = (byte)( outBuff >> 16 );
      destination[ destOffset + 1 ] = (byte)( outBuff >>  8 );
      destination[ destOffset + 2 ] = (byte)( outBuff       );
      return 3;
    }
  }   // end decodeToBytes

  /**
   * Low-level access to decoding ASCII characters in the form of a byte array. <strong>Ignores GUNZIP option, if
   * it's set.</strong> This is not generally a recommended method, although it is used internally as part of the decoding process.
   * Special case: if len = 0, an empty array is returned. Still, if you need more speed and reduced memory footprint (and aren't gzipping), consider this method.
   * @param source The Base64 encoded data
   * @return decoded data
   * @throws IOException if any I/O error occurs.
   * @since 2.3.1
   */
  public static byte[] decode( final byte[] source ) throws IOException {
    byte[] decoded = null;
    decoded = decode( source, 0, source.length, Base64.NO_OPTIONS );
    return decoded;
  }

  /**
   * Low-level access to decoding ASCII characters in the form of a byte array. <strong>Ignores GUNZIP option, if
   * it's set.</strong> This is not generally a recommended method,  although it is used internally as part of the decoding process.
   * Special case: if len = 0, an empty array is returned. Still, if you need more speed and reduced memory footprint (and aren't gzipping), consider this method.
   * @param source The Base64 encoded data
   * @param off    The offset of where to begin decoding
   * @param len    The length of characters to decode
   * @param options Can specify options such as alphabet type to use
   * @return decoded data
   * @throws IOException If bogus characters exist in source data
   * @since 1.3
   */
  public static byte[] decode( final byte[] source, final int off, final int len, final int options ) throws IOException {
    // Lots of error checking and exception throwing
    if( source == null ) throw new NullPointerException( "Cannot decode null source array." );
    if( off < 0 || off + len > source.length )
      throw new IllegalArgumentException( String.format("Source array with length %d cannot have offset of %d and process %d bytes.", source.length, off, len ) );

    if( len == 0 )return IO.EMPTY_BYTES;
    else if( len < 4 ){
      throw new IllegalArgumentException(
          "Base64-encoded string must have at least four characters, but length specified was " + len );
    }   // end if
    byte[] DECODABET = getDecodabet( options );
    int    len34   = len * 3 / 4;       // Estimate on array size
    byte[] outBuff = new byte[ len34 ]; // Upper limit on size of output
    int    outBuffPosn = 0;             // Keep track of where we're writing
    byte[] b4        = new byte[4];     // Four byte buffer from source, eliminating white space
    int    b4Posn    = 0;               // Keep track of four byte input buffer
    int    i         = 0;               // Source array counter
    byte   sbiDecode = 0;               // Special value from DECODABET

    for( i = off; i < off+len; i++ ) {  // Loop through source
      sbiDecode = DECODABET[ source[i]&0xFF ];
      // White space, Equals sign, or legit Base64 character
      // Note the values such as -5 and -9 in the DECODABETs at the top of the file.
      if( sbiDecode >= WHITE_SPACE_ENC )  {
        if( sbiDecode >= EQUALS_SIGN_ENC ) {
          b4[ b4Posn++ ] = source[i];         // Save non-whitespace
          if( b4Posn > 3 ) {                  // Time to decode?
            outBuffPosn += decode4to3( b4, 0, outBuff, outBuffPosn, options );
            b4Posn = 0;
            // If that was the equals sign, break out of 'for' loop
            if( source[i] == EQUALS_SIGN )break;
          }   // end if: quartet built
        }   // end if: equals sign or better
      }   // end if: white space, equals sign or better
      else {
        // There's a bad input character in the Base64 stream.
        throw new IOException( String.format("Bad Base64 input character decimal %d in array position %d", (source[i])&0xFF, i ) );
      }   // end else:
    }   // each input character
    byte[] out = new byte[ outBuffPosn ];
    System.arraycopy( outBuff, 0, out, 0, outBuffPosn );
    return out;
  }   // end decode

  /**
   * Decodes data from Base64 notation, automatically detecting gzip-compressed data and decompressing it.
   * @param s the string to decode
   * @return the decoded data
   * @throws IOException If there is a problem
   * @since 1.4
   */
  public static byte[] decode( final String s ) throws IOException {
    return decode( s, NO_OPTIONS );
  }

  /**
   * Decodes data from Base64 notation, automatically detecting gzip-compressed data and decompressing it.
   * @param s the string to decode
   * @param options encode options such as URL_SAFE
   * @return the decoded data
   * @throws IOException if there is an error
   * @throws NullPointerException if <tt>s</tt> is null
   * @since 1.4
   */
  public static byte[] decode( final String s, final int options ) throws IOException {
    if( s == null ) throw new NullPointerException( "Input string was null." );
    byte[] bytes;
    try {
      bytes = s.getBytes( PREFERRED_ENCODING );
    }   // end try
    catch( UnsupportedEncodingException uee ) {
      bytes = s.getBytes();
    }   // end catch

    // Decode
    bytes = decode( bytes, 0, bytes.length, options );
    // Check to see if it's gzip-compressed GZIP Magic Two-Byte Number: 0x8b1f (35615)
    boolean dontGunzip = (options & DONT_GUNZIP) != 0;
    if( (bytes != null) && (bytes.length >= 4) && (!dontGunzip) ) {
      int head = (bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
      if( java.util.zip.GZIPInputStream.GZIP_MAGIC == head )  {
        ByteArrayInputStream  bais = null;
        java.util.zip.GZIPInputStream gzis = null;
        ByteArrayOutputStream baos = null;
        byte[] buffer = new byte[2048];
        int    length = 0;
        try {
          baos = new ByteArrayOutputStream();
          bais = new ByteArrayInputStream( bytes );
          gzis = new java.util.zip.GZIPInputStream( bais );
          while( ( length = gzis.read( buffer ) ) >= 0 ) {
            baos.write(buffer,0,length);
          }   // end while: reading input
          // No error? Get new bytes.
          bytes = baos.toByteArray();
        }   // end try
        catch( IOException e ) {
          e.printStackTrace();
          // Just return originally-decoded bytes
        }   // end catch
        finally {
          try{ baos.close(); } catch( Exception e ){}
          try{ gzis.close(); } catch( Exception e ){}
          try{ bais.close(); } catch( Exception e ){}
        }   // end finally
      }   // end if: gzipped
    }   // end if: bytes.length >= 2
    return bytes;
  }   // end decode

  /**
   * Attempts to decode Base64 data and deserialize a Java Object within. Returns <tt>null</tt> if there was an error.
   * @param encodedObject The Base64 data to decode
   * @return The decoded and deserialized object
   * @throws NullPointerException if encodedObject is null
   * @throws IOException if there is a general error
   * @throws ClassNotFoundException if the decoded object is of a class that cannot be found by the JVM
   * @since 1.5
   */
  public static Object decodeToObject( final String encodedObject ) throws IOException, ClassNotFoundException {
    return decodeToObject(encodedObject,NO_OPTIONS,null);
  }

  /**
   * Attempts to decode Base64 data and deserialize a Java Object within. Returns <tt>null</tt> if there was an error.
   * If <tt>loader</tt> is not null, it will be the class loader used when deserializing.
   * @param encodedObject The Base64 data to decode
   * @param options Various parameters related to decoding
   * @param loader Optional class loader to use in deserializing classes.
   * @return The decoded and deserialized object
   * @throws NullPointerException if encodedObject is null
   * @throws IOException if there is a general error
   * @throws ClassNotFoundException if the decoded object is of a class that cannot be found by the JVM
   * @since 2.3.4
   */
  public static Object decodeToObject(final String encodedObject, final int options, final ClassLoader loader ) throws IOException, java.lang.ClassNotFoundException {
    // Decode and gunzip if necessary
    byte[] objBytes = decode( encodedObject, options );
    ByteArrayInputStream  bais = null;
    ObjectInputStream     ois  = null;
    Object obj = null;
    try {
      bais = new ByteArrayInputStream( objBytes );
      // If no custom class loader is provided, use Java's builtin OIS.
      if( loader == null ) ois  = new ObjectInputStream( bais );
      // Else make a customized object input stream that uses the provided class loader.
      else {
        ois = new ObjectInputStream(bais){
          @Override
          public Class<?> resolveClass(final ObjectStreamClass streamClass)
          throws IOException, ClassNotFoundException {
            Class c = Class.forName(streamClass.getName(), false, loader);
            if( c == null ) return super.resolveClass(streamClass);
            else return c;   // Class loader knows of this class.
          }   // end resolveClass
        };  // end ois
      }   // end else: no custom class loader
      obj = ois.readObject();
    }   // end try
    catch( IOException e ) {
      throw e;    // Catch and throw in order to execute finally{}
    }   // end catch
    catch( java.lang.ClassNotFoundException e ) {
      throw e;    // Catch and throw in order to execute finally{}
    }   // end catch
    finally {
      try{ bais.close(); } catch( Exception e ){}
      try{ ois.close();  } catch( Exception e ){}
    }   // end finally
    return obj;
  }   // end decodeObject

  /**
   * Convenience method for encoding data to a file.
   * <p>As of v 2.3, if there is a error, the method will throw an IOException. <b>This is new to v2.3!</b>
   * In earlier versions, it just returned false, but  in retrospect that's a pretty poor way to handle it.</p>
   * @param dataToEncode byte array of data to encode in base64 form
   * @param filename Filename for saving encoded data
   * @throws IOException if there is an error
   * @throws NullPointerException if dataToEncode is null
   * @since 2.1
   */
  public static void encodeToFile( final byte[] dataToEncode, final String filename )
  throws IOException {
    if( dataToEncode == null ) throw new NullPointerException( "Data to encode was null." );
    Base64OutputStream bos = null;
    try {
      bos = new Base64OutputStream(
          new FileOutputStream( filename ), Base64.ENCODE );
      bos.write( dataToEncode );
    }   // end try
    catch( IOException e ) {
      throw e; // Catch and throw to execute finally{} block
    }   // end catch: IOException
    finally {
      try{ bos.close(); } catch( Exception e ){}
    }   // end finally
  }   // end encodeToFile

  /**
   * Convenience method for decoding data to a file.
   * <p>As of v 2.3, if there is a error, the method will throw an IOException. <b>This is new to v2.3!</b>
   * In earlier versions, it just returned false, but in retrospect that's a pretty poor way to handle it.</p>
   * @param dataToDecode Base64-encoded data as a string
   * @param filename Filename for saving decoded data
   * @throws IOException if there is an error
   * @since 2.1
   */
  public static void decodeToFile( final String dataToDecode, final String filename) throws IOException {
    Base64OutputStream bos = null;
    try{
      bos = new Base64OutputStream(new FileOutputStream( filename ), Base64.DECODE );
      bos.write( dataToDecode.getBytes( PREFERRED_ENCODING ) );
    }   // end try
    catch( IOException e ) {
      throw e; // Catch and throw to execute finally{} block
    }   // end catch: IOException
    finally {
      try{ bos.close(); } catch( Exception e ){}
    }   // end finally
  }   // end decodeToFile

  /**
   * Convenience method for reading a base64-encoded file and decoding it.
   * <p>As of v 2.3, if there is a error, the method will throw an IOException. <b>This is new to v2.3!</b>
   * In earlier versions, it just returned false, but in retrospect that's a pretty poor way to handle it.</p>
   * @param filename Filename for reading encoded data
   * @return decoded byte array
   * @throws IOException if there is an error
   * @since 2.1
   */
  public static byte[] decodeFromFile( final String filename )
  throws IOException {
    byte[] decodedData = null;
    Base64InputStream bis = null;
    try
    {
      // Set up some useful variables
      File file = new File( filename );
      byte[] buffer = null;
      int length   = 0;
      int numBytes = 0;
      // Check for size of file
      if( file.length() > Integer.MAX_VALUE ) throw new IOException( "File is too big for this convenience method (" + file.length() + " bytes)." );
      buffer = new byte[ (int)file.length() ];
      // Open a stream
      bis = new Base64InputStream(new BufferedInputStream(new FileInputStream( file ) ), Base64.DECODE );
      // Read until done
      while( ( numBytes = bis.read( buffer, length, 4096 ) ) >= 0 )length += numBytes;
      // Save in a variable to return
      decodedData = new byte[ length ];
      System.arraycopy( buffer, 0, decodedData, 0, length );
    }   // end try
    catch( IOException e ) {
      throw e; // Catch and release to execute finally{}
    }   // end catch: IOException
    finally {
      try{ bis.close(); } catch( Exception e) {}
    }   // end finally
    return decodedData;
  }   // end decodeFromFile

  /**
   * Convenience method for reading a binary file and base64-encoding it.
   * <p>As of v 2.3, if there is a error, the method will throw an IOException. <b>This is new to v2.3!</b>
   * In earlier versions, it just returned false, but in retrospect that's a pretty poor way to handle it.</p>
   * @param filename Filename for reading binary data
   * @return base64-encoded string
   * @throws IOException if there is an error
   * @since 2.1
   */
  public static String encodeFromFile(final String filename) throws IOException {
    String encodedData = null;
    Base64InputStream bis = null;
    try
    {
      File file = new File( filename );
      byte[] buffer = new byte[ Math.max((int)(file.length() * 1.4+1),40) ]; // Need max() for math on small files (v2.2.1); Need +1 for a few corner cases (v2.3.5)
      int length   = 0;
      int numBytes = 0;
      bis = new Base64InputStream(new BufferedInputStream(new FileInputStream( file ) ), Base64.ENCODE );
      // Read until done
      while( ( numBytes = bis.read( buffer, length, 4096 ) ) >= 0 ) length += numBytes;
      // Save in a variable to return
      encodedData = new String( buffer, 0, length, Base64.PREFERRED_ENCODING );
    }   // end try
    catch( IOException e ) {
      throw e; // Catch and release to execute finally{}
    }   // end catch: IOException
    finally {
      try{ bis.close(); } catch( Exception e) {}
    }   // end finally
    return encodedData;
  }   // end encodeFromFile

  /**
   * Reads <tt>infile</tt> and encodes it to <tt>outfile</tt>.
   * @param infile Input file
   * @param outfile Output file
   * @throws IOException if there is an error
   * @since 2.2
   */
  public static void encodeFileToFile( final String infile, final String outfile ) throws IOException {
    String encoded = encodeFromFile( infile );
    OutputStream out = null;
    try{
      out = new BufferedOutputStream(new FileOutputStream( outfile ) );
      out.write( encoded.getBytes("US-ASCII") ); // Strict, 7-bit output.
    }   // end try
    catch( IOException e ) {
      throw e; // Catch and release to execute finally{}
    }   // end catch
    finally {
      try { out.close(); }
      catch( Exception ex ){}
    }   // end finally
  }   // end encodeFileToFile

  /**
   * Reads <tt>infile</tt> and decodes it to <tt>outfile</tt>.
   * @param infile Input file
   * @param outfile Output file
   * @throws IOException if there is an error
   * @since 2.2
   */
  public static void decodeFileToFile( final String infile, final String outfile ) throws IOException {
    byte[] decoded = decodeFromFile( infile );
    OutputStream out = null;
    try{
      out = new BufferedOutputStream(new FileOutputStream( outfile ) );
      out.write( decoded );
    }   // end try
    catch( IOException e ) {
      throw e; // Catch and release to execute finally{}
    }   // end catch
    finally {
      try { out.close(); }
      catch( Exception ex ){}
    }   // end finally
  }   // end decodeFileToFile
}
