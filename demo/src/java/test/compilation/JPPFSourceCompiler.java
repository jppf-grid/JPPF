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

import static test.compilation.TestCompilation.*;

import java.util.*;

import javax.tools.Diagnostic;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.compilation.SourceCompiler;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFSourceCompiler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFSourceCompiler.class);

  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    try
    {
      //String className = "test.compilation.MyTask";
      String taskClassName = null;
      // an array of the sources - the first element is the one executed as a task
      //CharSequence[] srcArray = { buildTaskSource() };
      CharSequence[] srcArray = { buildSourceCode(), buildSourceCode2() };
      // build the map of sources to compile
      Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
      for (CharSequence seq: srcArray)
      {
        String name = classNameFromSource(seq);
        output("adding source for " + name);
        if (taskClassName == null) taskClassName = name;
        sources.put(name, seq);
      }
      SourceCompiler compiler = null;
      try
      {
        compiler = new SourceCompiler();
        // receive the map of generated classes bytecode
        Map<String, byte[]> bytecodeMap = compiler.compileToMemory(sources);
        output("bytecode map = " + bytecodeMap);
        byte[] bytecode = bytecodeMap.get(taskClassName);
        output("got bytecode = " + (bytecode == null ? "null" : "[length=" + bytecode.length + "]"));
        if (bytecode == null)
        {
          List<Diagnostic> diags = compiler.getDiagnostics();
          output("diagnostics: ");
          for (Diagnostic d: diags) output("  " + d);
        }
        else
        {
          // load the class
          Class<?> clazz = compiler.getClassloader().loadClass(taskClassName);
          Object task = (Object) clazz.newInstance();
          executeJob(task);
        }
      }
      finally
      {
        compiler.close();
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Execute a job containing the specified task.
   * @param task the task to execute.
   * @throws Exception if any error occurs.
   */
  private static void executeJob(final Object task) throws Exception
  {
    JPPFClient client = new JPPFClient();
    try
    {
      JPPFJob job = new JPPFJob();
      job.setName("compiled class job");
      job.addTask(task);
      List<JPPFTask> results = client.submit(job);
      JPPFTask result = results.get(0);
      if (result.getException() != null) output("got exception: " + ExceptionUtils.getStackTrace(result.getException()));
      else output("got result: " + result.getResult());
    }
    finally
    {
      client.close();
    }
  }

  /**
   * Generate the source code of a class.
   * @return the source code to compile as a string.
   */
  public static CharSequence buildTaskSource() {
    StringBuilder sb = new StringBuilder();
    append(sb, "package test.compilation;                      ");
    append(sb, "                                               ");
    append(sb, "import org.jppf.server.protocol.JPPFTask;      ");
    append(sb, "                                               ");
    append(sb, "public class MyJPPFTask extends JPPFTask {     ");
    append(sb, "                                               ");
    append(sb, "  @Override                                    ");
    append(sb, "  public void run() {                          ");
    append(sb, "    String msg =                               ");
    append(sb, "      \"Hello, world of compilation! from \" + ");
    append(sb, "      getClass().getSimpleName();              ");
    append(sb, "    System.out.println(msg);                   ");
    append(sb, "    setResult(msg);                            ");
    append(sb, "  }                                            ");
    append(sb, "}                                              ");
    return sb;
  }
}
