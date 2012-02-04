/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
import java.net.*;
import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.utils.compilation.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestCompilation
{
  /**
   * Entry point ofr this program.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      testToFile();
      testToMemory();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Test compilation to file system from a string java source.
   * @throws Exception if any error occurs.
   */
  public static void testToFile() throws Exception {
    output("****************************************");
    File classesDir = new File("tmpclasses/");
    if (!classesDir.exists()) {
      // create the classes dir
      if (!classesDir.mkdirs()) throw new IOException("could not create the class directory '" + classesDir + "'");
    }
    String className = "test.compilation.MyTask";
    Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
    sources.put(className, buildSourceCode());
    sources.put(className + "2", buildSourceCode2());
    SourceCompiler sc = new SourceCompiler(CompilationOutputKind.FILE_SYSTEM);
    sc.compileToFile(sources, classesDir);
    sc.close();
    URLClassLoader cl = new URLClassLoader(new URL[] { classesDir.toURI().toURL() }, TestCompilation.class.getClassLoader());
    Class<?> clazz = Class.forName(className, true, cl);
    Callable task = (Callable) clazz.newInstance();
    output("result: " + task.call());
  }

  /**
   * Test compilation to memory from a string java source.
   * @throws Exception if any error occurs.
   */
  public static void testToMemory() throws Exception {
    output("****************************************");
    String className = "test.compilation.MyTask";
    // build the map of sources to compile
    Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
    sources.put(className, buildSourceCode());
    sources.put(className + "2", buildSourceCode2());
    SourceCompiler sc = new SourceCompiler(CompilationOutputKind.MEMORY);
    // receive the map of generated classes bytecode
    Map<String, byte[]> bytecodeMap = sc.compileToMemory(sources);
    sc.close();
    output("bytecode map = " + bytecodeMap);
    // create a custom class loader so we can load this class
    ClassLoader cl = new CustomClassLoader(bytecodeMap, TestCompilation.class.getClassLoader());
    // load the class
    Class<?> clazz = Class.forName(className, true, cl);
    // create an instance and execute it
    Callable task = (Callable) clazz.newInstance();
    output("result: " + task.call());
  }

  /**
   * Generate the source code of a class.
   * @return the source code to compile as a string.
   */
  public static CharSequence buildSourceCode2() {
    StringBuilder sb = new StringBuilder();
    append(sb, "package test.compilation;                              ");
    append(sb, "                                                       ");
    append(sb, "import java.util.concurrent.Callable;                  ");
    append(sb, "                                                       ");
    append(sb, "public class MyTask2                                   ");
    append(sb, "  implements Callable<Object>, java.io.Serializable {  ");
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
  public static CharSequence buildSourceCode() {
    StringBuilder sb = new StringBuilder();
    append(sb, "package test.compilation;                              ");
    append(sb, "                                                       ");
    append(sb, "import java.util.concurrent.Callable;                  ");
    append(sb, "                                                       ");
    append(sb, "public class MyTask                                    ");
    append(sb, "  implements Callable<Object>, java.io.Serializable {  ");
    append(sb, "  public Object call() {                               ");
    append(sb, "    // make this class execute its own code            ");
    append(sb, "    String msg = \"Hello, world of compilation!\";     ");
    append(sb, "    System.out.println(msg);                           ");
    append(sb, "    // execute the code of an instance inner class     ");
    append(sb, "    new InstanceInnerClass().execute();                ");
    append(sb, "    // execute the code of a static inner class        ");
    append(sb, "    new StaticInnerClass().execute();                  ");
    append(sb, "    // execute the code of an anonymous inner class    ");
    append(sb, "    new Runnable() {                                   ");
    append(sb, "      @Override                                        ");
    append(sb, "      public void run() {                              ");
    append(sb, "        System.out.println(                            ");
    append(sb, "          \"printing from **anonymous** inner class\");");
    append(sb, "      }                                                ");
    append(sb, "    }.run();                                           ");
    append(sb, "    // execute the code of another class altogether    ");
    append(sb, "    // (compiled as a separate compilation unit)       ");
    append(sb, "    new MyTask2().call();                              ");
    append(sb, "                                                       ");
    append(sb, "    return msg;                                        ");
    append(sb, "  }                                                    ");
    append(sb, "                                                       ");
    append(sb, "  public class InstanceInnerClass                      ");
    append(sb, "    implements java.io.Serializable {                  ");
    append(sb, "    public void execute() {                            ");
    append(sb, "      System.out.println(                              ");
    append(sb, "        \"printing from **instance** inner class\");   ");
    append(sb, "    }                                                  ");
    append(sb, "  }                                                    ");
    append(sb, "                                                       ");
    append(sb, "  public static class StaticInnerClass                 ");
    append(sb, "    implements java.io.Serializable {                  ");
    append(sb, "    public void execute() {                            ");
    append(sb, "      System.out.println(                              ");
    append(sb, "        \"printing from **static** inner class\");     ");
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
  private static void append(final StringBuilder sb, final String s)
  {
    sb.append(trimRight(s)).append('\n');
  }

  /**
   * Trim the specified char sequence to the right only.
   * @param seq the char sequence to trim.
   * @return a new char sequence without spaces on the right.
   */
  private static CharSequence trimRight(final CharSequence seq)
  {
    int pos = seq.length() - 1;
    while ((pos >=0) && (seq.charAt(pos) == ' ')) pos--;
    //return pos > 0 ? new StringBuilder().append(seq, 0, pos+1) : "";
    return new StringBuilder().append(seq, 0, pos+1);
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  public static void output(final String message) {
    System.out.println(message);
  }
}
