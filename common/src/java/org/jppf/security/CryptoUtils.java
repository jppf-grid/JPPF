/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.security;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.jppf.utils.*;

/**
 * Utility methods to handle encryption and decryption, digital signing, and key exchange protocols.
 * @author Laurent Cohen
 */
public class CryptoUtils
{
	/**
	 * Private key used for digital signature generation.
	 */
	private static PrivateKey privateKey = null;
	/**
	 * Public key used for digital signature verification.
	 */
	private static PublicKey publicKey = null;
	/**
	 * Public key used for encryption and decryption.
	 */
	private static SecretKey secretKey = null;

	/**
	 * Entry pont to test this class.
	 * @param args no used.
	 */
	public static void main(String...args)
	{
		try
		{
			//generateDHKeyPair();
			//generateKeyPair();
			generateSecretKey();
			/*
			byte[] data = "Some data to sign".getBytes();
			byte[] sig = generateSignature(data);
			boolean b = verifySignature(sig, data);
			System.out.println("Signature verification: "+b);
			byte[] encrypted = encrypt(data);
			byte[] decrypted = decrypt(encrypted);
			System.out.println("Decrypted value: "+new String(decrypted));
			*/
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Generate a private/public pair of keys.
	 * @return a pair of private and corresponding public key, as a <code>KeyPair</code> instance.
	 * @throws Exception if an exception is raised while generating the pair of keys.
	 */
	public static KeyPair generateKeyPair() throws Exception
	{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		random.setSeed(System.currentTimeMillis());
		keyGen.initialize(1024, random);
		KeyPair pair = keyGen.generateKeyPair();
		PrivateKey privateKey = pair.getPrivate();
		PublicKey publicKey = pair.getPublic();
		byte[] bytes = privateKey.getEncoded();
		System.out.println("Private key: "+StringUtils.dumpBytes(bytes, 0, bytes.length));
		bytes = publicKey.getEncoded();
		System.out.println("Public key: "+StringUtils.dumpBytes(bytes, 0, bytes.length));
		return pair;
	}

	/**
	 * Generate a Diffie Hellman private/public pair of keys.
	 * @return a pair of private and corresponding public key, as a <code>KeyPair</code> instance.
	 * @throws Exception if an exception is raised while generating the pair of keys.
	 */
	public static KeyPair generateDHKeyPair() throws Exception
	{
		DHParameterSpec paramSpec = null;
		AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
		paramGen.init(512);
		AlgorithmParameters params = paramGen.generateParameters();
		paramSpec = params.getParameterSpec(DHParameterSpec.class);

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
		keyGen.initialize(paramSpec);
    KeyPair pair = keyGen.generateKeyPair();

		PrivateKey privateKey = pair.getPrivate();
		PublicKey publicKey = pair.getPublic();
		byte[] bytes = privateKey.getEncoded();
		System.out.println("Private DH key: "+StringUtils.dumpBytes(bytes, 0, bytes.length));
		bytes = publicKey.getEncoded();
		System.out.println("Public DH key: "+StringUtils.dumpBytes(bytes, 0, bytes.length));
		return pair;
	}

	/**
	 * Generate a secret key.
	 * @return a <code>SecretKey</code> instance.
	 * @throws Exception if th ekey could not be generated.
	 */
	public static SecretKey generateSecretKey() throws Exception
	{
		KeyGenerator gen = KeyGenerator.getInstance("DES");
		gen.init(56);
		return gen.generateKey();
	}

	/**
	 * Read a public or private key from a file.
	 * @param filename the name of the file to read the key from.
	 * @return the encoded key value, as an array of bytes.
	 * @throws IOException if an exception is raised while readsing the file.
	 */
	public static byte[] readKeyFile(String filename) throws IOException
	{
		InputStream is = null;
		File file = new File(filename);
		if (file.exists()) is = new FileInputStream(file);
		else is = CryptoUtils.class.getClassLoader().getResourceAsStream(filename);
		if (is == null) throw new FileNotFoundException("could not find file: "+filename);
		String hexString = FileUtils.readTextFile(new InputStreamReader(is));
		return StringUtils.toBytes(hexString);
	}

	/**
	 * Digitally sign some data.
	 * @param data the data to sign.
	 * @return the digital data signature as an array of bytes.
	 * @throws Exception if an exception is raised while generating the signature.
	 */
	public static byte[] generateSignature(byte[] data) throws Exception
	{
		Signature sig = Signature.getInstance("SHA1withDSA");
		sig.initSign(getPrivateKey());
		sig.update(data);
		byte[] signature = sig.sign();
		return signature;
	}

	/**
	 * Check a digital signature for some data.
	 * @param signature data signature.
	 * @param data the signed data.
	 * @return the digital data signature as an array of bytes.
	 * @throws Exception if an exception is raised while generating the signature.
	 */
	public static boolean verifySignature(byte[] signature, byte[] data) throws Exception
	{
		Signature sig = Signature.getInstance("SHA1withDSA");
		sig.initVerify(getPublicKey());
		sig.update(data);
		return sig.verify(signature);
	}

	/**
	 * Get the private key used for signature generation.
	 * @return a <code>PrivateKey</code> instance.
	 * @throws Exception if the private key could not be obtained.
	 */
	private static PrivateKey getPrivateKey() throws Exception
	{
		if (privateKey == null)
		{
			byte[] priv = readKeyFile("config/PrivateKey.txt");
			PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(priv);
			KeyFactory keyFactory = KeyFactory.getInstance("DSA");
	    privateKey = keyFactory.generatePrivate(privKeySpec);
		}
		return privateKey;
	}

	/**
	 * Get the public key used for signature verification.
	 * @return a <code>PublicKey</code> instance.
	 * @throws Exception if the public key could not be obtained.
	 */
	private static PublicKey getPublicKey() throws Exception
	{
		if (publicKey == null)
		{
			byte[] pub = readKeyFile("config/PublicKey.txt");
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pub);
			KeyFactory keyFactory = KeyFactory.getInstance("DSA");
	    publicKey = keyFactory.generatePublic(pubKeySpec);
		}
		return publicKey;
	}

	/**
	 * Get the secret key used for encryption and decryption.
	 * @return a <code>SecretKey</code> instance.
	 * @throws Exception if the secret key could not be obtained.
	 */
	public static SecretKey getSecretKey() throws Exception
	{
		if (secretKey == null)
		{
			byte[] pub = readKeyFile("org/jppf/resources/SecretKey.txt");
			KeySpec spec = new DESKeySpec(pub);
			SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
			secretKey = skf.generateSecret(spec);
		}
		return secretKey;
	}

	/**
	 * Get a secret key form its encoded representation.
	 * @param encoded the secret key in encoded format.
	 * @return a <code>SecretKey</code> instance.
	 * @throws Exception if the secret key could not be obtained.
	 */
	public static SecretKey getSecretKeyFromEncoded(byte[] encoded) throws Exception
	{
		KeySpec spec = new DESKeySpec(encoded);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
		return skf.generateSecret(spec);
	}

	/**
	 * Encrypt some data.
	 * @param data the data as an array of bytes.
	 * @return the encrypted data as an array of bytes.
	 * @throws Exception if the data encryption failed.
	 */
	public static byte[] encrypt(byte[] data) throws Exception
	{
		return encrypt(getSecretKey(), data);
	}

	/**
	 * Encrypt some data using athe specified key.
	 * @param key the key to use for encryption.
	 * @param data the data as an array of bytes.
	 * @return the encrypted data as an array of bytes.
	 * @throws Exception if the data encryption failed.
	 */
	public static byte[] encrypt(SecretKey key, byte[] data) throws Exception
	{
		Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	/**
	 * Encrypt some data.
	 * @param data the data as an array of bytes.
	 * @return the encrypted data as an array of bytes.
	 * @throws Exception if the data encryption failed.
	 */
	public static byte[] decrypt(byte[] data) throws Exception
	{
		return decrypt(getSecretKey(), data);
	}

	/**
	 * Encrypt some data.
	 * @param key the key to use for decryption.
	 * @param data the data as an array of bytes.
	 * @return the encrypted data as an array of bytes.
	 * @throws Exception if the data encryption failed.
	 */
	public static byte[] decrypt(SecretKey key, byte[] data) throws Exception
	{
		Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(data);
	}
}
