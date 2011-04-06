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

/**
 * Represents a reference that could not be set on an object's field or array element,
 * because the referenced object hasn't yet been read from the stream.
 */
class PendingArrayElementReference extends PendingReference
{
	/**
	 * Position of the element in the array.
	 */
	int pos = -1;

	/**
	 * Initialize this pending reference with the specified parameters for an array element.
	 * @param pos the position of the ekement in the array.
	 * @param o the object whose field has to be set.
	 */
	PendingArrayElementReference(int pos, Object o)
	{
		this.pos = pos;
		this.o = o;
	}
}