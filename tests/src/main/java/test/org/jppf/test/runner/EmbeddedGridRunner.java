/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.*;

import test.org.jppf.test.setup.BaseSetup;

/**
 * A test class runner which ignores test methods annotated with {@link IgnoreForEmbeddedGrid @IgnoreForEmbeddedGrid}.
 * @author Laurent Cohen
 */
public class EmbeddedGridRunner extends BlockJUnit4ClassRunner {
  /**
   * Creates a EmbeddedGridRunner to run a test class.
   * @param klass the test class to run.
   * @throws InitializationError if any erorr occurs.
   */
  public EmbeddedGridRunner(final Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected boolean isIgnored(final FrameworkMethod child) {
    if (super.isIgnored(child)) return true;
    if (!BaseSetup.isTestWithEmbeddedGrid()) return false;
    if (child.getAnnotation(IgnoreForEmbeddedGrid.class) != null) return true;
    Class<?> clazz = child.getDeclaringClass();
    while (clazz != null) {
      if (clazz.getAnnotation(IgnoreForEmbeddedGrid.class) != null) return true;
      clazz = clazz.getSuperclass();
    }
    return false;
  }
}
