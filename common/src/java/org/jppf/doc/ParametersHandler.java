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

package org.jppf.doc;

import static org.jppf.doc.ParameterNames.*;

import java.util.*;

/**
 * Instances of this class handle the command-line options used by the template-based doc generator.
 * @author Laurent Cohen
 */
public class ParametersHandler
{
	/**
	 * Convert the command line arguments into a parameters map.
	 * @param args the arguments to convert.
	 * @return a mmaping of parameter names to object values.
	 * @throws Exception if an error occurs while parsing the arguments.
	 */
	public Map<ParameterNames, Object> parseArguments(final String...args) throws Exception
	{
		if ((args == null) || (args.length < 3)) throw new IllegalArgumentException("not enough arguments there must be at least the 3 arguments: -s sourceDir -d destDir -t templatesDir");
		Map<ParameterNames, Object> map = new EnumMap<ParameterNames, Object>(ParameterNames.class);
		int i = 0;
		while (i < args.length)
		{
			String cmd = args[i++];
			if (!cmd.startsWith("-")) throw new IllegalArgumentException("unknown option '" + cmd + "', all options must start with '-'");
			else if ("-s".equals(cmd)) map.put(SOURCE_DIR, args[i++]);
			else if ("-d".equals(cmd)) map.put(DEST_DIR, args[i++]);
			else if ("-t".equals(cmd)) map.put(TEMPLATES_DIR, args[i++]);
			else if ("-fi".equals(cmd)) map.put(FILE_INCLUDES, parseCommaSeparatedNames(args[i++]));
			else if ("-fe".equals(cmd)) map.put(FILE_EXCLUDES, parseCommaSeparatedNames(args[i++]));
			else if ("-di".equals(cmd)) map.put(DIR_INCLUDES, parseCommaSeparatedNames(args[i++]));
			else if ("-de".equals(cmd)) map.put(DIR_EXCLUDES, parseCommaSeparatedNames(args[i++]));
			else if ("-r".equals(cmd)) map.put(RECURSIVE, true);
			else throw new IllegalArgumentException("unknown option '" + cmd + '\'');
		}
		List<String> errors = new ArrayList<String>();
		if (map.get(SOURCE_DIR) == null) errors.add("missing option '-s sourceDir'");
		if (map.get(DEST_DIR) == null) errors.add("missing option '-d destDir'");
		if (map.get(TEMPLATES_DIR) == null) errors.add("missing option '-t templatesDir'");
		if (!errors.isEmpty())
		{
			StringBuilder sb = new StringBuilder("command-line errors:");
			for (String s: errors) sb.append("\n  ").append(s);
			throw new IllegalArgumentException(sb.toString());
		}
		if (map.get(RECURSIVE) == null) map.put(RECURSIVE, false);
		if (map.get(FILE_INCLUDES) == null) map.put(FILE_INCLUDES, JPPFFileFilter.DEFAULT_INCLUDES);
		if (map.get(DIR_EXCLUDES) == null) map.put(DIR_EXCLUDES, JPPFDirFilter.DEFAULT_EXCLUDES);

		return map;
	}

	/**
	 * Parse a list of comma-separated names into an array of strings.
	 * @param source the string to parse.
	 * @return an array of strings.
	 */
	private static String[] parseCommaSeparatedNames(final String source)
	{
		if (source == null) return null;
		String[] result = source.split(",");
		for (int i=0; i<result.length; i++) result[i] = result[i].trim();
		return result;
	}
}
