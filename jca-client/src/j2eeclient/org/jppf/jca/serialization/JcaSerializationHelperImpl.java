/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.jca.serialization;

import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JcaSerializationHelperImpl extends SerializationHelperImpl
{
	/**
	 * Get the object serializer for this helper.
	 * @return an <code>ObjectSerializer</code> instance.
	 * @throws Exception if the object serializer could not be instantiated.
	 * @see org.jppf.utils.SerializationHelper#getSerializer()
	 */
	public ObjectSerializer getSerializer() throws Exception
	{
		if (serializer == null)
		{
			ClassLoader cl = getClass().getClassLoader();
			serializer = (ObjectSerializer) cl.loadClass("org.jppf.jca.serialization.JcaObjectSerializerImpl").newInstance();
		}
		return serializer;
	}
}
