/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 * @(#)ProxyInputStream.java	1.3
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

package com.sun.jmx.remote.opt.internal;

import java.io.*;
import java.math.BigDecimal;

import org.omg.CORBA.*;
import org.omg.CORBA.portable.BoxedValueHelper;

/**
 * 
 */
@SuppressWarnings("deprecation")
public class ProxyInputStream extends org.omg.CORBA_2_3.portable.InputStream {
  /**
   * 
   */
  protected final org.omg.CORBA.portable.InputStream in;

  /**
   * 
   * @param in .
   */
  public ProxyInputStream(final org.omg.CORBA.portable.InputStream in) {
    this.in = in;
  }

  @Override
  public boolean read_boolean() {
    return in.read_boolean();
  }

  @Override
  public char read_char() {
    return in.read_char();
  }

  @Override
  public char read_wchar() {
    return in.read_wchar();
  }

  @Override
  public byte read_octet() {
    return in.read_octet();
  }

  @Override
  public short read_short() {
    return in.read_short();
  }

  @Override
  public short read_ushort() {
    return in.read_ushort();
  }

  @Override
  public int read_long() {
    return in.read_long();
  }

  @Override
  public int read_ulong() {
    return in.read_ulong();
  }

  @Override
  public long read_longlong() {
    return in.read_longlong();
  }

  @Override
  public long read_ulonglong() {
    return in.read_ulonglong();
  }

  @Override
  public float read_float() {
    return in.read_float();
  }

  @Override
  public double read_double() {
    return in.read_double();
  }

  @Override
  public String read_string() {
    return in.read_string();
  }

  @Override
  public String read_wstring() {
    return in.read_wstring();
  }

  @Override
  public void read_boolean_array(final boolean[] value, final int offset, final int length) {
    in.read_boolean_array(value, offset, length);
  }

  @Override
  public void read_char_array(final char[] value, final int offset, final int length) {
    in.read_char_array(value, offset, length);
  }

  @Override
  public void read_wchar_array(final char[] value, final int offset, final int length) {
    in.read_wchar_array(value, offset, length);
  }

  @Override
  public void read_octet_array(final byte[] value, final int offset, final int length) {
    in.read_octet_array(value, offset, length);
  }

  @Override
  public void read_short_array(final short[] value, final int offset, final int length) {
    in.read_short_array(value, offset, length);
  }

  @Override
  public void read_ushort_array(final short[] value, final int offset, final int length) {
    in.read_ushort_array(value, offset, length);
  }

  @Override
  public void read_long_array(final int[] value, final int offset, final int length) {
    in.read_long_array(value, offset, length);
  }

  @Override
  public void read_ulong_array(final int[] value, final int offset, final int length) {
    in.read_ulong_array(value, offset, length);
  }

  @Override
  public void read_longlong_array(final long[] value, final int offset, final int length) {
    in.read_longlong_array(value, offset, length);
  }

  @Override
  public void read_ulonglong_array(final long[] value, final int offset, final int length) {
    in.read_ulonglong_array(value, offset, length);
  }

  @Override
  public void read_float_array(final float[] value, final int offset, final int length) {
    in.read_float_array(value, offset, length);
  }

  @Override
  public void read_double_array(final double[] value, final int offset, final int length) {
    in.read_double_array(value, offset, length);
  }

  @Override
  public org.omg.CORBA.Object read_Object() {
    return in.read_Object();
  }

  @Override
  public TypeCode read_TypeCode() {
    return in.read_TypeCode();
  }

  @Override
  public Any read_any() {
    return in.read_any();
  }

  @Override
  @SuppressWarnings("deprecation")
  public Principal read_Principal() {
    return in.read_Principal();
  }

  @Override
  public int read() throws IOException {
    return in.read();
  }

  @Override
  public BigDecimal read_fixed() {
    return in.read_fixed();
  }

  @Override
  public Context read_Context() {
    return in.read_Context();
  }

  @Override
  @SuppressWarnings("all")
  public org.omg.CORBA.Object read_Object(final java.lang.Class clz) {
    return in.read_Object(clz);
  }

  @Override
  public ORB orb() {
    return in.orb();
  }

  @Override
  public Serializable read_value() {
    return narrow().read_value();
  }

  @Override
  @SuppressWarnings("all")
  public Serializable read_value(final Class clz) {
    return narrow().read_value(clz);
  }

  @Override
  public Serializable read_value(final BoxedValueHelper factory) {
    return narrow().read_value(factory);
  }

  @Override
  public Serializable read_value(final String rep_id) {
    return narrow().read_value(rep_id);
  }

  @Override
  public Serializable read_value(final Serializable value) {
    return narrow().read_value(value);
  }

  @Override
  public java.lang.Object read_abstract_interface() {
    return narrow().read_abstract_interface();
  }

  @Override
  @SuppressWarnings("all")
  public java.lang.Object read_abstract_interface(final Class clz) {
    return narrow().read_abstract_interface(clz);
  }

  /**
   * 
   * @return .
   */
  protected org.omg.CORBA_2_3.portable.InputStream narrow() {
    if (in instanceof org.omg.CORBA_2_3.portable.InputStream) return (org.omg.CORBA_2_3.portable.InputStream) in;
    throw new NO_IMPLEMENT();
  }

  /**
   * 
   * @return .
   */
  public org.omg.CORBA.portable.InputStream getProxiedInputStream() {
    return in;
  }
}
