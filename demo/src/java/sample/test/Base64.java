/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package sample.test;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

import org.jppf.server.protocol.JPPFRunnable;

public class Base64 implements Serializable
{
	private String palabra;

	public Base64()
	{
	}

	public Base64(String p1)
	{
		palabra = p1;
	}

	@JPPFRunnable
	public String codification(Vector v1) throws NoSuchAlgorithmException
	{
		String p = null;
		int i=0;
		while (i<v1.size() && p==null)
		{
		  String palabraAux = com.sun.org.apache.xerces.internal.impl.dv.util.Base64.encode(((String)v1.elementAt(i)).getBytes());
	    if (palabraAux.equalsIgnoreCase(palabra)) p = palabraAux;
			i++;
		}
		return p;
	}
}
