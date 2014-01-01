/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
import java.util.List;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;

import org.xml.sax.*;


/**
 * Utility class to validate an XML document against an XML schema.
 * @author Laurent Cohen
 * @exclude
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
  public static void main(final String...args)
  {
    try
    {
      if ((args == null) || (args.length != 2)) usageAndExit();
      List<String> docPaths = FileUtils.getFilePathList(args[1]);
      for (String path: docPaths)
      {
        JPPFErrorReporter reporter = new JPPFErrorReporter(path);
        SchemaValidator validator = new SchemaValidator(reporter);
        boolean b = validator.validate(path, args[0]);
        String s = "the document " + path;
        System.out.println(s + (b ? " is valid." : " has errors."));
        if (!b)
        {
          System.out.println("fatal errors: " + reporter.allFatalErrorsAsStrings());
          System.out.println("errors      : " + reporter.allErrorsAsStrings());
          System.out.println("warnings    : " + reporter.allWarningsAsStrings());
        }
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
  public SchemaValidator(final JPPFErrorReporter reporter)
  {
    this.reporter = (reporter == null)  ? new JPPFErrorReporter("Error Reporter") : reporter;
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
  public Schema loadSchema(final String schemaPath) throws IOException, SAXException
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
  public Schema loadSchema(final InputStream schemaStream) throws IOException, SAXException
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
  public Schema loadSchema(final Reader schemaReader) throws IOException, SAXException
  {
    if (schemaReader == null) return null;
    return getSchemaFactory().newSchema(new StreamSource(schemaReader));
  }

  /**
   * Validate an XML document in a file against an XML schema.
   * @param docPath the path to the XML document.
   * @param schemaPath the path to the XML schema.
   * @return true if the XML document is valid, false otherwise.
   * @throws IOException if an IO error occurs while looking up one of the files.
   * @throws SAXException if an error occurs while loading the schema or validating the document.
   */
  public boolean validate(final String docPath, final String schemaPath) throws IOException, SAXException
  {
    return validate(FileUtils.getFileReader(docPath), FileUtils.getFileReader(schemaPath));
  }

  /**
   * Validate an XML document from a reader against an XML schema.
   * @param docReader the reader used to access the document.
   * @param schemaReader the path to the XML schema.
   * @return true if the XML document is valid, false otherwise.
   * @throws IOException if an IO error occurs while looking up one of the files.
   * @throws SAXException if an error occurs while loading the schema or validating the document.
   */
  public boolean validate(final Reader docReader, final Reader schemaReader) throws IOException, SAXException
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
    synchronized(SchemaValidator.class)
    {
      if (sf == null)
      {
        sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        sf.setErrorHandler(new ValidatorErrorHandler(null));
      }
    }
    return sf;
  }

  /**
   * Print a trace of the specified sax parsing exception.
   * @param e the exception to print.
   * @param name an identifier for the document being parsed.
   * @return the resulting message as a string.
   */
  public String printSAXParseException(final SAXParseException e, final String name)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("Document ").append(name).append(" at ");
    sb.append(e.getLineNumber()).append(':').append(e.getColumnNumber()).append('\n');
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
    public ValidatorErrorHandler(final JPPFErrorReporter reporter)
    {
      this.reporter = (reporter == null) ? new JPPFErrorReporter("Error Reporter") : reporter;
    }

    /**
     * Receive notification of a recoverable error.
     * @param exception encapsulates the XML error.
     * @throws SAXException any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(final SAXParseException exception) throws SAXException
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
    @Override
    public void fatalError(final SAXParseException exception) throws SAXException
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
    @Override
    public void warning(final SAXParseException exception) throws SAXException
    {
      warningCount++;
      reporter.warnings.add(printSAXParseException(exception, reporter.name));
    }
  }
}
