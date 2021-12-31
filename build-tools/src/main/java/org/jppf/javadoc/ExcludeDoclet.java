/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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
package org.jppf.javadoc;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import com.sun.javadoc.*;
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;

/**
 * A doclet that processes an <code>@exclude</code> tag at the package, class or member level.
 * All elements marked with the <code>@exclude</code> tag in their documentation are
 * excluded from the javadoc output.
 * This code is based on the public domain code published by Chris Nokleberg at
 * <a href="http://www.sixlegs.com/blog/java/exclude-javadoc-tag.html">http://www.sixlegs.com/blog/java/exclude-javadoc-tag.html</a>.
 * @author Laurent Cohen
 */
public class ExcludeDoclet {
  /**
   * Name of the javadoc tag processed by this doclet.
   */
  private static final String TAG_NAME = "exclude";
  /**
   * Pre-initialized method invocation.
   */
  private static Method optionsValidationMethod;
  static {
    init();
  }

  /**
   * Entry point for this doclet.
   * @param args the command line options.
   */
  public static void main(final String[] args) {
    String name = ExcludeDoclet.class.getName();
    Main.execute(name, name, args);
  }

  /**
   * Determine whether command line options are valid.
   * This method delegates its processing to the standard doclet.
   * @param options the command-lines options.
   * @param reporter used to collect and report the javadoc errors.
   * @return true if the options are valid, false otherwise.
   * @throws java.io.IOException if an I/O exception occurs while processing the options.
   */
  public static boolean validOptions(final String[][] options, final DocErrorReporter reporter) throws IOException {
    try {
      return (Boolean) optionsValidationMethod.invoke(null, options, reporter);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
    //return Standard.validOptions(options, reporter);
  }

  /**
   * Determine whether command line options are valid.
   * @throws Exception if any error occurs.
   */
  private static void init() {
    try {
      final Class<?> standardClass = Class.forName("com.sun.tools.doclets.standard.Standard");
      final Class<?> errorReporterClass = Class.forName("com.sun.javadoc.DocErrorReporter");
      optionsValidationMethod = standardClass.getDeclaredMethod("validOptions", String[][].class, errorReporterClass);
      System.out.println("optionsValidationMethod = " + optionsValidationMethod);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Determine the number of arguments for each command line option (including the option name).
   * This method delegates its processing to the standard doclet.
   * @param option the option to check.
   * @return the expected number of arguments for the option.
   */
  public static int optionLength(final String option) {
    return Standard.optionLength(option);
  }

  /**
   * Entry point for this doclet.
   * This method modifies the standard doclet processing by wrapping each element in a proxy.
   * @param root the root of the document from which the javadoc is generated.
   * @return true if javadoc generation was succesful, false otherwise.
   * @throws java.io.IOException if an I/O exception occurs while generating the javadoc.
   */
  public static boolean start(final RootDoc root) throws java.io.IOException {
    try {
      System.out.println("root doc = " + root.name());
      return Standard.start((RootDoc) process(root, RootDoc.class));
    } catch (IllegalArgumentException e) {
      System.out.println("IllegalArgumentException for root doc = " + root);
      throw e;
    }
  }

  /**
   * Enable Java 1.5 language features.
   * @return <code>LanguageVersion.JAVA_1_5</code>.
   */
  public static LanguageVersion languageVersion() {
    return LanguageVersion.JAVA_1_5;
  }

  /**
   * Determine whether a package, class or memeber contains an <code>@exclude</code> tag,
   * in which case it is excluded from the javadoc output.
   * @param doc the element to check for eclude tag.
   * @return true if the element should be excluded, false otherwise.
   */
  private static boolean exclude(final Doc doc) {
    if (doc instanceof ProgramElementDoc) {
      if (((ProgramElementDoc) doc).containingPackage().tags(TAG_NAME).length > 0) return true;
    }
    return doc.tags(TAG_NAME).length > 0;
  }

  /**
   * This method modifies the standard doclet processing by wrapping each element in a proxy.
   * @param obj the element to process.
   * @param expect the expected class of the element.
   * @return a proxy to the element to process, if the element is a javadoc element (implementing the <code>Doc</code> interface).
   */
  private static Object process(final Object obj, final Class<?> expect) {
    if (obj == null) return null;
    Class<?> cls = obj.getClass();
    if (cls.getName().startsWith("com.sun.")) {
      return Proxy.newProxyInstance(cls.getClassLoader(), cls.getInterfaces(), new ExcludeHandler(obj));
    } else if (cls.isArray()) {
      Object[] array = (Object[]) obj;
      if (array.length > 0) {
        Class<?> componentType = expect.getComponentType();
        if (componentType != null) {
          List<Object> list = new ArrayList<>(array.length);
          for (int i = 0; i < array.length; i++) {
            Object entry = array[i];
            if ((entry instanceof Doc) && exclude((Doc) entry)) continue;
            list.add(process(entry, componentType));
          }
          try {
            return list.toArray((Object[]) Array.newInstance(componentType, list.size()));
          } catch (Exception e) {
            System.out.println(e.getClass().getName() + (e.getMessage() == null ? "" : ": " + e.getMessage()) + " for obj=" + obj
              + ", expect=" + expect + ", cls=" + cls + ", componentType=" + componentType + ", list=" + list);
            //e.printStackTrace(System.out);
            return null;
          }
        }
      }
    }
    return obj;
  }

  /**
   * Invocation handler used by dynamic proxies generated for javadoc elements processing.
   */
  private static class ExcludeHandler implements InvocationHandler {
    /**
     * The actual javadoc element.
     */
    private Object target;

    /**
     * Initialize this invocation handler with the specified javadoc element.
     * @param target the actual javadoc element to process.
     */
    public ExcludeHandler(final Object target) {
      this.target = target;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if (args != null) {
        String methodName = method.getName();
        if (methodName.equals("compareTo") || methodName.equals("equals") || methodName.equals("overrides") || methodName.equals("subclassOf")) {
          args[0] = unwrap(args[0]);
        }
      }
      try {
        return process(method.invoke(target, args), method.getReturnType());
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
    }

    /**
     * Obtain the actual processed element from its dynamic proxy.
     * @param proxy a dynamic proxy to the element.
     * @return the actual javadoc element to process.
     */
    private static Object unwrap(final Object proxy) {
      if (proxy instanceof Proxy) return ((ExcludeHandler) Proxy.getInvocationHandler(proxy)).target;
      return proxy;
    }
  }
}
