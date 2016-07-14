/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.doc.jenkins;

import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Laurent Cohen
 */
public class Handler extends DefaultHandler {
  /**
   * The set of aceepted elements.
   */
  private static final Set<String> elements = new HashSet<>(Arrays.asList("failCount", "skipCount", "totalCount", "startTime", "result", "duration"));
  /**
   * The build to parse.
   */
  Build build;
  /**
   *
   */
  private String currentElement;

  @Override
  public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
    //System.out.printf("startElement(localName=%s, qName=%s)%n", localName, qName);
    currentElement = null;
    switch(qName) {
      case "build":
        build = new Build();
        break;

      case "hudson.tasks.junit.TestResultAction":
        build.setTestResults(new TestResults());
        break;

      case "failCount":
      case "skipCount":
      case "totalCount":
      case "startTime":
      case "result":
      case "duration":
        currentElement = qName;
        break;
    }
  }

  @Override
  public void endElement(final String uri, final String localName, final String qName) throws SAXException {
    //System.out.printf("endElement(localName=%s, qName=%s)%n", localName, qName);
    currentElement = null;
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException {
    if (currentElement == null) return;
    String s = new String(ch, start, length).trim();
    //System.out.printf("characters(chars=%s)%n", s);
    switch(currentElement) {
      case "failCount":
        if (!s.isEmpty()) build.getTestResults().setFailures(Integer.valueOf(s));
        break;

      case "skipCount":
        if (!s.isEmpty()) build.getTestResults().setSkipped(Integer.valueOf(s));
        break;

      case "totalCount":
        if (!s.isEmpty()) build.getTestResults().setTotalCount(Integer.valueOf(s));
        break;

      case "startTime":
        if (!s.isEmpty()) build.setStartTime(Long.valueOf(s));
        break;

      case "result":
        build.setResult(s);
        break;

      case "duration":
        if (!s.isEmpty()) build.setDuration(Long.valueOf(s));
        break;
    }
  }

  @Override
  public void error(final SAXParseException e) throws SAXException {
    e.printStackTrace();
  }

  @Override
  public void fatalError(final SAXParseException e) throws SAXException {
    e.printStackTrace();
  }
}
