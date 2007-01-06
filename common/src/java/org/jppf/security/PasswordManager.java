/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.security;

import java.io.*;
import org.apache.log4j.Logger;
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
	static Logger log = Logger.getLogger(PasswordManager.class);
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
