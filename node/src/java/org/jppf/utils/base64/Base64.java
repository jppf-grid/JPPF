/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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


/**
 * <p>Encodes and decodes to and from Base64 notation.</p>
 * <p>Homepage: <a href="http://iharder.net/base64">http://iharder.net/base64</a>.</p>
 * 
 * <p>Example:</p>
 * 
 * <code>String encoded = Base64.encode( myByteArray );</code>
 * <br />
 * <code>byte[] myByteArray = Base64.decode( encoded );</code>
 *
 * <p>The <tt>options</tt> parameter, which appears in a few places, is used to pass
 * several pieces of information to the encoder. In the "higher level" methods such as
 * encodeBytes( bytes, options ) the options parameter can be used to indicate such
 * things as first gzipping the bytes before encoding them, not inserting linefeeds,
 * and encoding using the URL-safe and Ordered dialects.</p>
 *
 * <p>Note, according to <a href="http://www.faqs.org/rfcs/rfc3548.html">RFC3548</a>,
 * Section 2.1, implementations should not add line feeds unless explicitly told
 * to do so. I've got Base64 set to this behavior now, although earlier versions
 * broke lines by default.</p>
 *
 * <p>The constants defined in Base64 can be OR-ed together to combine options, so you
 * might make a call like this:</p>
 *
 * <code>String encoded = Base64.encodeBytes( mybytes, Base64.GZIP | Base64.DO_BREAK_LINES );</code>
 * <p>to compress the data before encoding it and then making the output have newline characters.</p>
 * <p>Also...</p>
 * <code>String encoded = Base64.encodeBytes( crazyString.getBytes() );</code>
 *
 * <p>I am placing this code in the Public Domain. Do with it as you will.
 * This software comes with no guarantees or warranties but with
 * plenty of well-wishing instead!
 * Please visit <a href="http://iharder.net/base64">http://iharder.net/base64</a>
 * periodically to check for updates or to contribute improvements.
 *
 * @author Robert Harder
 * @author rob@iharder.net
 * @version 2.3.7
 */
public final class Base64
{
/* ********  P U B L I C   F I E L D S  ******** */

  /** No options specified. Value is zero. */
  public final static int NO_OPTIONS = 0;
  /** Specify encoding in first bit. Value is one. */
  public final static int ENCODE = 1;
  /** Specify decoding in first bit. Value is zero. */
  public final static int DECODE = 0;
  /** Specify that data should be gzip-compressed in second bit. Value is two. */
  public final static int GZIP = 2;
  /** Specify that gzipped data should <em>not</em> be automatically gunzipped. */
  public final static int DONT_GUNZIP = 4;
  /** Do break lines when encoding. Value is 8. */
  public final static int DO_BREAK_LINES = 8;
  /**
   * Encode using Base64-like encoding that is URL- and Filename-safe as described in Section 4 of RFC3548:
   * <a href="http://www.faqs.org/rfcs/rfc3548.html">http://www.faqs.org/rfcs/rfc3548.html</a>.
   * It is important to note that data encoded this way is <em>not</em> officially valid Base64,
   * or at the very least should not be called Base64 without also specifying that is
   * was encoded using the URL- and Filename-safe dialect.
   */
  public final static int URL_SAFE = 16;
  /**
   * Encode using the special "ordered" dialect of Base64 described here:
   * <a href="http://www.faqs.org/qa/rfcc-1940.html">http://www.faqs.org/qa/rfcc-1940.html</a>.
   */
  public final static int ORDERED = 32;

/* ********  P R I V A T E   F I E L D S  ******** */

  /** Maximum line length (76) of Base64 output. */
  final static int MAX_LINE_LENGTH = 76;
  /** The equals sign (=) as a byte. */
  final static byte EQUALS_SIGN = (byte)'=';
  /** The new line character (\n) as a byte. */
  final static byte NEW_LINE = (byte)'\n';
  /** Preferred encoding. */
  final static String PREFERRED_ENCODING = "US-ASCII";
  /** Indicates white space in encoding */
  final static byte WHITE_SPACE_ENC = -5; //
  /** Indicates equals sign in encoding */
  final static byte EQUALS_SIGN_ENC = -1; //

/* ********  S T A N D A R D   B A S E 6 4   A L P H A B E T  ******** */

