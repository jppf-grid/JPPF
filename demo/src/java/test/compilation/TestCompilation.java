/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.compilation;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.*;

import org.jppf.utils.compilation.SourceCompiler;

/**
 * 
 * @author Laurent Cohen
 */
public class TestCompilation
{
  /**
   * A string containing a list of whitespace characters.
   */
  public static final String WHITE_SPACE = " \t\n\r";
  /**
   * Pattern used to find the package name from the source code.
   */
  public static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+(.*);");
  /**
   * Pattern used to find the class name from the source code.
   */
  public static final Pattern CLASS_PATTERN = Pattern.compile("public\\s+class\\s+(\\S+)\\s+");

  /**
   * Entry point ofr this program.
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    try
    {
      testToFile();
      testToMemory();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Test compilation to file system from a string java source.
   * @throws Exception if any error occurs.
   */
  public static void testToFile() throws Exception
  {
    output("****************************************");
    File classesDir = new File("tmpclasses/");
    if (!classesDir.exists()) {
      // create the classes dir
      if (!classesDir.mkdirs()) throw new IOException("could not create the classes directory '" + classesDir + "'");
    }
    String className = "test.compilation.MyTask";
    Map<String, CharSequence> sources = new HashMap<>();
    sources.put(className, buildSourceCode());
    sources.put(className + "2", buildSourceCode2());
    SourceCompiler compiler = null;
    try
    {
      compiler = new SourceCompiler();
      compiler.compileToFile(sources, classesDir);
      Class<?> clazz = compiler.getClassloader().loadClass(className);
      Callable task = (Callable) clazz.newInstance();
      output("result: " + task.call());
    }
    finally
    {
      compiler.close();
    }
  }

  /**
   * Test compilation to memory from a string java source.
   * @throws Exception if any error occurs.
   */
  public static void testToMemory() throws Exception
  {
    output("****************************************");
    String className = "test.compilation.MyTask";
    // build the map of sources to compile
    Map<String, CharSequence> sources = new HashMap<>();
    sources.put(className, buildSourceCode());
    sources.put(className + "2", buildSourceCode2());
    SourceCompiler compiler = null;
    try
    {
      compiler = new SourceCompiler();
      // receive the map of generated classes bytecode
      Map<String, byte[]> bytecodeMap = compiler.compileToMemory(sources);
      output("bytecode map = " + bytecodeMap);
      // load the class
      Class<?> clazz = compiler.getClassloader().loadClass(className);
      // create an instance and execute it
      Callable task = (Callable) clazz.newInstance();
      output("result: " + task.call());
    }
    finally
    {
      compiler.close();
    }
  }

  /**
   * Generate the source code of a class.
   * @return the source code to compile as a string.
   */
  public static CharSequence buildSourceCode2()
  {
    StringBuilder sb = new StringBuilder();
    append(sb, "package test.compilation;                              ");
    append(sb, "                                                       ");
    append(sb, "import java.io.Serializable;                           ");
    append(sb, "import java.util.concurrent.Callable;                  ");
    append(sb, "                                                       ");
    append(sb, "public class MyTask2                                   ");
    append(sb, "  implements Callable<Object>, Serializable {          ");
    append(sb, "                                                       ");
    append(sb, "  public Object call() {                               ");
    append(sb, "    String msg =                                       ");
    append(sb, "      \"Hello, world of compilation! (from MyTask2)\"; ");
    append(sb, "    System.out.println(msg);                           ");
    append(sb, "    return msg;                                        ");
    append(sb, "  }                                                    ");
    append(sb, "}                                                      ");
    return sb;
  }

