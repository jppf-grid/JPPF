/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

package sample.test.clientpool;

import java.io.*;

/**
 * 
 * @author Laurent Cohen
 */
public class PoolConfigGenerator
{
	/**
	 * Generate config properties for a connection pool.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			String driverHost = "cyffcb1-a";
			int classPort = 11111;
			int appPort = 11112;
			String jmxHost = "cyffcb1-a";
			int jmxPort = 11198;
			int priority = 1;
			Writer writer = new BufferedWriter(new FileWriter("config/gen_config.properties"));
			StringBuilder sb = new StringBuilder();
			sb.append("jppf.discovery.enabled = false\n\n");
			sb.append("jppf.drivers =");
			for (int i=1; i<=144; i++) sb.append(" ").append("d").append(i);
			sb.append("\n\n");
			for (int i=1; i<=144; i++)
			{
				String name = "d" + i;
				sb.append(name).append(".jppf.server.host = ").append(driverHost).append("\n");
				sb.append(name).append(".class.server.port = ").append(classPort).append("\n");
				sb.append(name).append(".app.server.port = ").append(appPort).append("\n");
				sb.append(name).append(".priority = ").append(priority).append("\n");
				sb.append(name).append(".jppf.management.host = ").append(jmxHost).append("\n");
				sb.append(name).append(".jppf.management.port = ").append(jmxPort).append("\n");
				sb.append("\n");
			}
			sb.append("matrix.size = 300\n");
			sb.append("task.nbRows = 1\n");
			sb.append("matrix.iterations = 50\n");
			sb.append("\n");

			writer.write(sb.toString());
			writer.flush();
			writer.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
