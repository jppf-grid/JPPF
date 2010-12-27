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

package org.jppf.example.dataencryption.old;

import java.io.*;
import java.security.spec.KeySpec;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;

import org.jppf.data.transform.JPPFDataTransform;
import org.jppf.utils.FileUtils;

/**
 * Sample data transform that uses the DES cyptographic algorithm with a 56 bits secret key. 
 * @author Laurent Cohen
 */
public class DESCipherTransform implements JPPFDataTransform
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
		transform(Cipher.ENCRYPT_MODE, source, destination);
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
		transform(Cipher.DECRYPT_MODE, source, destination);
	}

	/**
	 * Transform the specified input source and write it into the specified destination.<br>
	 * The transformation is either encrytion or decryption, depeding on how the cipher was initilized.
	 * @param mode the cipher mode to use for encryption/decryption.
	 * @param source the input stream of data to encrypt/decrypt.
	 * @param destination the stream into which the encrypted/decrypted data is written.
	 * @throws Exception if any error occurs while encrypting or decrypting the data.
	 */
	private void transform(int mode, InputStream source, OutputStream destination) throws Exception
	{
		Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
		cipher.init(mode, getSecretKey());
		CipherOutputStream cos = new CipherOutputStream(destination, cipher);
		byte[] buffer = new byte[8192];
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
				ClassLoader cl = DESCipherTransform.class.getClassLoader();
				InputStream is = cl.getResourceAsStream("org/jppf/example/dataencryption/sk.bin");
				byte[] encoded = FileUtils.getInputStreamAsByte(is);
				KeySpec spec = new DESKeySpec(encoded);
				secretKey = SecretKeyFactory.getInstance("DES").generateSecret(spec);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return secretKey;
	}
}
