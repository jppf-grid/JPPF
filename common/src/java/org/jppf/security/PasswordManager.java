/*
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
import org.apache.commons.logging.*;
import org.jppf.utils.*;

/**
 * This class handles the admin password on the server side.
 * @author Laurent Cohen
 */
public class PasswordManager
{
	/**
	 * Log4j logger for this class.
	 */
	static Log log = LogFactory.getLog(PasswordManager.class);
	/**
	 * Path to the file where the encrypted admin password is stored.
	 */
	private static final String PWD_FILE = "admin.pwd";

	/**
	 * Read the encrypted admin password stored in a file.
	 * @return the encrypted password as an array of bytes.
	 */
	public byte[] readPassword()
	{
		byte[] result = null;
		try
		{
			File file = new File(PWD_FILE);
			if (!file.exists())
			{
				byte[] b = CryptoUtils.encrypt("admin".getBytes());
				savePassword(b);
			}
			String hexString = FileUtils.readTextFile(PWD_FILE);
			result = StringUtils.toBytes(hexString);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Save the encrypted admin password in a text file.
	 * @param password the password as an array of bytes.
	 */
	public void savePassword(byte[] password)
	{
		try
		{
			FileUtils.writeTextFile(PWD_FILE, StringUtils.dumpBytes(password, 0, password.length));
		}
		catch(IOException e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
