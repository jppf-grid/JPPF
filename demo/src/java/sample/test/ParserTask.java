/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
