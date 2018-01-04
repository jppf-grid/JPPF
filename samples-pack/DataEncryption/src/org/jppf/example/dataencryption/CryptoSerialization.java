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

package org.jppf.example.dataencryption;

import java.io.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

import org.jppf.example.dataencryption.helper.Helper;
import org.jppf.serialization.JPPFCompositeSerialization;

/**
 * Sample composite serialization which serializes transparently encrypted objects.
 * @author Laurent Cohen
 */
public class CryptoSerialization extends JPPFCompositeSerialization {
  /**
   * Secret (symmetric) key used for encryption and decryption.
   */
  private static SecretKey secretKey;
  /**
   * Cipher initialization vector.
   */
  private static IvParameterSpec ivSpec = getInitializationVector();

  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    // create a cipher instance
    Cipher cipher = Cipher.getInstance(Helper.getTransformation());
    // initialize the cipher with the key stored in the secured keystore
    cipher.init(Cipher.WRAP_MODE, getSecretKey(), getInitializationVector());
    // generate a new key that we will use to encrypt the data
    final SecretKey key = generateKey();
    // encrypt the new key, using the secret key found in the keystore
    final byte[] keyBytes = cipher.wrap(key);
    // now we write the encrypted key before the data
    final DataOutputStream dos = new DataOutputStream(os);
    // write the key length
    dos.writeInt(keyBytes.length);
    // write the key content
    dos.write(keyBytes);
    dos.flush();

    // get a new cipher for the actual encryption
    cipher = Cipher.getInstance(Helper.getTransformation());
    // init the cipher in encryption mode
    cipher.init(Cipher.ENCRYPT_MODE, key, getInitializationVector());
    // encrypt the plain riginal object into a sealed object
    final SealedObject sealed = new SealedObject((Serializable) o, cipher);
    // serialize the sealed object
    getDelegate().serialize(sealed, os);
  }

  @Override
  public Object deserialize(final InputStream is) throws Exception {
    // start by reading the secret key to use to decrypt the data
    final DataInputStream dis = new DataInputStream(is);
    // read the length of the key
    final int keyLength = dis.readInt();
    // read the encrypted key
    final byte[] keyBytes = new byte[keyLength];
    int count = 0;
    while (count < keyLength) {
      final int n = dis.read(keyBytes, count, keyLength - count);
      if (n > 0) count += n;
      else throw new EOFException("could only read " + count + " bytes of the key, out of " + keyLength);
    }
    // decrypt the key using the initial key stored in the keystore
    Cipher cipher = Cipher.getInstance(Helper.getTransformation());
    cipher.init(Cipher.UNWRAP_MODE, getSecretKey(), getInitializationVector());
    final SecretKey key = (SecretKey) cipher.unwrap(keyBytes, Helper.getAlgorithm(), Cipher.SECRET_KEY);

    // get a new cipher for the actual decryption
    cipher = Cipher.getInstance(Helper.getTransformation());
    // init the cipher in decryption mode with the retireved key
    cipher.init(Cipher.DECRYPT_MODE, key, getInitializationVector());
    // deserialize a sealed (encrypted) object
    final SealedObject sealed = (SealedObject) getDelegate().deserialize(is);
    // decrypt the sealed object into the plain riginal object
    return sealed.getObject(cipher);
  }

  @Override
  public String getName() {
    // the name given to this serialization and used in composite serilization schemes
    // e.g. jppf.object.serialization.class = CRYPTO org.jppf.serialization.DefaultJavaSerialization
    return "CRYPTO";
  }

  /**
   * Generate a secret key.
   * @return a {@link SecretKey} instance.
   * @throws Exception if any error occurs.
   */
  private static SecretKey generateKey() throws Exception {
    final KeyGenerator gen = KeyGenerator.getInstance(Helper.getAlgorithm());
    return gen.generateKey();
  }

  /**
   * Get the secret key used for encryption/decryption.
   * In this method, the secret key is read from a location in the classpath.
   * This is definitely unsecure, and for demonstration purposes only.
   * The secret key should be stored in a secure location such as a key store.
   * @return a <code>SecretKey</code> instance.
   * @throws Exception if any error occurs.
   */
  private static synchronized SecretKey getSecretKey() throws Exception {
    if (secretKey == null) secretKey = Helper.retrieveSecretKey();
    return secretKey;
  }

  /**
   * Get the initialization vector used when initializing {@link Cipher} instances.
   * @return a {@link IvParameterSpec} instance.
   */
  private static synchronized IvParameterSpec getInitializationVector() {
    if (ivSpec == null) {
      // here we simply initialize the vector with zeroes 
      final byte[] bytes = new byte[16];
      ivSpec = new IvParameterSpec(bytes);
    }
    return ivSpec;
  }
}