  /**
   * Generate the source code of a more complex class.
   * @return the source code to compile as a string.
   */
  public static CharSequence buildSourceCode()
  {
    StringBuilder sb = new StringBuilder();
    append(sb, "package test.compilation;                              ");
    append(sb, "                                                       ");
    append(sb, "import java.io.Serializable;                           ");
    append(sb, "import java.util.concurrent.Callable;                  ");
    append(sb, "                                                       ");
    append(sb, "public class MyTask                                    ");
    append(sb, "  implements Callable<Object>, Serializable {          ");
    append(sb, "                                                       ");
    append(sb, "  public Object call() {                               ");
    append(sb, "                                                       ");
    append(sb, "    // make this class execute its own code            ");
    append(sb, "    String msg = \"Hello, world of compilation!\";     ");
    append(sb, "    System.out.println(msg);                           ");
    append(sb, "                                                       ");
    append(sb, "    // execute the code of an instance inner class     ");
    append(sb, "    new InstanceInnerClass().execute();                ");
    append(sb, "                                                       ");
    append(sb, "    // execute the code of a static inner class        ");
    append(sb, "    new StaticInnerClass().execute();                  ");
    append(sb, "                                                       ");
    append(sb, "    // execute the code of an anonymous inner class    ");
    append(sb, "    new StaticInnerClass() {                           ");
    append(sb, "      @Override                                        ");
    append(sb, "      public void execute() {                          ");
    append(sb, "        System.out.println(\"printing from \" +        ");
    append(sb, "          getClass().getSimpleName());                 ");
    append(sb, "      }                                                ");
    append(sb, "    }.execute();                                       ");
    append(sb, "                                                       ");
    append(sb, "    // execute the code of another class altogether    ");
    append(sb, "    // (compiled as a separate compilation unit)       ");
    append(sb, "    new MyTask2().call();                              ");
    append(sb, "                                                       ");
    append(sb, "    return msg;                                        ");
    append(sb, "  }                                                    ");
    append(sb, "                                                       ");
    append(sb, "  public class InstanceInnerClass                      ");
    append(sb, "    implements Serializable {                          ");
    append(sb, "    public void execute() {                            ");
    append(sb, "        System.out.println(\"printing from \" +        ");
    append(sb, "          getClass().getSimpleName());                 ");
    append(sb, "    }                                                  ");
    append(sb, "  }                                                    ");
    append(sb, "                                                       ");
    append(sb, "  public static class StaticInnerClass                 ");
    append(sb, "    implements Serializable {                          ");
    append(sb, "    public void execute() {                            ");
    append(sb, "        System.out.println(\"printing from \" +        ");
    append(sb, "          getClass().getSimpleName());                 ");
    append(sb, "    }                                                  ");
    append(sb, "  }                                                    ");
    append(sb, "}                                                      ");
    return sb;
  }

  /**
   * Convenience method to make the buildSourceCodeXXX() methods more readable (well, we hope).
   * @param sb a string builder to append to.
   * @param s the string to append.
   */
  static void append(final StringBuilder sb, final String s)
  {
    sb.append(trimRight(s)).append('\n');
  }

  /**
   * Trim the specified char sequence to the right only.
   * @param seq the char sequence to trim.
   * @return a new char sequence without spaces on the right.
   */
  static CharSequence trimRight(final CharSequence seq)
  {
    int pos = seq.length() - 1;
    while ((pos >=0) && (WHITE_SPACE.indexOf(seq.charAt(pos)) >= 0)) pos--;
    return new StringBuilder().append(seq, 0, pos+1);
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  static void output(final String message)
  {
    System.out.println(message);
  }

  /**
   * Attempt to determine the qualified name of a class from its source. 
   * @param source the source code to parse.
   * @return the fully qualified name of the top-level class found in the source. 
   */
  public static String classNameFromSource(final CharSequence source)
  {
    Matcher matcher = PACKAGE_PATTERN.matcher(source);
    String pkg = matcher.find() ? matcher.group(1).trim() + '.' : "";
    matcher = CLASS_PATTERN.matcher(source);
    if (!matcher.find()) return null;
    String name = matcher.group(1).trim();
    return pkg + name;
  }
}
