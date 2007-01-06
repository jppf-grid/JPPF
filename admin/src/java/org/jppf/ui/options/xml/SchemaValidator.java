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
package org.jppf.ui.options.xml;

import java.io.*;
import java.util.List;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import org.jppf.utils.FileUtils;
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
	 * Used to collect validation error and warning messages.
	 */
	private JPPFErrorReporter reporter = null;

	/**
	 * Entry point to test this class.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			if ((args == null) || (args.length != 2)) usageAndExit();

			List<String> docPaths = FileUtils.getFilePathList(args[1]);
			for (String path: docPaths)
			{
				SchemaValidator validator = new SchemaValidator(new JPPFErrorReporter(path));
				boolean b = validator.validate(path, args[0]);
				String s = "the document " + path;
				System.out.println(s + (b ? " is valid." : " has errors."));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

	/**
	 * Show usage instructions for this validation toll, and exit the JVM.
	 */
	public static void usageAndExit()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Usage:\n\n");
		sb.append("    SchemaValidator <xml schema> <xml document list>\n\n");
		sb.append("Where:\n\n");
		sb.append("  - xml schema is the location of the XML Schema to validate against\n");
		sb.append("  - xml schema is the location of the a text file containing the list\n");
		sb.append("    of XML documents to validate\n\n");
		sb.append("Any file location, including those in the document list, can be either\n");
		sb.append("in the file system or in the classpath.");
		System.out.println(sb.toString());
		System.exit(1);
	}

	/**
	 * Initialize this validator with the specified error reporter.
	 * @param reporter used to collect validation error and warning messages.
	 */
	public SchemaValidator(JPPFErrorReporter reporter)
	{
		if (reporter == null) this.reporter = new JPPFErrorReporter("Error Reporter");
		else this.reporter = reporter;
	}

	/**
	 * Load an XML schema from the specified path.
	 * This method looks up the schema first in the file system, then in the classpath
	 * if it is not found in the file system.
	 * @param schemaPath the path to the schema to load.
	 * @return a <code>Schema</code> instance, or null if the schema file could not be found.
	 * @throws IOException if an IO error occurs while looking up the schema file.
	 * @throws SAXException if an error occurs while loading the schema.
	 */
	public Schema loadSchema(String schemaPath) throws IOException, SAXException
	{
		InputStream is = FileUtils.getFileInputStream(schemaPath);
		if (is == null) return null;
		Schema schema = getSchemaFactory().newSchema(new StreamSource(is));
		return schema;
	}

	/**
	 * Load an XML schema from the specified input stream.
	 * This method looks up the schema first in the file system, then in the classpath
	 * if it is not found in the file system.
	 * @param schemaStream the input stream to load the schema from.
	 * @return a <code>Schema</code> instance, or null if the schema file could not be found.
	 * @throws IOException if an IO error occurs while looking up the schema file.
	 * @throws SAXException if an error occurs while loading the schema.
	 */
	public Schema loadSchema(InputStream schemaStream) throws IOException, SAXException
	{
		if (schemaStream == null) return null;
		return getSchemaFactory().newSchema(new StreamSource(schemaStream));
	}

	/**
	 * Load an XML schema from the specified reader.
	 * This method looks up the schema first in the file system, then in the classpath
	 * if it is not found in the file system.
	 * @param schemaReader the reader to load the schema from.
	 * @return a <code>Schema</code> instance, or null if the schema file could not be found.
	 * @throws IOException if an IO error occurs while looking up the schema file.
	 * @throws SAXException if an error occurs while loading the schema.
	 */
	public Schema loadSchema(Reader schemaReader) throws IOException, SAXException
	{
		if (schemaReader == null) return null;
		return getSchemaFactory().newSchema(new StreamSource(schemaReader));
	}

	/**
	 * Validate an XML document in a file against an XML schema.
	 * @param docPath the path to the XML document.
	 * @param schemaPath the path to the XML schema.
	 * @return true if the XML docuement is valid, false otherwise.
	 * @throws IOException if an IO error occurs while looking up one of the files.
	 * @throws SAXException if an error occurs while loading the schema or validating the document.
	 */
	public boolean validate(String docPath, String schemaPath) throws IOException, SAXException
	{
		return validate(FileUtils.getFileReader(docPath), FileUtils.getFileReader(schemaPath));
	}

	/**
	 * Validate an XML document from a reader against an XML schema.
	 * @param docReader the reader used to access the document.
	 * @param schemaReader the path to the XML schema.
	 * @return true if the XML docuement is valid, false otherwise.
	 * @throws IOException if an IO error occurs while looking up one of the files.
	 * @throws SAXException if an error occurs while loading the schema or validating the document.
	 */
	public boolean validate(Reader docReader, Reader schemaReader) throws IOException, SAXException
	{
		if (docReader == null) return false;
		Schema schema = loadSchema(schemaReader);
		if (schema == null) return false;
		Validator validator = schema.newValidator();
		ValidatorErrorHandler handler = new ValidatorErrorHandler(reporter);
		validator.setErrorHandler(handler);
		validator.validate(new SAXSource(new InputSource(docReader)));
		return handler.errorCount + handler.fatalCount <= 0;
	}

	/**
	 * Get the schema factory for this validator.
	 * @return a <code>SchemaFactory</code> instance.
	 */
	public SchemaFactory getSchemaFactory()
	{
		if (sf == null)
		{
			sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			sf.setErrorHandler(new ValidatorErrorHandler(null));
		}
		return sf;
	}

	/**
	 * Print a trace of the specified sax parsing exception.
	 * @param e the exception to print.
	 * @param name an identifier for the document being parsed.
	 * @return the resulting message as a string.
	 */
	public String printSAXParseException(SAXParseException e, String name)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Document ").append(name).append(" at ");
		sb.append(e.getLineNumber()).append(":").append(e.getColumnNumber()).append("\n");
		sb.append(e.getMessage());
		return sb.toString();
	}

	/**
	 * Error handler for XML validation and parsing operations.
	 */
	private class ValidatorErrorHandler implements ErrorHandler
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
		 * Used to collect validation error and warning messages.
		 */
		public JPPFErrorReporter reporter = null;

		/**
		 * Initialize this error handler with the specified error reporter.
		 * @param reporter used to collect validation error and warning messages.
		 */
		public ValidatorErrorHandler(JPPFErrorReporter reporter)
		{
			if (reporter == null) this.reporter = new JPPFErrorReporter("Error Reporter");
			else this.reporter = reporter;
		}

		/**
		 * Receive notification of a recoverable error.
		 * @param exception encapsulates the XML error.
		 * @throws SAXException any SAX exception, possibly wrapping another exception.
		 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		 */
		public void error(SAXParseException exception) throws SAXException
		{
			errorCount++;
			reporter.errors.add(printSAXParseException(exception, reporter.name));
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
			reporter.fatalErrors.add(printSAXParseException(exception, reporter.name));
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
			reporter.warnings.add(printSAXParseException(exception, reporter.name));
		}
	}
}
