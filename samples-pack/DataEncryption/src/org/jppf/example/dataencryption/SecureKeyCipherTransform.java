/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
import java.security.KeyStore;

import javax.crypto.*;

import org.jppf.data.transform.JPPFDataTransform;
import org.jppf.example.dataencryption.helper.Helper;

/**
 * Sample data transform that uses the DES cyptographic algorithm with a 56 bits secret key. 
 * @author Laurent Cohen
 */
public class SecureKeyCipherTransform implements JPPFDataTransform
{
	/**
	 * Secret (symetric) key used for encryption and decryption.
	 */
	private static SecretKey secretKey = getSecretKey();

	/**
	 * Encrypt the data using streams.
	 * @param source the input stream of data to encrypt.
	 * @param destination the stream into which the encrypted data is written.
	 * @throws Exception if any error occurs while encrypting the data.
	 * @see org.jppf.data.transform.JPPFDataTransform#wrap(byte[])
	 */
	public void wrap(InputStream source, OutputStream destination) throws Exception
	{
		// create a cipher instance
		Cipher cipher = Cipher.getInstance(Helper.getTransformation());
		// initialize the cipher with the key stored in the secured keystore
		cipher.init(Cipher.WRAP_MODE, getSecretKey());
		// generate a new key that we will use to encrypt the data
		SecretKey key = generateKey();
		// encrypt the new key, using the secret key found in the keystore
		byte[] keyBytes = cipher.wrap(key);
		// now we write the encrypted key before the data
		DataOutputStream dos = new DataOutputStream(destination);
		// write the key length
		dos.writeInt(keyBytes.length);
		// write the key content
		dos.write(keyBytes);
		// finally, encrypt the data using the new key
		transform(Cipher.ENCRYPT_MODE, source, destination, key);
	}

	/**
	 * Decrypt the data.
	 * @param source the input stream of data to decrypt.
	 * @param destination the stream into which the decrypted data is written.
	 * @throws Exception if any error occurs while decrypting the data.
	 * @see org.jppf.data.transform.JPPFDataTransform#unwrap(byte[])
	 */
	public void unwrap(InputStream source, OutputStream destination) throws Exception
	{
		// start by reading the secret key to use to decrypt the data
		DataInputStream dis = new DataInputStream(source);
		// read the length of the key
		int keyLength = dis.readInt();
		// read the encrypted key
		byte[] keyBytes = new byte[keyLength];
		dis.read(keyBytes);
		// decrypt the key using the initial key stored in the keystore
		Cipher cipher = Cipher.getInstance(Helper.getTransformation());
		cipher.init(Cipher.UNWRAP_MODE, getSecretKey());
		SecretKey key = (SecretKey) cipher.unwrap(keyBytes, Helper.getAlgorithm(), Cipher.SECRET_KEY);
		// finally, decrypt the data using the new key
		transform(Cipher.DECRYPT_MODE, source, destination, key);
	}

	/**
	 * Generate a secret key.
	 * @return a {@link SecretKey} instance.
	 * @throws Exception if any error occurs.
	 */
	private SecretKey generateKey() throws Exception
	{
		KeyGenerator gen = KeyGenerator.getInstance(Helper.getAlgorithm());
		return gen.generateKey();
	}

	/**
	 * Transform the specified input source and write it into the specified destination.<br>
	 * The transformation is either encrytion or decryption, depending on how the cipher was initialized.
	 * @param mode the cipher mode to use for encryption/decryption.
	 * @param source the input stream of data to encrypt/decrypt.
	 * @param destination the stream into which the encrypted/decrypted data is written.
	 * @param key the secret key to use with the cipher.
	 * @throws Exception if any error occurs while encrypting or decrypting the data.
	 */
	private void transform(int mode, InputStream source, OutputStream destination, SecretKey key) throws Exception
	{
		// get the cipher and parameters
		Cipher cipher = Cipher.getInstance(Helper.getTransformation());
		// init the cipher in encryption or decryption mode
		cipher.init(mode, key);
		CipherOutputStream cos = new CipherOutputStream(destination, cipher);
		byte[] buffer = new byte[8192];
		// encrypt or decrypt from source to destination
		while (true)
		{
			int n = source.read(buffer);
			if (n <= 0) break;
			destination.write(buffer, 0, n);
		}
	}

	/**
	 * Get the secret key used for encryption/decryption.
	 * In this method, the secret key is read from a location in the classpath.
	 * This is definitely unsecure, and for demonstration purposes only.
	 * The secret key should be stored in a secure location such as a key store.
	 * @return a <code>SecretKey</code> instance.
	 */
	private static synchronized SecretKey getSecretKey()
	{
		if (secretKey == null)
		{
			try
			{
				// get the keystore password
				char[] password = Helper.getPassword();
				ClassLoader cl = SecureKeyCipherTransform.class.getClassLoader();
				InputStream is = cl.getResourceAsStream(Helper.getKeystoreFolder() + Helper.getKeystoreFilename());
				KeyStore ks = KeyStore.getInstance(Helper.getProvider());
				// load the keystore
				ks.load(is, password);
				// get the secret key from the keystore
				secretKey = (SecretKey) ks.getKey(Helper.getKeyAlias(), password);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return secretKey;
	}
}
