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

package org.jppf.utils;

import java.io.*;

/**
 * Utility class that splits a text file into smaller ones with the same number of lines.
 * @author Laurent Cohen
 */
public class TexFileSplitter
{
	/**
	 * A map of the specified options and their values.
	 */
	private static TypedProperties props = new TypedProperties();

	/**
	 * Split the input text files into output text files according to the specified options.
	 * @param args specifies the options in the format -&lt;option1&gt; &lt;value1&gt; ... -&lt;optionN&gt; &lt;valueN&gt;.
	 */
	public static void main(final String[] args)
	{
		try
		{
			processArguments(args);
			File file = new File(props.getString("inputFile"));
			BufferedReader reader = new BufferedReader(new FileReader(file));
			int lines = 0;
			String s = "";
			while (s != null)
			{
				s = reader.readLine();
				if (s != null) lines++;
			}
			reader.close();
			System.out.println("counted " + lines  + " lines");
			reader = new BufferedReader(new FileReader(file));
			int nbFiles = props.getInt("nbFiles");
			int n = lines/nbFiles;
			for (int i=0; i<nbFiles; i++)
			{
				int nb = (i < nbFiles - 1) ? n : lines - (nbFiles - 1)*n;
				s = "";
				File out = new File(props.getString("outputDir") + '/' + props.getString("prefix") + '-' + i + props.getString("extension"));
				if (!out.getParentFile().mkdirs()) throw new IOException("could not create folder " + out.getParentFile());
				BufferedWriter writer = new BufferedWriter(new FileWriter(out));
				for (int j=0; j<nb && s != null; j++)
				{
					s = reader.readLine();
					if (s != null) writer.write(s + '\n');
				}
				writer.flush();
				writer.close();
				System.out.println("created file '" + out.getName() + '\'');
			}
			reader.close();
			System.out.println("wrote all files");
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			displayOptions();
		}
	}

	/**
	 * Process the command-line options
	 * @param args contains the specified options and their value.
	 * @throws Exception if any error occurs while parsing the options.
	 */
	private static void processArguments(final String...args) throws Exception
	{
		if ((args == null) || (args.length < 1)) throw new Exception("you must specify at least the input file (-i option)");
		int i = 0;
		while (i <args.length)
		{
			String s = args[i++];
			if (StringUtils.isOneOf(s, false, "-?", "-h", "-help")) throw new Exception("\nFile splitter help");
			else if ("-i".equals(s)) props.setProperty("inputFile", args[i++]);
			else if ("-o".equals(s)) props.setProperty("outputDir", args[i++]);
			else if ("-n".equals(s)) props.setProperty("nbFiles", args[i++]);
			else if ("-p".equals(s)) props.setProperty("prefix", args[i++]);
			else if ("-e".equals(s)) props.setProperty("extension", args[i++]);
		}
		if (props.getString("inputFile", null) == null) throw new Exception("missing mandatory '-i' option");
		if (props.getString("outputDir", null) == null) props.setProperty("outputDir", new File(props.getString("inputFile")).getParentFile().getName());
		if (props.getString("nbFiles", null) == null) props.setProperty("nbFiles", "10");
		if (props.getString("prefix", null) == null) props.setProperty("prefix", "part");
		if (props.getString("extension", null) == null) props.setProperty("extension", ".txt");
	}

	/**
	 * Display the list of options and their meaning.
	 */
	public static void displayOptions()
	{
		//System.out.println("\nFile splitter help");
		System.out.println("command-line format: java -cp <classpath> org.jppf.utils.FileSplitter -option_1 value_1 ... -option_n value_n");
		System.out.println("Available options:");
		System.out.println("  -i input_file             : the path to the text file to split");
		System.out.println("  -o output_dir             : the output directory where to create the resulting files");
		System.out.println("                              no value means same directory as input file");
		System.out.println("  -n nb_files               : number of files to split the input into");
		System.out.println("                              if not specified, 10 is used");
		System.out.println("  -p output_files_prefix    : prefix for the resulting files names");
		System.out.println("  -e output_files_extension : extension for the resulting files names");
		System.out.println("  -?, -h, -help             : display this help screen (all other options are ignored)");
		System.out.println("Examples:");
		System.out.println("java -cp <classpath> org.jppf.utils.FileSplitter -i logfile.txt -o ./split -n 5 -p split-log -e .log");
		System.out.println("splits the file logfile.txt into 5, resulting files will be split-log-0.log ... split-log-5.log in folder ./split");
		System.out.println("java -cp <classpath> org.jppf.utils.FileSplitter -i logfile.txt");
		System.out.println("splits the file logfile.log into 10, resulting files will be part-0.txt ... part-9.txt in the same folder");
	}
}
