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

import java.lang.annotation.*;
import java.util.*;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.model.*;

/**
 * A suite that allows running the specified classes a specified number of times,
 * and optionally allows a random shuffling of the test classes at each repetition.
 * <p>Example usage:
 * <pre>
 * &#64;RunWith(RepeatingSuite.class)
 * &#64;RepeatingSuite.RepeatingSuiteClasses(
 *   repeat=2, shuffleClasses=false, shuffleMethods=false,
 *   classes={MyClass1.class, MyClass2.class, MyClass3.class}
 * )
 * public class MySuite {
 * }
 * </pre>
 * @author Laurent Cohen
 */
public class RepeatingSuite extends Suite {
  /**
   * Called reflectively on classes annotated with {@code @RunWith(RepeatingSuite.class)}.
   * @param suiteClass the root class.
   * @throws InitializationError if any error occurs.
   */
  public RepeatingSuite(final Class<?> suiteClass) throws InitializationError {
    super(suiteClass, getRunners(suiteClass));
  }

  /**
   * Build the runners for the classes in the repeated suite.
   * There is one distinct runner for each classs for each iteration,
   * resulting in {@code nbRepeat * nbClasses} runners.
   * @param suiteClass the root class.
   * @return a list of runners.
   * @throws InitializationError if any error occurs.
   */
  private static List<Runner> getRunners(final Class<?> suiteClass) throws InitializationError {
    final RepeatingSuiteClasses annotation = suiteClass.getAnnotation(RepeatingSuiteClasses.class);
    if (annotation == null) throw new InitializationError(String.format("class '%s' must have a RepeatingSuiteClasses annotation", suiteClass.getName()));
    final int repeat = annotation.repeat();
    if (repeat <= 0) throw new InitializationError(String.format("class '%s' must have a repeat >= 1, currently %d", suiteClass.getName(), repeat));
    final List<Class<?>> classes = Arrays.asList(annotation.classes());
    System.out.printf("Repeating tests with repeat=%d, shuffleClasses=%b, shuffleMethods=%b, classes=%s%n", repeat, annotation.shuffleClasses(), annotation.shuffleMethods(), classes);
    final List<Runner> runners = new ArrayList<>(repeat * classes.size());
    // compute the max number of digits for the iteration numbers
    final int nbDigits = (repeat > 1) ? (int) Math.ceil(Math.log10(repeat)) : 1;
    // iteration numbers are 0-padded up to the max number of digits
    final String format = "[%0" + nbDigits + "d]";
    for (int i=0; i<repeat; i++) {
      final String suffix = String.format(format, i);
      final List<Class<?>> tmp = new ArrayList<>(classes);
      if (annotation.shuffleClasses() && !tmp.isEmpty()) Collections.shuffle(tmp);
      for (final Class<?> testClass: tmp) {
        runners.add(new BlockJUnit4ClassRunner(testClass) {
          @Override
          protected String getName() {
            return super.getName() + suffix;
          }

          @Override
          protected String testName(final FrameworkMethod method) {
            return super.testName(method) + suffix;
          }

          @Override
          protected List<FrameworkMethod> getChildren() {
            final List<FrameworkMethod> children = super.getChildren();
            if (annotation.shuffleMethods()) Collections.shuffle(children);
            return children;
          }
        });
      }
    }
    return runners;
  }

  /**
   * The {@code RepeatedSuiteClasses} annotation specifies the classes to be run, along with the number of repetitions
   * and random shuffling of the classes, when a class annotated with {@code @RunWith(RepeatingSuite.class)} is run.
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
    public boolean shuffleClasses() default false;
    /**
     * @return whether to shuffle the methods within a class at each repetition.
     */
    public boolean shuffleMethods() default false;
    /**
     * @return the number of times to repeat.
     */
    public int repeat() default 1;
  }

  @Override
  protected List<Runner> getChildren() {
    return super.getChildren();
  }
}
