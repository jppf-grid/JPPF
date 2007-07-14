/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package sample.test;

import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This task implementation is used for testing the parsing of an xml document.
 * @author Laurent Cohen
 */
public class ParserTask extends JPPFTestTask
{
	/**
	 * Path to the XML file.
	 */
	private String filePath = null;

	/**
	 * Initialize this task with the specified xml file path.
	 * @param filePath the client side path of the xml file to parse.
	 */
	public ParserTask(String filePath)
	{
		this.filePath = filePath;
	}

	/**
	 * Do the parsing.
	 * @throws Exception if parsing fails.
	 * @see java.lang.Runnable#run()
	 */
	public void testParsing() throws Exception
	{
		 System.out.println("Started Parser for "+filePath+" ...");
		 fileParser(filePath);
		 System.out.println("Finished Parser for "+filePath+" ..."); 
	}

	/**
	 * Parse the file.
	 * @param filetoParse path of the file to parse in the cleint file system.
	 * @throws Exception if parsing fails.
	 */
	public void fileParser(String filetoParse) throws Exception
	{
		InputStream is = getClass().getClassLoader().getResourceAsStream(filetoParse);
		InputSource src = new InputSource(is);
		// Creating XMLReader instance
		XMLReader reader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
		/*
		reader.setContentHandler(brsaxParser);
		reader.setErrorHandler(brsaxParser);
		// Parsing the XML file
		reader.parse(src);
		*/
	}
}
