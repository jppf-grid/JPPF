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

package org.jppf.fileserver;

import java.util.Properties;

import org.jppf.utils.TypedProperties;

/**
 * Abstract implementation of the {@link ConfigurableAdapter} interface.
 * @author Laurent Cohen
 */
public abstract class ConfigurableAdapter implements Configurable
{
	/**
	 * The configuration parameters.
	 */
	protected TypedProperties configuration;

	/**
	 * {@inheritDoc}
	 */
	public void configure(Properties configuration)
	{
		this.configuration = new TypedProperties(configuration);
	}

	/**
	 * Get the configuration parameters.
	 * @return the configuraton as a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getConfiguration()
	{
		return configuration;
	}
}