  /** The 64 valid Base64 values. Host platform may be something funny like EBCDIC, so we hardcode these values. */
  final static byte[] STANDARD_ALPHABET = {
    (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G',
    (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N',
    (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U',
    (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z',
    (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g',
    (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
    (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u',
    (byte)'v', (byte)'w', (byte)'x', (byte)'y', (byte)'z',
    (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5',
    (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'+', (byte)'/'
  };

  /**
   * Translates a Base64 value to either its 6-bit reconstruction value
   * or a negative number indicating some other meaning.
   **/
  final static byte[] STANDARD_DECODABET = {
    -9,-9,-9,-9,-9,-9,-9,-9,-9,                 // Decimal  0 -  8
    -5,-5,                                      // Whitespace: Tab and Linefeed
    -9,-9,                                      // Decimal 11 - 12
    -5,                                         // Whitespace: Carriage Return
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14 - 26
    -9,-9,-9,-9,-9,                             // Decimal 27 - 31
    -5,                                         // Whitespace: Space
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33 - 42
    62,                                         // Plus sign at decimal 43
    -9,-9,-9,                                   // Decimal 44 - 46
    63,                                         // Slash at decimal 47
    52,53,54,55,56,57,58,59,60,61,              // Numbers zero through nine
    -9,-9,-9,                                   // Decimal 58 - 60
    -1,                                         // Equals sign at decimal 61
    -9,-9,-9,                                      // Decimal 62 - 64
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,            // Letters 'A' through 'N'
    14,15,16,17,18,19,20,21,22,23,24,25,        // Letters 'O' through 'Z'
    -9,-9,-9,-9,-9,-9,                          // Decimal 91 - 96
    26,27,28,29,30,31,32,33,34,35,36,37,38,     // Letters 'a' through 'm'
    39,40,41,42,43,44,45,46,47,48,49,50,51,     // Letters 'n' through 'z'
    -9,-9,-9,-9,-9                              // Decimal 123 - 127
    ,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,       // Decimal 128 - 139
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255
  };

/* ********  U R L   S A F E   B A S E 6 4   A L P H A B E T  ******** */

  /**
   * Used in the URL- and Filename-safe dialect described in Section 4 of RFC3548:
   * <a href="http://www.faqs.org/rfcs/rfc3548.html">http://www.faqs.org/rfcs/rfc3548.html</a>.
   * Notice that the last two bytes become "hyphen" and "underscore" instead of "plus" and "slash."
   */
  final static byte[] URL_SAFE_ALPHABET = {
    (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G',
    (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N',
    (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U',
    (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z',
    (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g',
    (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
    (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u',
    (byte)'v', (byte)'w', (byte)'x', (byte)'y', (byte)'z',
    (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5',
    (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'-', (byte)'_'
  };

  /**
   * Used in decoding URL- and Filename-safe dialects of Base64.
   */
  final static byte[] URL_SAFE_DECODABET = {
    -9,-9,-9,-9,-9,-9,-9,-9,-9,                 // Decimal  0 -  8
    -5,-5,                                      // Whitespace: Tab and Linefeed
    -9,-9,                                      // Decimal 11 - 12
    -5,                                         // Whitespace: Carriage Return
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14 - 26
    -9,-9,-9,-9,-9,                             // Decimal 27 - 31
    -5,                                         // Whitespace: Space
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33 - 42
    -9,                                         // Plus sign at decimal 43
    -9,                                         // Decimal 44
    62,                                         // Minus sign at decimal 45
    -9,                                         // Decimal 46
    -9,                                         // Slash at decimal 47
    52,53,54,55,56,57,58,59,60,61,              // Numbers zero through nine
    -9,-9,-9,                                   // Decimal 58 - 60
    -1,                                         // Equals sign at decimal 61
    -9,-9,-9,                                   // Decimal 62 - 64
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,            // Letters 'A' through 'N'
    14,15,16,17,18,19,20,21,22,23,24,25,        // Letters 'O' through 'Z'
    -9,-9,-9,-9,                                // Decimal 91 - 94
    63,                                         // Underscore at decimal 95
    -9,                                         // Decimal 96
    26,27,28,29,30,31,32,33,34,35,36,37,38,     // Letters 'a' through 'm'
    39,40,41,42,43,44,45,46,47,48,49,50,51,     // Letters 'n' through 'z'
    -9,-9,-9,-9,-9                              // Decimal 123 - 127
    ,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 128 - 139
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255
  };

/* ********  O R D E R E D   B A S E 6 4   A L P H A B E T  ******** */

  /**
   * I don't get the point of this technique, but someone requested it,
   * and it is described here:
   * <a href="http://www.faqs.org/qa/rfcc-1940.html">http://www.faqs.org/qa/rfcc-1940.html</a>.
   */
  final static byte[] ORDERED_ALPHABET = {
    (byte)'-',
    (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4',
    (byte)'5', (byte)'6', (byte)'7', (byte)'8', (byte)'9',
    (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G',
    (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N',
    (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U',
    (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z',
    (byte)'_',
    (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g',
    (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
    (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u',
    (byte)'v', (byte)'w', (byte)'x', (byte)'y', (byte)'z'
  };

  /**
   * Used in decoding the "ordered" dialect of Base64.
   */
  final static byte[] ORDERED_DECODABET = {
    -9,-9,-9,-9,-9,-9,-9,-9,-9,                 // Decimal  0 -  8
    -5,-5,                                      // Whitespace: Tab and Linefeed
    -9,-9,                                      // Decimal 11 - 12
    -5,                                         // Whitespace: Carriage Return
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14 - 26
    -9,-9,-9,-9,-9,                             // Decimal 27 - 31
    -5,                                         // Whitespace: Space
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33 - 42
    -9,                                         // Plus sign at decimal 43
    -9,                                         // Decimal 44
    0,                                          // Minus sign at decimal 45
    -9,                                         // Decimal 46
    -9,                                         // Slash at decimal 47
    1,2,3,4,5,6,7,8,9,10,                       // Numbers zero through nine
    -9,-9,-9,                                   // Decimal 58 - 60
    -1,                                         // Equals sign at decimal 61
    -9,-9,-9,                                   // Decimal 62 - 64
    11,12,13,14,15,16,17,18,19,20,21,22,23,     // Letters 'A' through 'M'
    24,25,26,27,28,29,30,31,32,33,34,35,36,     // Letters 'N' through 'Z'
    -9,-9,-9,-9,                                // Decimal 91 - 94
    37,                                         // Underscore at decimal 95
    -9,                                         // Decimal 96
    38,39,40,41,42,43,44,45,46,47,48,49,50,     // Letters 'a' through 'm'
    51,52,53,54,55,56,57,58,59,60,61,62,63,     // Letters 'n' through 'z'
    -9,-9,-9,-9,-9                                 // Decimal 123 - 127
    ,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 128 - 139
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
    -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255
  };


/* ********  D E T E R M I N E   W H I C H   A L H A B E T  ******** */


  /**
   * Returns one of the SOMETHING_ALPHABET byte arrays depending on the options specified.
   * It's possible, though silly, to specify ORDERED <b>and</b> URLSAFE in which case one of them will be picked, though there is
   * no guarantee as to which one will be picked.
   * @param options the options.
   * @return the byte array specified via the options.
   */
  static byte[] getAlphabet( final int options ) {
    if ((options & URL_SAFE) == URL_SAFE)return URL_SAFE_ALPHABET;
    else if ((options & ORDERED) == ORDERED)return ORDERED_ALPHABET;
    return STANDARD_ALPHABET;
  }	// end getAlphabet

  /**
   * Returns one of the _SOMETHING_DECODABET byte arrays depending on the options specified.
   * It's possible, though silly, to specify ORDERED and URL_SAFE in which case one of them will be picked, though there is
   * no guarantee as to which one will be picked.
   * @param options the options.
   * @return the byte array specified via the options.
   */
  static byte[] getDecodabet( final int options ) {
    if( (options & URL_SAFE) == URL_SAFE)return URL_SAFE_DECODABET;
    else if ((options & ORDERED) == ORDERED) return ORDERED_DECODABET;
    return STANDARD_DECODABET;
  }	// end getAlphabet

  /** Defeats instantiation. */
  private Base64(){}
}   // end class Base64
