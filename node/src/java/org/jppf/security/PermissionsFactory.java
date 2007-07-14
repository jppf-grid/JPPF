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
import java.lang.reflect.*;
import java.net.SocketPermission;
import java.security.*;
import org.jppf.utils.*;

/**
 * This class is used to generate and obtain the permissions that constitute the security policy for a JPPF node.
 * @author Laurent Cohen
 */
public final class PermissionsFactory
{
	/**
	 * Encapsulates the set of all permissions granted to a node.
	 */
	private static Permissions permissions = null;
	
	/**
	 * Instantiation of this class is not permitted.
	 */
	private PermissionsFactory()
	{
	}
	
	/**
	 * Reset the current permissions to enable their reload.
	 * @see java.security.Permissions
	 */
	public static void resetPermissions()
	{
		permissions = null;
	}
	
	/**
	 * Get the set of permissions granted to a node.
	 * @param classLoader the ClassLoader used to retrieve the policy file.
	 * @return a Permissions object.
	 * @see java.security.Permissions
	 */
	public static Permissions getPermissions(ClassLoader classLoader)
	{
		if (permissions == null)
		{
			if (classLoader == null)
			{
				classLoader = PermissionsFactory.class.getClassLoader();
			}
			createPermissions(classLoader);
		}
		return permissions;
	}
	
	/**
	 * Initialize the permissions granted to a node.
	 * @param classLoader the ClassLoader used to retrieve the policy file.
	 */
	private static synchronized void createPermissions(ClassLoader classLoader)
	{
		if (permissions != null) return;
		permissions = new Permissions();
		createDynamicPermissions(permissions);
		readStaticPermissions(classLoader);
	}

