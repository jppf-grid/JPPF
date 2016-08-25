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

import java.lang.annotation.*;
import java.util.*;

import org.junit.runner.Runner;
import org.junit.runners.*;
import org.junit.runners.model.*;

/**
 * A suite that allows running the specified classes a specified number fof times,
 * and optionally allows a random shuffling of the test classes at each repetition.
 * <p>Example usage:
 * <pre>
 * @RunWith(RepeatingSuite.class)
 * @RepeatingSuite.RepeatingSuiteClasses(repeat=2, shuffle=false,
 *   classes={MyClass1.class, MyClass2.class,, MyClass3.class}
 * )
 * public class MySuite {
 * } 
 * </pre>
 * @author Laurent Cohen
 */
public class RepeatingSuite extends Suite {
  /**
   * Called reflectively on classes annotated with <code>@RunWith(Suite.class)</code>.
   * @param klass the root class.
   * @throws InitializationError if any error occurs.
   */
  public RepeatingSuite(final Class<?> klass) throws InitializationError {
    super(klass, getRunners(klass));
  }

  /**
   * Build the runners for the classes in the repeated suite.
   * @param suiteClass the root class.
   * @return a list of runners.
   * @throws InitializationError if any error occurs.
   */
  static List<Runner> getRunners(final Class<?> suiteClass) throws InitializationError {
    List<Runner> runners = new ArrayList<>();
    RepeatingSuiteClasses annotation = suiteClass.getAnnotation(RepeatingSuiteClasses.class);
    if (annotation == null) throw new InitializationError(String.format("class '%s' must have a RepeatingSuiteClasses annotation", suiteClass.getName()));
    int repeat = annotation.repeat();
    if (repeat <= 0) throw new InitializationError(String.format("class '%s' must have a repeat >= 1, currently %d", suiteClass.getName(), repeat));
    List<Class<?>> classes = Arrays.asList(annotation.classes());
    for (int i=0; i<repeat; i++) {
      final String suffix = String.format("[%d]", i);
      List<Class<?>> tmp = new ArrayList<>(classes);
      if (annotation.shuffle() && !tmp.isEmpty()) Collections.shuffle(tmp);
      for (final Class<?> testClass: tmp) runners.add(new BlockJUnit4ClassRunner(testClass) {
        @Override
        protected String getName() {
          return super.getName() + suffix;
        }

        @Override
        protected String testName(final FrameworkMethod method) {
          return super.testName(method) + suffix;
        }
      });
    }
    return runners;
  }

  /**
   * The {@code RepeatedSuiteClasses} annotation specifies the classes to be run, along with the number of repetitions
   * and and random shuffling of the classes, when a class annotated with {@code @RunWith(RepeatingSuite.class)} is run.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Inherited
  public @interface RepeatingSuiteClasses {
    /**
     * @return the classes to be run.
     */
    public Class<?>[] classes();
    /**
     * @return whether to shuffle the classes at each repetition.
     */
    public boolean shuffle() default false;
    /**
     * @return the number of times to repeat.
     */
    public int repeat() default 1;
  }
}
