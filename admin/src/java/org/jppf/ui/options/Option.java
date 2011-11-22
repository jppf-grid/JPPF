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
package org.jppf.ui.options;


/**
 * Instances of this interface represent one option in an options page.
 * @author Laurent Cohen
 */
public interface Option extends OptionElement
{
	/**
	 * The value of this option.
	 * @return the value as an <code>Object</code> instance.
	 */
	Object getValue();
	/**
	 * Determine whether the value of this option should be saved in the user preferences.
	 * @return true if the value should be saved, false otherwise.
	 */
	boolean isPersistent();
}
