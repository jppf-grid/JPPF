/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package test.org.jppf.serialization;

import static org.junit.Assert.*;

import java.io.NotSerializableException;
import java.util.*;

import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.ObjectSerializerImpl;
import org.junit.Test;

import test.org.jppf.test.setup.AbstractNonStandardSetup;

/**
 * Unit tests for the JPPF serialization scheme.
 * @author Laurent Cohen
 */
public abstract class AbstractTestSerialization extends AbstractNonStandardSetup {
  /**
   * WHtehr the serialization scheme allows non-serializable classes.
   */
  static boolean allowsNonSerializable = true;

  /**
   * Test a simple job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testSimpleJob() throws Exception {
    super.testSimpleJob(null);
  }

  @Override
  @Test(timeout = 15000)
  public void testMultipleJobs() throws Exception {
    super.testMultipleJobs();
  }

  @Override
  @Test(timeout = 10000)
  public void testCancelJob() throws Exception {
    super.testCancelJob();
  }

  @Override
  @Test(timeout = 5000)
  public void testNotSerializableWorkingInNode() throws Exception {
    if (allowsNonSerializable) super.testNotSerializableWorkingInNode();
  }

  @Override
  @Test(timeout = 8000)
  public void testForwardingMBean() throws Exception {
    super.testForwardingMBean();
  }

  /**
   * Test the serialization and deseralization of primitive values.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testPrimitives() throws Exception {
    try {
      PrimitiveStruct ps1 = new PrimitiveStruct();
      PrimitiveStruct ps2 = (PrimitiveStruct) copyBySerialization(ps1);
      assertTrue(ps1.b == ps2.b);
      assertTrue(ps1.s == ps2.s);
      assertTrue(ps1.i == ps2.i);
      assertTrue(ps1.l == ps2.l);
      assertTrue(ps1.f == ps2.f);
      assertTrue(ps1.d == ps2.d);
      assertTrue(ps1.c == ps2.c);
      assertTrue(ps1.z == ps2.z);
    } catch(NotSerializableException e) {
      if (allowsNonSerializable) throw e;
    }
  }

  /**
   * Test the serialization and deseralization of arrays of primitive values.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testPrimitivesArrays() throws Exception {
    try {
      PrimitiveArrayStruct ps1 = new PrimitiveArrayStruct();
      PrimitiveArrayStruct ps2 = (PrimitiveArrayStruct) copyBySerialization(ps1);
      assertTrue(Arrays.equals(ps1.b, ps2.b));
      assertTrue(Arrays.equals(ps1.s, ps2.s));
      assertTrue(Arrays.equals(ps1.i, ps2.i));
      assertTrue(Arrays.equals(ps1.l, ps2.l));
      assertTrue(Arrays.equals(ps1.f, ps2.f));
      assertTrue(Arrays.equals(ps1.d, ps2.d));
      assertTrue(Arrays.equals(ps1.c, ps2.c));
      assertTrue(Arrays.equals(ps1.z, ps2.z));
    } catch(NotSerializableException e) {
      if (allowsNonSerializable) throw e;
    }
  }

  /**
   * Test the serialization and deseralization of a single char.
   * <br>See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-470">JPPF-470</a>.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testChar() throws Exception {
    testObject(new Character('F'));
    testObject(new Character((char) 145));
    testObject(Character.MIN_CODE_POINT);
    testObject(Character.MAX_CODE_POINT);
    testObject(Character.MIN_HIGH_SURROGATE);
    testObject(Character.MAX_HIGH_SURROGATE);
    testObject(Character.MIN_LOW_SURROGATE);
    testObject(Character.MAX_LOW_SURROGATE);
    testObject(Character.MIN_SURROGATE);
    testObject(Character.MAX_SURROGATE);
    testObject(Character.MIN_VALUE);
    testObject(Character.MAX_VALUE);
  }

  /**
   * Test the serialization and deseralization of a single byte.
   * <br>See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-470">JPPF-470</a>.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testByte() throws Exception {
    testObject(new Byte((byte) -65));
    testObject(new Byte((byte) 65));
    testObject(Byte.MIN_VALUE);
    testObject(Byte.MAX_VALUE);
  }

  /**
   * Test the serialization and deseralization of a single Short.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testShort() throws Exception {
    testObject(new Short((short) -12389));
    testObject(new Short((short) 12389));
    testObject(Short.MIN_VALUE);
    testObject(Short.MAX_VALUE);
  }

  /**
   * Test the serialization and deseralization of a single Integer.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testInt() throws Exception {
    testObject(new Integer(1234567890));
    testObject(new Integer(-1234567890));
    assertEquals(0x40, 1 << 6);
    testObject(Integer.MIN_VALUE);
    testObject(Integer.MAX_VALUE);
  }

  /**
   * Test the serialization and deseralization of a single Long.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testLong() throws Exception {
    testObject(new Long(-1234567890123456789L));
    testObject(new Long(1234567890123456789L));
    testObject(Long.MIN_VALUE);
    testObject(Long.MAX_VALUE);
  }

  /**
   * Test the serialization and deseralization of a single Float.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testFloat() throws Exception {
    testObject(new Float(-123.455e-14f));
    testObject(new Float(123.455e14f));
    testObject(Float.MIN_VALUE);
    testObject(Float.MAX_VALUE);
    testObject(Float.NaN);
    testObject(Float.NEGATIVE_INFINITY);
    testObject(Float.POSITIVE_INFINITY);
  }

  /**
   * Test the serialization and deseralization of a single Double.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testDouble() throws Exception {
    testObject(new Double(-4.025e-203d));
    testObject(new Double(4.025e203d));
    testObject(Double.MIN_VALUE);
    testObject(Double.MAX_VALUE);
    testObject(Double.NaN);
    testObject(Double.NEGATIVE_INFINITY);
    testObject(Double.POSITIVE_INFINITY);
  }

  /**
   * Test the serialization and deseralization of a single Boolean.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testBoolean() throws Exception {
    testObject(new Boolean(true));
    testObject(new Boolean(false));
  }

  /**
   * Test the serialization and deseralization of a single char.
   * <br>See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-470">JPPF-470</a>.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testCharArray() throws Exception {
    char[] array1 = { 'F', 145, Character.MIN_CODE_POINT, (char) Character.MAX_CODE_POINT, Character.MIN_HIGH_SURROGATE, Character.MAX_HIGH_SURROGATE,
      Character.MIN_LOW_SURROGATE, Character.MAX_LOW_SURROGATE, Character.MIN_SURROGATE, Character.MAX_SURROGATE, Character.MIN_VALUE, Character.MAX_VALUE };
    char[] array2 = (char[]) copyBySerialization(array1);
    assertTrue(Arrays.equals(array1, array2));
  }

  /**
   * Test the serialization and deseralization of a byte array.
   * <br>See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-470">JPPF-470</a>.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testByteArray() throws Exception {
    byte[] array1 = { -65, 65, Byte.MIN_VALUE, Byte.MAX_VALUE};
    byte[] array2 = (byte[]) copyBySerialization(array1);
    assertTrue(Arrays.equals(array1, array2));
  }

  /**
   * Test the serialization and deseralization of a short array.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testShortArray() throws Exception {
    short[] array1 = { -12389, 12389, Short.MIN_VALUE, Short.MAX_VALUE};
    short[] array2 = (short[]) copyBySerialization(array1);
    assertTrue(Arrays.equals(array1, array2));
  }

  /**
   * Test the serialization and deseralization of an int array.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testIntArray() throws Exception {
    int[] array1 = { -1234567890, 1234567890, Integer.MIN_VALUE, Integer.MAX_VALUE};
    int[] array2 = (int[]) copyBySerialization(array1);
    assertTrue(Arrays.equals(array1, array2));
  }

  /**
   * Test the serialization and deseralization of a long array.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testLongArray() throws Exception {
    long[] array1 = { -1234567890123456789L, 1234567890123456789L, Long.MIN_VALUE, Long.MAX_VALUE};
    long[] array2 = (long[]) copyBySerialization(array1);
    assertTrue(Arrays.equals(array1, array2));
  }

  /**
   * Test the serialization and deseralization of a float array.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testFloatArray() throws Exception {
    float[] array1 = { -123.455e-14f, 123.455e-14f, Float.MIN_VALUE, Float.MAX_VALUE, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY};
    float[] array2 = (float[]) copyBySerialization(array1);
    assertTrue(Arrays.equals(array1, array2));
  }

  /**
   * Test the serialization and deseralization of a double array.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testDoubleArray() throws Exception {
    double[] array1 = { -123.455e-14f, 123.455e-14f, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
    double[] array2 = (double[]) copyBySerialization(array1);
    assertTrue(Arrays.equals(array1, array2));
  }

  /**
   * Test the serialization and deseralization of a boolean array.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testBooleanArray() throws Exception {
    boolean[] array1 = {true, false};
    boolean[] array2 = (boolean[]) copyBySerialization(array1);
    assertTrue(Arrays.equals(array1, array2));
  }

  /**
   * Test the serialization and deseralization of an object.
   * @param o1 the object to check.
   * @throws Exception if any error occurs.
   */
  private void testObject(final Object o1) throws Exception {
    assertEquals(o1, copyBySerialization(o1));
  }

