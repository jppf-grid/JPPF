/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.scripting;

/**
 * Factory used to instantiate script runners.
 * @author Laurent Cohen
 */
public final class ScriptRunnerFactory
{
	/**
	 * Instantiation of this class is not allowed.
	 */
	private ScriptRunnerFactory()
	{
	}

	/**
	 * Instantiate a script runner based on the specified script language.
	 * @param language the name of the script language to use.
	 * @return A <code>ScriptRunner</code> instance, or null if no known sciprt runner
	 * exists for the specified language.
	 */
	public static ScriptRunner makeScriptRunner(String language)
	{
		return new RhinoScriptRunner();
	}
}
