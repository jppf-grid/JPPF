/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.ui.options.xml;

import java.io.*;
import java.net.URL;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import org.xml.sax.*;


/**
 * Utility class to validate an XML docuement against an XML schema.
 * @author Laurent Cohen
 */
public class SchemaValidator
{
	/**
	 * Schema factory instance used to load XML schemas. 
	 */
	private static SchemaFactory sf = null;

	/**
	 * Entry point to test this class.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			// "org/jppf/ui/options/xml/option.xsd"
			// "org/jppf/ui/options/xml/AdminPage.xml"
			if ((args == null) || (args.length < 2))
				throw new Exception("There must be at least 2 arguments");

			for (int i=1; i<args.length; i++)
			{
				boolean b = validate(args[i], args[0]);
			
				if (b) System.out.println("the document "+args[i]+" is valid.");
				else System.out.println("the document "+args[i]+" has errors.");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * Load a file from the specified path.
	 * This method looks up the schema first in the file system, then in the classpath
	 * if it is not found in the file system.
	 * @param path the path to the file to load.
	 * @return a <code>InputStream</code> instance, or null if the schema file could not be found.
	 * @throws IOException if an IO error occurs while looking up the file.
	 */
	public static InputStream findFile(String path) throws IOException
	{
		InputStream is = null;
		File file = new File(path);
		if (file.exists()) is = new FileInputStream(file);
		if (is == null)
		{
			URL url = SchemaValidator.class.getClassLoader().getResource(path);
			is = (url == null) ? null : url.openStream();
			//is = SchemaValidator.class.getClassLoader().getResourceAsStream(path);
		}
		return is;
	}

	/**
	 * Load an XML schema from the specified path.
	 * This method looks up the schema first in the file system, then in the classpath
	 * if it is not found in the file system.
	 * @param path the path to the schema to load.
	 * @return a <code>Schema</code> instance, or null if the schema file could not be found.
	 * @throws IOException if an IO error occurs while looking up the schema file.
	 * @throws SAXException if an error occurs while loading the schema.
	 */
	public static Schema loadSchema(String path) throws IOException, SAXException
	{
		InputStream is = findFile(path);
		if (is == null) return null;
		Schema schema = getSchemaFactory().newSchema(new StreamSource(is));
		return schema;
	}

	/**
	 * Validate an XML schema against an XML schema.
	 * @param docPath the path to the XML document.
	 * @param schemaPath the path to the XML schema.
	 * @return true if the XML docuement is valid, false otherwise.
	 * @throws IOException if an IO error occurs while looking up one of the files.
	 * @throws SAXException if an error occurs while loading the schema or validating the document.
	 */
	public static boolean validate(String docPath, String schemaPath) throws IOException, SAXException
	{
		InputStream docIs = findFile(docPath);
		if (docIs == null) return false;
		Schema schema = loadSchema(schemaPath);
		if (schema == null) return false;
		Validator validator = schema.newValidator();
		ValidatorErrorHandler handler = new ValidatorErrorHandler();
		validator.setErrorHandler(handler);
		validator.validate(new SAXSource(new InputSource(docIs)));
		return handler.errorCount + handler.fatalCount <= 0;
	}

	/**
	 * Error handler for XML validation and parsing operations.
	 */
	private static class ValidatorErrorHandler implements ErrorHandler
	{
		/**
		 * Number of recoverable errors.
		 */
		public int errorCount = 0;
		/**
		 * Number of non-recoverable errors.
		 */
		public int fatalCount = 0;
		/**
		 * Number of warnings.
		 */
		public int warningCount = 0;
		/**
		 * Receive notification of a recoverable error.
		 * @param exception encapsulates the XML error.
		 * @throws SAXException any SAX exception, possibly wrapping another exception.
		 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		 */
		public void error(SAXParseException exception) throws SAXException
		{
			errorCount++;
			printSAXParseException(exception);
		}

		/**
		 * Receive notification of a non-recoverable error.
		 * @param exception encapsulates the XML error.
		 * @throws SAXException any SAX exception, possibly wrapping another exception.
		 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		public void fatalError(SAXParseException exception) throws SAXException
		{
			fatalCount++;
			printSAXParseException(exception);
		}

		/**
		 * Receive notification of a warning.
		 * @param exception encapsulates the XML warning.
		 * @throws SAXException any SAX exception, possibly wrapping another exception.
		 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
		 */
		public void warning(SAXParseException exception) throws SAXException
		{
			warningCount++;
			printSAXParseException(exception);
		}
	}

	/**
	 * Print a trace of the specified sax parsing exception.
	 * @param e the exception to print.
	 */
	public static void printSAXParseException(SAXParseException e)
	{
		e.printStackTrace();
	}

	/**
	 * Get the schema factory for this validator.
	 * @return a <code>SchemaFactory</code> instance.
	 */
	public static SchemaFactory getSchemaFactory()
	{
		if (sf == null)
		{
			sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			sf.setErrorHandler(new ValidatorErrorHandler());
		}
		return sf;
	}
}
