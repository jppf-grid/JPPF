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

package test.compilation;

import static test.compilation.TestCompilation.*;

import java.util.*;

import javax.tools.*;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.compilation.SourceCompiler;

/**
 * Build a JPPTask class from its source stored in a string,
 * then execute it in a JPPF grid.
 * @author Laurent Cohen
 */
public class JPPFSourceCompiler {
  /**
   * Entry point for this demo.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      //String className = "test.compilation.MyTask";
      String taskClassName = null;
      // an array of the sources - the first element is the one executed as a task
      //CharSequence[] srcArray = { buildTaskSource() };
      final CharSequence[] srcArray = { buildSourceCode(), buildSourceCode2() };
      // build the map of sources to compile
      final Map<String, CharSequence> sources = new HashMap<>();
      for (final CharSequence seq : srcArray) {
        final String name = classNameFromSource(seq);
        output("adding source for " + name);
        if (taskClassName == null) taskClassName = name;
        sources.put(name, seq);
      }
      SourceCompiler compiler = null;
      try {
        compiler = new SourceCompiler();
        // receive the map of generated classes bytecode
        final Map<String, byte[]> bytecodeMap = compiler.compileToMemory(sources);
        output("bytecode map = " + bytecodeMap);
        final byte[] bytecode = bytecodeMap.get(taskClassName);
        output("got bytecode = " + (bytecode == null ? "null" : "[length=" + bytecode.length + "]"));
        if (bytecode == null) {
          final List<Diagnostic<JavaFileObject>> diags = compiler.getDiagnostics();
          output("diagnostics: ");
          for (Diagnostic<JavaFileObject> d : diags) output("  " + d);
        } else {
          // load the class
          final Class<?> clazz = compiler.getClassloader().loadClass(taskClassName);
          final Object task = clazz.newInstance();
          executeJob(task);
        }
      } finally {
        compiler.close();
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Execute a job containing the specified task.
   * @param task the task to execute.
   * @throws Exception if any error occurs.
   */
  private static void executeJob(final Object task) throws Exception {
    final JPPFClient client = new JPPFClient();
    try {
      final JPPFJob job = new JPPFJob();
      job.setName("compiled class job");
      job.add(task);
      final List<Task<?>> results = client.submitJob(job);
      final Task<?> result = results.get(0);
      if (result.getThrowable() != null) output("got exception: " + ExceptionUtils.getStackTrace(result.getThrowable()));
      else output("got result: " + result.getResult());
    } finally {
      client.close();
    }
  }
}
