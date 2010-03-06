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

import java.io.InputStream;
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
	 * Encrypt the data.
	 * @param data the data to transform.
	 * @return the transformed data as an array of bytes.
	 * @see org.jppf.data.transform.JPPFDataTransform#wrap(byte[])
	 */
	public byte[] wrap(byte[] data)
	{
		try
		{
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
			return cipher.doFinal(data);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Decrypt the data.
	 * @param data the data to transform.
	 * @return the transformed data as an array of bytes.
	 * @see org.jppf.data.transform.JPPFDataTransform#unwrap(byte[])
	 */
	public byte[] unwrap(byte[] data)
	{
		try
		{
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
			return cipher.doFinal(data);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
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
				SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
				return skf.generateSecret(spec);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return secretKey;
	}
}
