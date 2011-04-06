/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.utils.streams.serialization;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;

/**
 * 
 * @author Laurent Cohen
 */
public class GetFieldValueAction implements PrivilegedAction<Object>
{
	/**
	 * The field to use to get the value.
	 */
	private Field field;
	/**
	 * The object to set the field value on.
	 */
	private Object o;
	/**
	 * An eventual exception thrown by this action.
	 */
	private Exception e = null;

	/**
	 * Initialize this action with the specified parameters.
	 * @param field the field to use to get the value.
	 * @param o the object to set the field value on.
	 */
	public GetFieldValueAction(Field field, Object o)
	{
		this.field = field;
		this.o = o;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object run()
	{
		try
		{
			if (!field.isAccessible()) field.setAccessible(true);
			return field.get(o);
		}
		catch (Exception e)
		{
			this.e = e;
		}
		return null;
	}

	/**
	 * Get an eventual exception thrown by this action.
	 * @return an {@link Exception} object.
	 */
	public Exception getException()
	{
		return e;
	}
}
