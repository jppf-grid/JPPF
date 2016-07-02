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

package test.org.jppf.job;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.job.*;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.common.*;

/**
 *
 * @author Laurent Cohen
 */
public class TestJobSelector {
  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testAllJobSelector() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createJobs(6, prefix);
    JobSelector selector = new AllJobsSelector();
    checkSerialization(selector);
    List<JPPFJob> filtered = filter(jobs, selector);
    assertNotNull(filtered);
    assertEquals(jobs, filtered);
  }

  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testJobUuidSelector() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createJobs(6, prefix);
    // select jobs with an even number
    List<String> uuids = new ArrayList<>();
    for (int i=1; i<jobs.size(); i+=2) uuids.add(jobs.get(i).getUuid());
    JobSelector selector = new JobUuidSelector(uuids);
    checkSerialization(selector);
    List<JPPFJob> filtered = filter(jobs, selector);
    assertNotNull(filtered);
    assertEquals(3, filtered.size());
    for (int i=1; i<=filtered.size(); i+=2) {
      assertTrue(filtered.contains(jobs.get(i)));
    }
  }

  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testScriptedJobSelector() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createJobs(6, prefix);
    // select jobs with an even number
    StringBuilder script = new StringBuilder()
      .append("var test = jppfJob.getMetadata().getParameter('test', -1);\n")
      .append("test % 2 == 0;\n");
    ScriptedJobSelector selector = new ScriptedJobSelector("javascript", script.toString());
    ScriptedJobSelector selector2 = (ScriptedJobSelector) checkSerialization(selector);
    assertEquals(selector.getLanguage(), selector2.getLanguage());
    assertEquals(selector.getScript(), selector2.getScript());
    List<JPPFJob> filtered = filter(jobs, selector);
    assertNotNull(filtered);
    assertEquals(3, filtered.size());
    for (int i=1; i<=filtered.size(); i+=2) {
      assertTrue(filtered.contains(jobs.get(i)));
    }
  }

  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testScriptedJobSelector2() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createJobs(6, prefix);
    // select jobs with an even number
    JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    checkSerialization(selector);
    List<JPPFJob> filtered = filter(jobs, selector);
    assertNotNull(filtered);
    assertEquals(jobs, filtered);
  }

  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testCustomJobSelector() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createJobs(6, prefix);
    // select jobs with an even number
    JobSelector selector = new MyJobSelector();
    checkSerialization(selector);
    List<JPPFJob> filtered = filter(jobs, selector);
    assertNotNull(filtered);
    assertEquals(3, filtered.size());
    for (int i=1; i<=filtered.size(); i+=2) {
      assertTrue(filtered.contains(jobs.get(i)));
    }
  }

  /**
   * Filter the specified list of jobs according to the specified selector.
   * @param jobs the jobs to filter.
   * @param selector the selector to apply.
   * @return a list of {@link JPPFJob} instances.
   */
  private List<JPPFJob> filter(final List<JPPFJob> jobs, final JobSelector selector) {
    List<JPPFJob> list = new ArrayList<>(jobs.size());
    for (JPPFJob job: jobs) {
      if (selector.accepts(job)) list.add(job);
    }
    return list;
  }

  /**
   * Create the specified number of jobs.
   * @param nbJobs the number of jobs to create.
   * @param namePrefix the prefix of the job names.
   * @return a list of {@link JPPFJob} instances.
   * @throws Exception if any error occurs.
   */
  private List<JPPFJob> createJobs(final int nbJobs, final String namePrefix) throws Exception {
    List<JPPFJob> jobs = new ArrayList<>();
    for (int i=1; i<= nbJobs; i++) {
      JPPFJob job = BaseTestHelper.createJob(namePrefix + '-' + i, false, false, 1, LifeCycleTask.class, 0L);
      job.getMetadata().setParameter("test", i);
      jobs.add(job);
    }
    return jobs;
  }

  /**
   * Check that the specified job selector can be serialized and deserialized.
   * @param selector the selector to check.
   * @return a deep copy of the input selector via serialization.
   * @throws Exception if any error occurs.
   */
  private JobSelector checkSerialization(final JobSelector selector) throws Exception {
    ObjectSerializer ser = new ObjectSerializerImpl();
    JPPFBuffer buf = ser.serialize(selector);
    JobSelector selector2 = (JobSelector) ser.deserialize(buf);
    return selector2;
  }

  /** */
  public static class MyJobSelector implements JobSelector {
    @Override
    public boolean accepts(final JPPFDistributedJob job) {
      int n = job.getMetadata().getParameter("test", -1);
      return n % 2 == 0;
    }
  }
}
