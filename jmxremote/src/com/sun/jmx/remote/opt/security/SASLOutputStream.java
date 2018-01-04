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
/*
 * @(#)file      SASLOutputStream.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.10
 * @(#)lastedit  07/03/08
 * @(#)build     @BUILD_TAG_PLACEHOLDER@
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jmx.remote.opt.security;

import java.io.*;

import javax.security.sasl.*;

import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 *
 */
public class SASLOutputStream extends OutputStream {
  /**
   *
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "SASLOutputStream");
  /**
   *
   */
  private int rawSendSize = 65536;
  /**
   *
   */
  private byte[] lenBuf = new byte[4]; // buffer for storing length
  /**
   *
   */
  private OutputStream out; // underlying output stream
  /**
   *
   */
  private SaslClient sc;
  /**
   *
   */
  private SaslServer ss;

  /**
   *
   * @param sc .
   * @param out .
   * @throws IOException if any I/O error occurs.
   */
  public SASLOutputStream(final SaslClient sc, final OutputStream out) throws IOException {
    super();
    this.out = out;
    this.sc = sc;
    this.ss = null;
    final String str = (String) sc.getNegotiatedProperty(Sasl.RAW_SEND_SIZE);
    if (str != null) {
      try {
        rawSendSize = Integer.parseInt(str);
      } catch (@SuppressWarnings("unused") final NumberFormatException e) {
        throw new IOException(Sasl.RAW_SEND_SIZE + " property must be numeric string: " + str);
      }
    }
  }

  /**
   *
   * @param ss .
   * @param out .
   * @throws IOException if any I/O error occurs.
   */
  public SASLOutputStream(final SaslServer ss, final OutputStream out) throws IOException {
    super();
    this.out = out;
    this.ss = ss;
    this.sc = null;
    final String str = (String) ss.getNegotiatedProperty(Sasl.RAW_SEND_SIZE);
    if (str != null) {
      try {
        rawSendSize = Integer.parseInt(str);
      } catch (@SuppressWarnings("unused") final NumberFormatException e) {
        throw new IOException(Sasl.RAW_SEND_SIZE + " property must be numeric string: " + str);
      }
    }
  }

  @Override
  public void write(final int b) throws IOException {
    final byte[] buffer = new byte[1];
    buffer[0] = (byte) b;
    write(buffer, 0, 1);
  }

  @Override
  public void write(final byte[] buffer, final int offset, final int total) throws IOException {
    int count;
    byte[] wrappedToken;
    final byte[] saslBuffer;
    // "Packetize" buffer to be within rawSendSize
    if (logger.traceOn()) logger.trace("write", "Total size: " + total);
    for (int i = 0; i < total; i += rawSendSize) {
      // Calculate length of current "packet"
      count = (total - i) < rawSendSize ? (total - i) : rawSendSize;
      // Generate wrapped token
      if (sc != null) wrappedToken = sc.wrap(buffer, offset + i, count);
      else wrappedToken = ss.wrap(buffer, offset + i, count);
      // Write out length
      intToNetworkByteOrder(wrappedToken.length, lenBuf, 0, 4);
      if (logger.traceOn()) logger.trace("write", "sending size: " + wrappedToken.length);
      out.write(lenBuf, 0, 4);
      // Write out wrapped token
      out.write(wrappedToken, 0, wrappedToken.length);
    }
  }

  @Override
  public void close() throws IOException {
    if (sc != null) sc.dispose();
    else ss.dispose();
    out.close();
  }

  /**
   * Encodes an integer into 4 bytes in network byte order in the buffer supplied.
   * @param num .
   * @param buf .
   * @param start .
   * @param count .
   */
  private void intToNetworkByteOrder(final int num, final byte[] buf, final int start, final int count) {
    if (count > 4) throw new IllegalArgumentException("Cannot handle more " + "than 4 bytes");
    int n = num;
    for (int i = count - 1; i >= 0; i--) {
      buf[start + i] = (byte) (n & 0xff);
      n >>>= 8;
    }
  }
}
