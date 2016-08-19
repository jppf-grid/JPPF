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

package test.org.jppf.node.policy;

import static org.junit.Assert.*;

import org.jppf.client.JPPFJob;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.junit.Test;

import test.org.jppf.test.setup.common.*;

/**
 * Test for functions commons to all execution policies.
 * @author Laurent Cohen
 */
public class TestExecutionPolicy {
  /**
   * Test that the context for an execution policy is properly set for all the elements in the policy graph.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testMatches() throws Exception {
    JPPFSystemInformation info = new JPPFSystemInformation(JPPFUuid.normalUUID(), false, false);
    info.populate();
    info.getRuntime().setString("ipv4.addresses", "localhost|192.168.1.14");
    TestCustomPolicy tcp = new TestCustomPolicy();
    ExecutionPolicy policy = new Contains("jppf.uuid", true, "AB").and(tcp);
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, 1, LifeCycleTask.class, 0L);
    JPPFStatistics stats = JPPFStatisticsHelper.createServerStatistics();
    policy.setContext(job.getSLA(), job.getClientSLA(), job.getMetadata(), 2, stats);
    PolicyContext ctx = tcp.getContext();
    assertNotNull(ctx);
    assertEquals(job.getSLA(), ctx.getSLA());
    assertEquals(job.getClientSLA(), ctx.getClientSLA());
    assertEquals(job.getMetadata(), ctx.getMetadata());
    assertEquals(2, ctx.getJobDispatches());
    assertEquals(stats, ctx.getStats());
  }

  /**
   * 
   */
  public static class TestCustomPolicy extends CustomPolicy {
    @Override
    public boolean accepts(final PropertiesCollection info) {
      return false;
    }
  }
}