	/**
	 * Initialize the permissions that depend on the JPPF configuration.
	 * @param p the permissions collection to add the permissions to.
	 */
	private static void createDynamicPermissions(Permissions p)
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getString("jppf.server.host", "localhost");
		int port = props.getInt("class.server.port", 11111);
		p.add(new SocketPermission(host + ":" + port, "connect,listen"));
		port = props.getInt("node.server.port", 11113);
		p.add(new SocketPermission(host + ":" + port, "connect,listen"));
	}

	/**
	 * Read the static permissions stored in a policy file, if any is defined.
	 * @param classLoader the ClassLoader used to retrieve the policy file.
	 */
	private static void readStaticPermissions(ClassLoader classLoader)
	{
		InputStream is = null;
		try
		{
			String file = JPPFConfiguration.getProperties().getString("jppf.policy.file");
			if (file == null) return;
			try
			{
				is = new FileInputStream(file);
			}
			catch(FileNotFoundException e)
			{
			}
			if (is == null) is = classLoader.getResourceAsStream(file);
			if (is == null) return;
			LineNumberReader reader = new LineNumberReader(new InputStreamReader(is));
			int count = 0;
			boolean end = false;
			while (!end)
			{
				String line = reader.readLine();
				if (line == null) break;
				count++;
				line = line.trim();
				if ("".equals(line) || line.startsWith("//")) continue;
				if (!line.startsWith("permission"))
				{
					err(file, count, " should start with \"permission\"");
					continue;
				}
				line = line.substring("permission".length());
				if (!line.endsWith(";"))
				{
					err(file, count, " should end with \";\"");
					continue;
				}
				line = line.substring(0, line.length() - 1);
				line = line.trim();
				Permission p = parsePermission(line, file, count);
				if (p != null) permissions.add(p);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally{
			if(is != null){
				try{
					is.close();
				}catch(Exception ignored){}
			}
		}
	}
	
	/**
	 * Parse a permission entry in the policy file.
	 * @param source the string containing the permission entry.
	 * @param file the name of the policy file the permission belongs to.
	 * @param line the line number at which the permission entry is.
	 * @return a <code>Permission</code> object built from the permission entry.
	 */
	private static Permission parsePermission(String source, String file, int line)
	{
		String className = null;
		String name = null;
		String actions = null;

		int idx = source.indexOf("\"");
		if (idx < 0)
		{
			err(file, line, "permission entry has no name/action, or missing opening quote");
			return null;
		}
		className = source.substring(0, idx).trim();

		idx++;
		int idx2 = source.indexOf("\"", idx);
		if (idx2 < 0)
		{
			err(file, line, "missing closing quote on permission name");
			return null;
		}
		name = source.substring(idx, idx2);

		idx = source.indexOf("\"", idx2+1);
		if (idx >= 0)
		{
			idx++;
			idx2 = source.indexOf("\"", idx);
			if (idx2 < 0)
			{
				err(file, line, "missing closing quote on permission action");
				return null;
			}
			actions = source.substring(idx, idx2);
		}
		name = expandProperties(name, file, line);
		if (name == null) return null;
		if (actions != null)
		{
			actions = expandProperties(actions, file, line);
			if (actions == null) return null;
		}
		return instantiatePermission(className, name, actions, file, line);
	}
	
	/**
	 * Instantiate a Permission object given its class name, permission name and actions list.
	 * @param className the name of the permission class.
	 * @param name the name of the permission to grant.
	 * @param actions a comma separated list of actions, may be null.
	 * @param file the name of the policy file the permission belongs to.
	 * @param line the line number at which the permission entry is.
	 * @return a <code>Permission</code> object built from the permission components.
	 */
	private static Permission instantiatePermission(String className, String name, String actions, String file, int line)
	{
		Permission permission = null;
		Class<?> c = null;
		try
		{
			c = Class.forName(className);
		}
		catch(ClassNotFoundException e)
		{
			return new UnresolvedPermission(className, name, actions, null);
		}

		Constructor constructor = null;
		Object[] params = null;
		Exception ex = null;
		String msg = null;
		try
		{
			if (actions != null)
			{
				constructor = c.getConstructor(String.class, String.class);
				params = new Object[] { name, actions };
			}
			else
			{
				constructor = c.getConstructor(String.class);
				params = new Object[] { name };
			}
			permission = (Permission) constructor.newInstance(params);
		}
		catch(InstantiationException e)
		{
			ex = e;
			msg = "could not instantiate";
		}
		catch(IllegalAccessException e)
		{
			ex = e;
			msg = "could not instantiate";
		}
		catch(InvocationTargetException e)
		{
			ex = e;
			msg = "could not instantiate";
		}
		catch(NoSuchMethodException e)
		{
			ex = e;
			msg = "could not find a proper constructor for";
		}
		if (ex != null)
		{
			msg = msg + " permission with class=\"" + className + "\", name=\"" + name + "\", actions=\"" +
				actions + "\" [" + ex.getMessage() + "]";
			err(file, line, msg);
			return null;
		}
		return permission;
	}
	
	/**
	 * Expand the properties in a permission name of action list.
	 * @param source string containing the token.
	 * @param file the name of the policy file the permission belongs to.
	 * @param line the line number at which the permission entry is.
	 * @return the token with its properties expanded.
	 */
	private static String expandProperties(String source, String file, int line)
	{
		StringBuilder sb = new StringBuilder();
		int length = source.length();
		int pos = 0;
		while (pos < length)
		{
			int idx = source.indexOf("${", pos);
			if (idx < 0)
			{
				if (pos < length - 1) sb.append(source.substring(pos));
				break;
			}
			if (idx > pos) sb.append(source.substring(pos, idx));
			pos = idx + 2;
			idx = source.indexOf("}", pos);
			if (idx < 0)
			{
				err(file, line, "missing closing \"}\" on property expansion");
				return null;
			}
			String s = source.substring(pos, idx);
			if (s == null) return source;
			s = s.trim();
			if ("".equals(s)) return source;
			String value = System.getProperty(s);
			if (value == null) return source;
			sb.append(value);
			pos = idx + 1;
		}
		return sb.toString();
	}
	
	/**
	 * Print an error message.
	 * @param file the policy file in which the error was detected.
	 * @param line the line number at which the error was detected.
	 * @param msg the error description.
	 */
	private static void err(String file, int line, String msg)
	{
		System.err.println("Policy file '"+file+"', line "+line+" : "+msg);
	}
}
