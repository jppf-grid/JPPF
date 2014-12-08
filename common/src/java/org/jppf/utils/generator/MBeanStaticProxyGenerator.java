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

package org.jppf.utils.generator;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

import org.jppf.management.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.*;

/**
 * This class generates the code of a static proxy for one or more specified MBean interfaces using Java reflection.
 * @author Laurent Cohen
 */
public class MBeanStaticProxyGenerator {
  /**
   * The string used for a single indentation level.
   */
  private final static String INDENT_STRING = "  ";
  /**
   * Suffix appended to the MBean interface name to compute the name of the generated class.
   */
  private final static String CLASS_NAME_SUFFIX = "StaticProxy";
  /**
   * The set of imports to generate.
   */
  private final Set<String> importSet = new TreeSet<>();
  /**
   * The class object for the MBean interface.
   */
  private Class<?> inf;
  /**
   * Holds the generated file header.
   */
  private String fileHeader = "";
  /**
   * The name of the generated class.
   */
  private String className;
  /**
   * The name of the package in which the class is generated.
   */
  private String targetPackage;
  /**
   * Holds the generated code.
   */
  private StringBuilder code = new StringBuilder();
  /**
   * Holds the generated import statements.
   */
  private StringBuilder imports = new StringBuilder();
  /**
   * Indentation level for the generated code.
   */
  private int indentLevel = 0;

  /**
   * Generate the static proxy source code for the specified interface.
   * @param interfaceName the name of the MBean interface for which to generate the proxy code.
   * @param targetPackage the name of the package in which the class is generated.
   * @return the genenrated source code.
   * @throws Exception if any error occurs.
   */
  public String generateSource(final String interfaceName, final String targetPackage) throws Exception {
    return generateSource(Class.forName(interfaceName), targetPackage);
  }

  /**
   * Generate the static proxy source code for the specified interface.
   * @param inf the MBean interface for which to generate the proxy code.
   * @param targetPackage the name of the package in which the class is generated.
   * @return the geenrated source code.
   * @throws Exception if any error occurs.
   */
  public String generateSource(final Class<?> inf, final String targetPackage) throws Exception {
    this.inf = inf;
    this.targetPackage = targetPackage;
    code = new StringBuilder();
    imports = new StringBuilder();
    fileHeader = "";
    importSet.clear();

    className = this.inf.getSimpleName() + CLASS_NAME_SUFFIX;
    generateFileHeader();
    generateClassHeader();
    generateConstructor();
    Method[] methods = inf.getMethods();
    for (Method m: methods) {
      generateMethodHeader(m);
      if (ReflectionUtils.isGetter(m)) generateGetAttribute(m);
      else if (ReflectionUtils.isSetter(m)) generateSetAttribute(m);
      else generateInvokeMethod(m);
      generateMethodFooter(m);
    }
    generateClassFooter();
    generateImports();
    String pkg = "package " + targetPackage + ";\n\n";

    return new StringBuilder(fileHeader).append(pkg).append(imports).append(code).toString();
  }

  /**
   * Generate the file header, not including the imports.
   * @throws Exception if any error occurs.
   */
  private void generateFileHeader() throws Exception {
    InputStream is = getClass().getResourceAsStream("JavaSourceHeader.txt");
    fileHeader = FileUtils.readTextFile(new BufferedReader(new InputStreamReader(is)));
    int year = Calendar.getInstance().get(Calendar.YEAR);
    fileHeader = fileHeader.replace("@current_year@", Integer.toString(year));
  }

  /**
   * Generate the imports.
   * @throws Exception if any error occurs.
   */
  private void generateImports() throws Exception {
    for (String s: importSet) imports.append("import ").append(s).append(";\n");
    imports.append('\n');
  }

  /**
   * Generate the class header.
   * @throws Exception if any error occurs.
   */
  private void generateClassHeader() throws Exception {
    printIndent().println("/**");
    printIndent().print(  " * Generated static proxy for the {@link ").print(inf.getName()).println("} MBean interface.");
    printIndent().println(" */");
    importSet.add(AbstractMBeanStaticProxy.class.getName());
    importSet.add(JMXConnectionWrapper.class.getName());
    String pkg = inf.getPackage().getName();
    if (!pkg.equals(targetPackage)) importSet.add(inf.getName());
    print("public class ").print(className).print(" extends ").print(AbstractMBeanStaticProxy.class.getSimpleName());
    print(" implements ").print(inf.getSimpleName()).println(" {");
    indentLevel++;
  }

  /**
   * Generate the class footer.
   * @throws Exception if any error occurs.
   */
  private void generateClassFooter() throws Exception {
    indentLevel--;
    println("}");
  }

