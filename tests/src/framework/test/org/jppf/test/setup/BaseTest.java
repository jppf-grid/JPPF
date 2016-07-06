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

package test.org.jppf.test.setup;

import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 *
 * @author Laurent Cohen
 */
public class BaseTest {
  /** */
  private static String name;
  /** */
  @Rule
  public TestWatcher testWatcher = new TestWatcher() {

    @Override
    protected void starting(final Description description) {
      String className = description.getClassName();
      if (!className.equals(name)) {
        name = className;
        System.out.printf("***** test class %s *****%n", className);
      }
      if (description.isTest()) {
        System.out.printf("***** start of %s() *****%n", description.getMethodName());
      }
    }
  };
}
