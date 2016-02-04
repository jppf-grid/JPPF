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

package test.org.jppf.test.runner;

/**
 * Interface for rendering the results of a JUnit test run.
 * @author Laurent Cohen
 */
public interface TestResultRenderer
{
  /**
   * Get the report header.
   * @return the header as a string.
   */
  String getHeader();
  /**
   * Get the report footer.
   * @return the footer as a string.
   */
  String getFooter();
  /**
   * Get the report body.
   * @return the body as a string.
   */
  String getBody();
  /**
   * Perform the actual rendering.
   */
  void render();
  /**
   * Get the indent used by this renderer.
   * @return the indent as a string.
   */
  String getIndent();
}