  /**
   * Perform a deep copy of the input object using serialization.
   * @param src the object to copy.
   * @return a copy of the object.
   * @throws Exception if any error occurs.
   */
  private Object copyBySerialization(final Object src) throws Exception {
    ObjectSerializer ser = new ObjectSerializerImpl();
    byte[] bytes = ser.serialize(src).buffer;
    return ser.deserialize(bytes);
  }

  /**
   * Holds a field for each primitive type.
   */
  public static class PrimitiveStruct {
    /** */
    public byte b = 18;
    /** */
    public short s = 12755;
    /** */
    public int i = 127844;
    /** */
    public long l = -12345678901L;
    /** */
    public float f = 123.455e14f;
    /** */
    public double d = -4.025e-203d;
    /** */
    public char c = 'F';
    /** */
    public boolean z = true;
  }

  /**
   * Holds a field for each primitive type array.
   */
  public static class PrimitiveArrayStruct {
    /** */
    public byte b[] = {-128, 127};
    /** */
    public short[] s = {-32768, 32767};
    /** */
    public int[] i = {-2147483648, 2147483647};
    /** */
    public long[] l = {-9223372036854775808L, 9223372036854775807L};
    /** */
    public float[] f = {-123.455e14f, 123.455e14f};
    /** */
    public double[] d = {-4.025e-203d, 4.025e-203d};
    /** */
    public char[] c = {0, 65535};
    /** */
    public boolean[] z = {true, false};
  }
}
