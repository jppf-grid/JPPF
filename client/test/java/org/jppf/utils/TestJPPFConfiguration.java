/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.utils;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

/**
 * Unit test for the <code>JPPFConfiguration</code> class.
 * @author Laurent Cohen
 */
public class TestJPPFConfiguration
{
	/**
	 * Invocation of the <code>JPPFClient()</code> constructor.
	 * @throws Exception if any error occurs
	 */
	@Test
	public void testAlternateConfigurationSource() throws Exception
	{
		JPPFConfiguration.ConfigurationSource source  = new TestConfigurationSource();
		System.setProperty(JPPFConfiguration.CONFIG_PROPERTY, "");
		System.setProperty(JPPFConfiguration.CONFIG_PLUGIN_PROPERTY, source.getClass().getName());
		JPPFConfiguration.reset();
		TypedProperties config = JPPFConfiguration.getProperties();
		assertNotNull(config);
		String s  = config.getString("jppf.config.source.origin", null);
		assertNotNull(s);
		assertEquals(s, "string");
	}

	/**
	 * Test implementation of alternate configuration source.
	 */
	public static class TestConfigurationSource implements JPPFConfiguration.ConfigurationSource
	{
		/**
		 * Public noargs constructor.
		 */
		public TestConfigurationSource()
		{
		}

		/**
		 * {@inheritDoc}
		 */
		public InputStream getPropertyStream() throws IOException
		{
			String props = "jppf.config.source.origin = string";
			JPPFBuffer buffer = new JPPFBuffer(props);
			return new ByteArrayInputStream(buffer.getBuffer());
		}
	};
}
