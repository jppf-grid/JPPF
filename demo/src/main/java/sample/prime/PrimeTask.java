/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
package sample.prime;

import java.math.BigInteger;

import org.jppf.node.protocol.AbstractTask;

/**
 * This task performs the multiplication of a matrix row by another matrix, as part of
 * the multiplication of 2 whole matrices.
 * @author Laurent Cohen
 */
public class PrimeTask extends AbstractTask<Integer> {
  /**
   * BigInteger representation of 0.
   */
  private static final BigInteger ZERO = BigInteger.ZERO;
  /**
   * BigInteger representation of 2.
   */
  private static final BigInteger TWO = new BigInteger("2");
  /**
   * BigInteger representation of 4.
   */
  private static final BigInteger FOUR = new BigInteger("4");
  /**
   * The exponent for the number to test. That number is 2^exponent - 1.
   */
  private int exponent = 0;
  /**
   * Initialised when this object is constructed.
   */
  private BigInteger mersenne = null;

  /**
   * Initialize this task with a specified row of values to multiply.
   * @param exponent the values as an array of <code>double</code> values.
   */
  public PrimeTask(final int exponent) {
    this.exponent = exponent;
    //System.out.println("initializing mersenne");
    /*
    long elapsed1 = System.nanoTime();
    mersenne = TWO.pow(exponent).subtract(ONE);
    elapsed1 = (System.nanoTime() - elapsed1) / 1_000_000L;
    System.out.println("elapsed1 = " + elapsed1);
    */
    final int n1 = exponent / 8;
    final int n2 = exponent % 8;
    final int n3 = n2 == 0 ? n1 : n1 + 1;
    final byte[] data = new byte[n3];
    final byte b = (byte) -1;
    for (int i = n3 - 1; i >= n3 - n1; i--) data[i] = b;
    if (n1 < n3) {
      int n = 0;
      for (int i = 0; i < n2; i++) n = n * 2 + 1;
      data[0] = (byte) n;
    }
    mersenne = new BigInteger(1, data);
    //elapsed2 = (System.nanoTime() - elapsed2) / 1_000_000L;
    //System.out.println("elapsed2 = " + elapsed2);
    //System.out.println("mersenne initialized");
  }

  /**
   * Perform the multiplication of a matrix row by another matrix.
   */
  @Override
  public void run() {
    try {
      if (test()) setResult(Integer.valueOf(exponent));
    } catch (final Exception e) {
      setThrowable(e);
    }
  }

  /**
   * Test whether the exponent denotes a prime number.
   * @return true if and only if 2^exponent - 1 is a prime number.
   */
  public boolean test() {
    BigInteger lucas = FOUR;
    for (int i = 3; i <= exponent; i++) lucas = lucas.multiply(lucas).subtract(TWO).mod(mersenne);
    return (lucas.compareTo(ZERO) == 0);
  }
}