  /**
   * Generate the constructor.
   * @throws Exception if any error occurs.
   */
  private void generateConstructor() throws Exception {
    printIndent().println("/**");
    printIndent().println(" * Initialize this mbean static proxy.");
    printIndent().println(" * @param connection the JMX connection used to invoke remote MBean methods.");
    printIndent().println(" * @param mbeanName the object name of the mbean for which this object is a proxy.");
    printIndent().println(" */");
    printIndent().print("public ").print(className).println("(final JMXConnectionWrapper connection, final String mbeanName) {");
    indentLevel++;
    printIndent().println("super(connection, mbeanName);");
    indentLevel--;
    printIndent().println("}");
  }

  /**
   * Generate the declaration of an MBean interface method.
   * @param m the MBean interface method to generate the code for.
   * @throws Exception if any error occurs.
   */
  private void generateMethodHeader(final Method m) throws Exception {
    addImports(m);
    printNewLine().printIndent().println("@Override");
    printIndent().print("public ").print(m.getReturnType().getSimpleName()).print(" ").print(m.getName()).print("(");
    Class<?>[] types = m.getParameterTypes();
    if (types.length > 0) {
      for (int i=0; i<types.length; i++) {
        if (i > 0) print(", ");
        print("final ").print(types[i].getSimpleName()).print(" param" + i);
      }
    }
    println(") {");
    indentLevel++;
  }

  /**
   * Generate the footer for an MBean interface method.
   * @param m the MBean interface method to generate the code for.
   * @throws Exception if any error occurs.
   */
  private void generateMethodFooter(final Method m) throws Exception {
    indentLevel--;
    printIndent().println("}");
  }

  /**
   * Generate the code for an MBean method invocation.
   * @param m the MBean interface method to generate the code for.
   * @throws Exception if any error occurs.
   */
  private void generateInvokeMethod(final Method m) throws Exception {
    printIndent();
    if (m.getReturnType() != void.class) print("return (").print(m.getReturnType().getSimpleName()).print(") ");
    print("invoke(\"").print(m.getName()).print("\", ");
    Class<?>[] types = m.getParameterTypes();
    if (types.length > 0) {
      print("new Object[] { ");
      for (int i=0; i<types.length; i++) {
        if (i > 0) print(", ");
        print("param" + i);
      }
      print(" }, new String[] { ");
      for (int i=0; i<types.length; i++) {
        if (i > 0) print(", ");
        print("\"").print(types[i].getName()).print("\"");
      }
      print(" }");
    } else {
      print("(Object[]) null, (String[]) null");
    }
    println(");");
  }

  /**
   * Generate the code for getting an MBean attribute value.
   * @param m the MBean interface method to generate the code for.
   * @throws Exception if any error occurs.
   */
  private void generateGetAttribute(final Method m) throws Exception {
    printIndent().print("return (").print(m.getReturnType().getSimpleName()).print(") getAttribute(\"").print(ReflectionUtils.getMBeanAttributeName(m)).println("\");");
  }

  /**
   * Generate the code for setting an MBean attribute value.
   * @param m the MBean interface method to generate the code for.
   * @throws Exception if any error occurs.
   */
  private void generateSetAttribute(final Method m) throws Exception {
    printIndent().print("setAttribute(\"").print(ReflectionUtils.getMBeanAttributeName(m)).print("\", param0);");
  }

  /**
   * Add the imports required for the specified method.
   * @param m the MBean interface method to generate the imports for.
   * @throws Exception if any error occurs.
   */
  private void addImports(final Method m) throws Exception {
    addImportIfMissing(m.getReturnType());
    for (Class<?> c: m.getParameterTypes()) addImportIfMissing(c);
    //for (Class<?> c: m.getExceptionTypes()) addImportIfMissing(c);
  }

  /**
   * Add an import ofr the specified class if it wasn't already added.
   * @param c the class to add a import for.
   * @throws Exception if any error occurs.
   */
  private void addImportIfMissing(final Class<?> c) throws Exception {
    if (c.isArray()) addImportIfMissing(c.getComponentType());
    else {
      if (c.isPrimitive() || "java.lang".equals(c.getPackage().getName())) return;
      String name = c.getName();
      if (!importSet.contains(name)) importSet.add(name);
    }
  }

  /**
   * Print a new line.
   * @return this object.
   */
  private MBeanStaticProxyGenerator printNewLine() {
    return println("");
  }

  /**
   * Print an indentation at the current level.
   * @return this object.
   */
  private MBeanStaticProxyGenerator printIndent() {
    for (int i=0; i<indentLevel; i++) code.append(INDENT_STRING);
    return this;
  }

  /**
   * Print the specified string.
   * @param s the string to print.
   * @return this object.
   */
  private MBeanStaticProxyGenerator print(final String s) {
    code.append(s);
    return this;
  }

  /**
   * Print the specified string and append a new line.
   * @param s the string to print.
   * @return this object.
   */
  private MBeanStaticProxyGenerator println(final String s) {
    code.append(s).append('\n');
    return this;
  }

  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      MBeanStaticProxyGenerator gen = new MBeanStaticProxyGenerator();
      String s = gen.generateSource(DriverJobManagementMBean.class, "org.jppf.management.generated");
      System.out.println(s);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
