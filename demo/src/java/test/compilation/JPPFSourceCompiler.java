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

import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.compilation.*;
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
      String className = "test.compilation.MyTask";
      // build the map of sources to compile
      Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
      sources.put(className, TestCompilation.buildSourceCode());
      sources.put(className + "2", TestCompilation.buildSourceCode2());
      // receive the map of generated classes bytecode
      SourceCompiler sc = new SourceCompiler(CompilationOutputKind.MEMORY);
      Map<String, byte[]> bytecodeMap = sc.compileToMemory(sources);
      output("bytecode map = " + bytecodeMap);
      byte[] bytecode = bytecodeMap.get(className);
      output("got bytecode = " + (bytecode == null ? "null" : "[length=" + bytecode.length + "]"));
      // create a custom class loader so we can load this class
      ClassLoader cl = new CustomClassLoader(bytecodeMap, JPPFSourceCompiler.class.getClassLoader());
      Class<?> clazz = Class.forName(className, true, cl);
      Callable task = (Callable) clazz.newInstance();
      executeJob(task);
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
  private static void executeJob(final Callable task) throws Exception
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
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  public static void output(final String message)
  {
    System.out.println(message);
    log.info(message);
  }
}
