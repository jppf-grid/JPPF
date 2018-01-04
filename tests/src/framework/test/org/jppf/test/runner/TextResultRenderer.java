/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package test.org.jppf.test.runner;

import java.io.*;
import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

/**
 * 
 * @author Laurent Cohen
 */
public class TextResultRenderer extends AbstractTestResultRenderer {
  /**
   * Initialize this renderer witht he specified results.
   * @param result the results to render.
   */
  public TextResultRenderer(final ResultHolder result) {
    super(result);
    this.indent = "  ";
  }

  @Override
  public void render() {
    renderHeader();
    renderBody();
  }

  /**
   * Render the header.
   */
  private void renderHeader() {
    header.append("\nTotal tests: ").append(result.getTestsCount());
    header.append(", successful: ").append(result.getSuccessCount());
    header.append(", failed: ").append(result.getFailureCount());
    header.append(", ignored: ").append(result.getIgnoredCount());
    header.append('\n');
    header.append("Start time: ").append(new Date(result.getStartTime()));
    header.append(", total elapsed: ").append(StringUtils.toStringDuration(result.getEndTime() - result.getStartTime()));
    header.append('\n');
    header.append('\n');
  }

  /**
   * Render the header.
   */
  private void renderBody() {
    if (!result.getExceptions().isEmpty()) {
      body.append("The following exceptions occurred before the test run:\n\n");
      for (ExceptionHolder exh: result.getExceptions()) renderException(exh);
    }
    body.append("Tests results:\n\n");
    for (final String className: result.getClasses()) {
      body.append("class ").append(className).append('\n');
      incIndentation();
      final Collection<Failure> failures = result.getFailures().getValues(className);
      if (failures != null) {
        for (final Failure failure: failures) renderFailure(failure);
      }
      Collection<Description> descriptions = result.getSuccesses().getValues(className);
      if (descriptions != null) {
        for (final Description d: descriptions) renderDescription(d, "OK");
      }
      descriptions = result.getIngored().getValues(className);
      if (descriptions != null) {
        for (final Description d: descriptions) renderDescription(d, "Ignored");
      }
      decIndentation();
    }
  }

  /**
   * Render an exception.
   * @param exh the object holding the exception.
   */
  private void renderException(final ExceptionHolder exh) {
    body.append(getIndentation()).append(exh.getClassName()).append('\n');
    incIndentation();
    final String s = ExceptionUtils.getStackTrace(exh.getThrowable());
    body.append(indent(s, getIndentation())).append("\n\n");
    decIndentation();
  }

  /**
   * Render the specified failure.
   * @param failure the failure to render.
   */
  private void renderFailure(final Failure failure) {
    renderDescription(failure.getDescription(), "Failure");
    if (failure.getException() != null) {
      incIndentation();
      final String s = ExceptionUtils.getStackTrace(failure.getException());
      body.append(indent(s, getIndentation())).append("\n");
      decIndentation();
    }
  }

  /**
   * Render the specified description.
   * @param desc the description to render.
   * @param type either "OK", "ignored" or "Failure".
   */
  private void renderDescription(final Description desc, final String type) {
    body.append(getIndentation()).append(desc.getMethodName()).append("() : ").append(type).append('\n');
  }


  /**
   * Indent the specified source string by added the specified
   * indentation at the start of each of its lines.
   * @param source the osurce string to indent.
   * @param indentation the indentation to use.
   * @return the indented string.
   */
  private static String indent(final String source, final String indentation) {
    if (source == null) throw new IllegalArgumentException("source can't be null");
    if (indentation == null) throw new IllegalArgumentException("indentation can't be null");
    final StringBuilder sb = new StringBuilder();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new StringReader(source));
      String s;
      while ((s = reader.readLine()) != null) {
        if (s.indexOf("at org.junit.Assert") >= 0) continue;
        sb.append(indentation).append(s).append('\n');
      }
      //boolean endsWithNewline = false;
      //if (!endsWithNewline) sb.deleteCharAt(sb.length()-1);
      while (true) {
        final char c = sb.charAt(sb.length()-1);
        if ((c == '\n') || (c == '\r')) sb.deleteCharAt(sb.length()-1);
        else break;
      }
    } catch(@SuppressWarnings("unused") final Exception e) {
    } finally {
      if (reader != null) StreamUtils.closeSilent(reader);
    }
    return sb.toString();
  }
}
