/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
import java.util.regex.Pattern;

import org.jppf.client.JPPFJob;
import org.jppf.job.*;
import org.jppf.node.protocol.*;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.BaseTest;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 *
 * @author Laurent Cohen
 */
public class TestJobSelector extends BaseTest {
  /** */
  private static List<JPPFJob> jobs;
  /** */
  private static final long TEST_TIMEOUT = 5000L;
  /** */
  private static final String CLASS_NAME = TestJobSelector.class.getSimpleName();

  /** @throws Exception if any error occurs */
  @BeforeClass
  public static void setupClass() throws Exception {
    jobs = createJobs();
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testAllJobSelector() throws Exception {
    checkSelector(new AllJobsSelector(), 0, 1, 2, 3, 4, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testUuidSelector() throws Exception {
    final String[] uuids = new String[3];
    for (int i=0; i<=2; i++) uuids[i] = jobs.get(1 + 2 * i).getUuid();
    checkSelector(new JobUuidSelector(uuids), 1, 3, 5);
    checkSelector(new JobUuidSelector(Arrays.asList(uuids)), 1, 3, 5);
    final String[] uuids2 = { jobs.get(2).getUuid(), "some uuid"};
    checkSelector(new JobUuidSelector(uuids2), 2);
    checkSelector(new JobUuidSelector(Arrays.asList(uuids2)), 2);
    final String[] badUuids = { "UUID-1", "hello-0", "wrong_uuid"};
    checkSelector(new JobUuidSelector(badUuids));
    checkSelector(new JobUuidSelector(Arrays.asList(badUuids)));
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testNameSelector() throws Exception {
    final String[] names = new String[3];
    for (int i=0; i<=2; i++) names[i] = jobs.get(1 + 2 * i).getName();
    checkSelector(new JobNameSelector(names), 1, 3, 5);
    checkSelector(new JobNameSelector(Arrays.asList(names)), 1, 3, 5);
    final String[] names2 = { jobs.get(2).getName(), "some name"};
    checkSelector(new JobNameSelector(names2), 2);
    checkSelector(new JobNameSelector(Arrays.asList(names2)), 2);
    final String[] badNames = { "NAME-1", "hello-0", "wrong_name"};
    checkSelector(new JobNameSelector(badNames));
    checkSelector(new JobNameSelector(Arrays.asList(badNames)));
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testScriptedSelector() throws Exception {
    final String script = new StringBuilder("var test = jppfJob.getMetadata().getParameter('test', -1);\n").append("test % 2 == 0;\n").toString();
    final ScriptedJobSelector selector = new ScriptedJobSelector("javascript", script);
    final ScriptedJobSelector selector2 = (ScriptedJobSelector) checkSerialization(selector);
    assertEquals(selector.getLanguage(), selector2.getLanguage());
    assertEquals(selector.getScript(), selector2.getScript());
    checkSelector(selector, 0, 2, 4);
    final String script2 = new StringBuilder("var test = jppfJob.getMetadata().getParameter('test', -1);\n").append("test < 0;\n").toString();
    final ScriptedJobSelector selector3 = new ScriptedJobSelector("javascript", script2);
    checkSelector(selector3);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testScriptedSelector2() throws Exception {
    final JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + CLASS_NAME + "')");
    checkSelector(selector, 0, 1, 2, 3, 4, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testCustomSelector() throws Exception {
    checkSelector(new MyJobSelector(), 1, 3, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testAndSelector() throws Exception {
    checkSelector(new EqualsJobSelector("test", 1).and(new MoreThanJobSelector("test", 2)));
    checkSelector(new LessThanJobSelector("test", 5).and(new MoreThanJobSelector("test", 2)), 3, 4);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testOrSelector() throws Exception {
    checkSelector(new LessThanJobSelector("test", 0).or(new MoreThanJobSelector("test", 10)));
    checkSelector(new LessThanJobSelector("test", 2).or(new MoreThanJobSelector("test", 4)), 0, 1, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testXorSelector() throws Exception {
    checkSelector(new LessThanJobSelector("test", 6).xor(new MoreThanJobSelector("test", -1)));
    checkSelector(new LessThanJobSelector("test", 2).xor(new MoreThanJobSelector("test", 10)), 0, 1);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testNegateSelector() throws Exception {
    checkSelector(new LessThanJobSelector("test", 6).negate());
    checkSelector(new LessThanJobSelector("test", 2).negate(), 2, 3, 4, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testLessThanSelector() throws Exception {
    checkSelector(new LessThanJobSelector("test", 0));
    checkSelector(new LessThanJobSelector("test", 2), 0, 1);
    checkSelector(new LessThanJobSelector("test.string", "string-A"));
    checkSelector(new LessThanJobSelector("test.string", "string-C"), 0, 1);
    checkSelector(new LessThanJobSelector(CLASS_NAME + "-0"));
    checkSelector(new LessThanJobSelector(CLASS_NAME + "-2"), 0, 1);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testAtMostSelector() throws Exception {
    checkSelector(new AtMostJobSelector("test", -1));
    checkSelector(new AtMostJobSelector("test", 0), 0);
    checkSelector(new AtMostJobSelector("test", 2), 0, 1, 2);
    checkSelector(new AtMostJobSelector("test.string", "string-@"));
    checkSelector(new AtMostJobSelector("test.string", "string-A"), 0);
    checkSelector(new AtMostJobSelector("test.string", "string-C"), 0, 1, 2);
    checkSelector(new AtMostJobSelector(CLASS_NAME + "-/"));
    checkSelector(new AtMostJobSelector(CLASS_NAME + "-0"), 0);
    checkSelector(new AtMostJobSelector(CLASS_NAME + "-2"), 0, 1, 2);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testMoreThanSelector() throws Exception {
    checkSelector(new MoreThanJobSelector("test", 5));
    checkSelector(new MoreThanJobSelector("test", 3), 4, 5);
    checkSelector(new MoreThanJobSelector("test.string", "string-F"));
    checkSelector(new MoreThanJobSelector("test.string", "string-D"), 4, 5);
    checkSelector(new MoreThanJobSelector(CLASS_NAME + "-5"));
    checkSelector(new MoreThanJobSelector(CLASS_NAME + "-3"), 4, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testAtLeastSelector() throws Exception {
    checkSelector(new AtLeastJobSelector("test", 6));
    checkSelector(new AtLeastJobSelector("test", 5), 5);
    checkSelector(new AtLeastJobSelector("test", 3), 3, 4, 5);
    checkSelector(new AtLeastJobSelector("test.string", "string-G"));
    checkSelector(new AtLeastJobSelector("test.string", "string-F"), 5);
    checkSelector(new AtLeastJobSelector("test.string", "string-D"), 3, 4, 5);
    checkSelector(new AtLeastJobSelector(CLASS_NAME + "-6"));
    checkSelector(new AtLeastJobSelector(CLASS_NAME + "-5"), 5);
    checkSelector(new AtLeastJobSelector(CLASS_NAME + "-3"), 3, 4, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testBetweenEESelector() throws Exception {
    checkSelector(new BetweenEEJobSelector("test", -3, -1));
    checkSelector(new BetweenEEJobSelector("test", 6, 8));
    checkSelector(new BetweenEEJobSelector("test", 4, 4));
    checkSelector(new BetweenEEJobSelector("test", 1, 4), 2, 3);
    checkSelector(new BetweenEEJobSelector("test", -1, 4), 0, 1, 2, 3);
    checkSelector(new BetweenEEJobSelector("test", 4, 10), 5);
    checkSelector(new BetweenEEJobSelector("test.string", "string->", "string-@"));
    checkSelector(new BetweenEEJobSelector("test.string", "string-G", "string-I"));
    checkSelector(new BetweenEEJobSelector("test.string", "string-E", "string-E"));
    checkSelector(new BetweenEEJobSelector("test.string", "string-B", "string-E"), 2, 3);
    checkSelector(new BetweenEEJobSelector("test.string", "string-@", "string-E"), 0, 1, 2, 3);
    checkSelector(new BetweenEEJobSelector("test.string", "string-E", "string-K"), 5);
    checkSelector(new BetweenEEJobSelector(CLASS_NAME + "--", CLASS_NAME + "-/"));
    checkSelector(new BetweenEEJobSelector(CLASS_NAME + "-6", CLASS_NAME + "-8"));
    checkSelector(new BetweenEEJobSelector(CLASS_NAME + "-4", CLASS_NAME + "-4"));
    checkSelector(new BetweenEEJobSelector(CLASS_NAME + "-1", CLASS_NAME + "-4"), 2, 3);
    checkSelector(new BetweenEEJobSelector(CLASS_NAME + "-/", CLASS_NAME + "-4"), 0, 1, 2, 3);
    checkSelector(new BetweenEEJobSelector(CLASS_NAME + "-4", CLASS_NAME + "-:"), 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testBetweenIISelector() throws Exception {
    checkSelector(new BetweenIIJobSelector("test", -3, -1));
    checkSelector(new BetweenIIJobSelector("test", 6, 8));
    checkSelector(new BetweenIIJobSelector("test", -3, 0), 0);
    checkSelector(new BetweenIIJobSelector("test", 4, 4), 4);
    checkSelector(new BetweenIIJobSelector("test", 1, 4), 1, 2, 3, 4);
    checkSelector(new BetweenIIJobSelector("test", -1, 4), 0, 1, 2, 3, 4);
    checkSelector(new BetweenIIJobSelector("test", 4, 10), 4, 5);
    checkSelector(new BetweenIIJobSelector("test.string", "string->", "string-@"));
    checkSelector(new BetweenIIJobSelector("test.string", "string-G", "string-I"));
    checkSelector(new BetweenIIJobSelector("test.string", "string->", "string-A"), 0);
    checkSelector(new BetweenIIJobSelector("test.string", "string-E", "string-E"), 4);
    checkSelector(new BetweenIIJobSelector("test.string", "string-B", "string-E"), 1, 2, 3, 4);
    checkSelector(new BetweenIIJobSelector("test.string", "string-@", "string-E"), 0, 1, 2, 3, 4);
    checkSelector(new BetweenIIJobSelector("test.string", "string-E", "string-K"), 4, 5);
    checkSelector(new BetweenIIJobSelector(CLASS_NAME + "--", CLASS_NAME + "-/"));
    checkSelector(new BetweenIIJobSelector(CLASS_NAME + "-6", CLASS_NAME + "-8"));
    checkSelector(new BetweenIIJobSelector(CLASS_NAME + "--", CLASS_NAME + "-0"), 0);
    checkSelector(new BetweenIIJobSelector(CLASS_NAME + "-4", CLASS_NAME + "-4"), 4);
    checkSelector(new BetweenIIJobSelector(CLASS_NAME + "-1", CLASS_NAME + "-4"), 1, 2, 3, 4);
    checkSelector(new BetweenIIJobSelector(CLASS_NAME + "-/", CLASS_NAME + "-4"), 0, 1, 2, 3, 4);
    checkSelector(new BetweenIIJobSelector(CLASS_NAME + "-4", CLASS_NAME + "-:"), 4, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testBetweenEISelector() throws Exception {
    checkSelector(new BetweenEIJobSelector("test", -3, -1));
    checkSelector(new BetweenEIJobSelector("test", 6, 8));
    checkSelector(new BetweenEIJobSelector("test", -3, 0), 0);
    checkSelector(new BetweenEIJobSelector("test", 4, 4));
    checkSelector(new BetweenEIJobSelector("test", 1, 4), 2, 3, 4);
    checkSelector(new BetweenEIJobSelector("test", -1, 4), 0, 1, 2, 3, 4);
    checkSelector(new BetweenEIJobSelector("test", 4, 10), 5);
    checkSelector(new BetweenEIJobSelector("test.string", "string->", "string-@"));
    checkSelector(new BetweenEIJobSelector("test.string", "string-G", "string-I"));
    checkSelector(new BetweenEIJobSelector("test.string", "string->", "string-A"), 0);
    checkSelector(new BetweenEIJobSelector("test.string", "string-E", "string-E"));
    checkSelector(new BetweenEIJobSelector("test.string", "string-B", "string-E"), 2, 3, 4);
    checkSelector(new BetweenEIJobSelector("test.string", "string-@", "string-E"), 0, 1, 2, 3, 4);
    checkSelector(new BetweenEIJobSelector("test.string", "string-E", "string-K"), 5);
    checkSelector(new BetweenEIJobSelector(CLASS_NAME + "--", CLASS_NAME + "-/"));
    checkSelector(new BetweenEIJobSelector(CLASS_NAME + "-6", CLASS_NAME + "-8"));
    checkSelector(new BetweenEIJobSelector(CLASS_NAME + "--", CLASS_NAME + "-0"), 0);
    checkSelector(new BetweenEIJobSelector(CLASS_NAME + "-4", CLASS_NAME + "-4"));
    checkSelector(new BetweenEIJobSelector(CLASS_NAME + "-1", CLASS_NAME + "-4"), 2, 3, 4);
    checkSelector(new BetweenEIJobSelector(CLASS_NAME + "-/", CLASS_NAME + "-4"), 0, 1, 2, 3, 4);
    checkSelector(new BetweenEIJobSelector(CLASS_NAME + "-4", CLASS_NAME + "-:"), 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testBetweenIESelector() throws Exception {
    checkSelector(new BetweenIEJobSelector("test", -3, -1));
    checkSelector(new BetweenIEJobSelector("test", 6, 8));
    checkSelector(new BetweenIEJobSelector("test", -3, 0));
    checkSelector(new BetweenIEJobSelector("test", 4, 4));
    checkSelector(new BetweenIEJobSelector("test", 1, 4), 1, 2, 3);
    checkSelector(new BetweenIEJobSelector("test", -1, 4), 0, 1, 2, 3);
    checkSelector(new BetweenIEJobSelector("test", 4, 10), 4, 5);
    checkSelector(new BetweenIEJobSelector("test.string", "string->", "string-@"));
    checkSelector(new BetweenIEJobSelector("test.string", "string-G", "string-I"));
    checkSelector(new BetweenIEJobSelector("test.string", "string->", "string-A"));
    checkSelector(new BetweenIEJobSelector("test.string", "string-E", "string-E"));
    checkSelector(new BetweenIEJobSelector("test.string", "string-B", "string-E"), 1, 2, 3);
    checkSelector(new BetweenIEJobSelector("test.string", "string-@", "string-E"), 0, 1, 2, 3);
    checkSelector(new BetweenIEJobSelector("test.string", "string-E", "string-K"), 4, 5);
    checkSelector(new BetweenIEJobSelector(CLASS_NAME + "--", CLASS_NAME + "-/"));
    checkSelector(new BetweenIEJobSelector(CLASS_NAME + "-6", CLASS_NAME + "-8"));
    checkSelector(new BetweenIEJobSelector(CLASS_NAME + "--", CLASS_NAME + "-0"));
    checkSelector(new BetweenIEJobSelector(CLASS_NAME + "-4", CLASS_NAME + "-4"));
    checkSelector(new BetweenIEJobSelector(CLASS_NAME + "-1", CLASS_NAME + "-4"), 1, 2, 3);
    checkSelector(new BetweenIEJobSelector(CLASS_NAME + "-/", CLASS_NAME + "-4"), 0, 1, 2, 3);
    checkSelector(new BetweenIEJobSelector(CLASS_NAME + "-4", CLASS_NAME + "-:"), 4, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testEqualsSelector() throws Exception {
    checkSelector(new EqualsJobSelector("test", 6));
    checkSelector(new EqualsJobSelector("test", 2), 2);
    checkSelector(new EqualsJobSelector("test.string", "string-G"));
    checkSelector(new EqualsJobSelector("test.string", "string-C"), 2);
    checkSelector(new EqualsJobSelector(CLASS_NAME + "-6"));
    checkSelector(new EqualsJobSelector(CLASS_NAME + "-2"), 2);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testNotEqualsSelector() throws Exception {
    checkSelector(new NotEqualsJobSelector("test", 6), 0, 1, 2, 3, 4, 5);
    checkSelector(new NotEqualsJobSelector("test", 2), 0, 1, 3, 4, 5);
    checkSelector(new NotEqualsJobSelector("test.string", "string-G"), 0, 1, 2, 3, 4, 5);
    checkSelector(new NotEqualsJobSelector("test.string", "string-C"), 0, 1, 3, 4, 5);
    checkSelector(new NotEqualsJobSelector(CLASS_NAME + "-6"), 0, 1, 2, 3, 4, 5);
    checkSelector(new NotEqualsJobSelector(CLASS_NAME + "-2"), 0, 1, 3, 4, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testContainsSelector() throws Exception {
    checkSelector(new ContainsJobSelector("test.string", "no one has this"));
    checkSelector(new ContainsJobSelector("test.string", "-C"), 2);
    checkSelector(new ContainsJobSelector("test.string", "string-"), 0, 1, 2, 3, 4, 5);
    checkSelector(new ContainsJobSelector("--"));
    checkSelector(new ContainsJobSelector("-2"), 2);
    checkSelector(new ContainsJobSelector(CLASS_NAME), 0, 1, 2, 3, 4, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testIsOneOfSelector() throws Exception {
    checkSelector(new IsOneOfJobSelector("test", -1, 7, 10));
    checkSelector(new IsOneOfJobSelector("test", -1, 2, 5, 7, 10), 2, 5);
    checkSelector(new IsOneOfJobSelector("test.string", Arrays.asList("string-*", "string-G", "string-L")));
    checkSelector(new IsOneOfJobSelector("test.string", Arrays.asList("string-*", "string-C", "string-F", "string-L")), 2, 5);
    checkSelector(new IsOneOfJobSelector(CLASS_NAME + "-/", CLASS_NAME + "-7", CLASS_NAME + "-:"));
    checkSelector(new IsOneOfJobSelector(CLASS_NAME + "-/", CLASS_NAME + "-2", CLASS_NAME + "-5", CLASS_NAME + "-7", CLASS_NAME + "-:"), 2, 5);
  }

  /** @throws Exception if any error occurs */
  @Test(timeout = TEST_TIMEOUT)
  public void testRegexSelector() throws Exception {
    checkSelector(new RegexJobSelector("test.string", ".*hello.*"));
    checkSelector(new RegexJobSelector("test.string", "string-B.*"), 1);
    checkSelector(new RegexJobSelector("test.string", ".*ring-.*"), 0, 1, 2, 3, 4, 5);
    checkSelector(new RegexJobSelector("test.string", "STRING-B.*", Pattern.LITERAL));
    checkSelector(new RegexJobSelector("test.string", "STRING-B.*", Pattern.CASE_INSENSITIVE), 1);
    checkSelector(new RegexJobSelector(".*hello.*"));
    checkSelector(new RegexJobSelector(CLASS_NAME + "-1.*"), 1);
    checkSelector(new RegexJobSelector(CLASS_NAME + ".*"), 0, 1, 2, 3, 4, 5);
    checkSelector(new RegexJobSelector(CLASS_NAME.toUpperCase() + "-1.*", Pattern.LITERAL));
    checkSelector(new RegexJobSelector(CLASS_NAME.toUpperCase() + "-1.*", Pattern.CASE_INSENSITIVE), 1);
  }

  /**
   * Filter the specified list of jobs according to the specified selector.
   * @param jobs the jobs to filter.
   * @param selector the selector to apply.
   * @return a list of {@link JPPFJob} instances.
   */
  private static List<JPPFJob> filter(final List<JPPFJob> jobs, final JobSelector selector) {
    final List<JPPFJob> filtered = new ArrayList<>(jobs.size());
    for (final JPPFJob job: jobs) {
      if (selector.accepts(job)) filtered.add(job);
    }
    return filtered;
  }

  /**
   * Create the specified number of jobs.
   * @return a list of {@link JPPFJob} instances.
   * @throws Exception if any error occurs.
   */
  private static List<JPPFJob> createJobs() throws Exception {
    final int nbJobs = 6;
    final String namePrefix = CLASS_NAME;
    final List<JPPFJob> jobs = new ArrayList<>();
    for (int i=0; i< nbJobs; i++) {
      final JPPFJob job = BaseTestHelper.createJob(namePrefix + '-' + i, false, 0, null);
      final JobMetadata metadata = job.getMetadata();
      metadata.setParameter("test", i);
      metadata.setParameter("test.string", "string-" + (char) ('A' + i));
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
  private static JobSelector checkSerialization(final JobSelector selector) throws Exception {
    final ObjectSerializer ser = new ObjectSerializerImpl();
    final JPPFBuffer buf = ser.serialize(selector);
    final JobSelector selector2 = (JobSelector) ser.deserialize(buf);
    return selector2;
  }

  /**
   * Check that the specified job selector can be serialized and deserialized.
   * @param selector the job selector to test.
   * @param expectedJobIndexes the indexes of the expected jobs i the filtered view of the jobs.
   * @throws Exception if any error occurs.
   */
  private static void checkSelector(final JobSelector selector, final int...expectedJobIndexes) throws Exception {
    checkSerialization(selector);
    final List<JPPFJob> filtered = filter(jobs, selector);
    final StringBuilder sb = new StringBuilder("filtered jobs:");
    for (final JPPFJob job: filtered) sb.append('\n').append(job);
    print(false, false, sb.toString());
    assertEquals(expectedJobIndexes.length, filtered.size());
    int pos = 0;
    for (final JPPFJob job: filtered) {
      final int index = expectedJobIndexes[pos];
      final JPPFJob job2 = jobs.get(index);
      assertTrue(String.format("pos = %d, index = %d", pos, index), job == job2);
      pos++;
    }
  }

  /**
   * A custom job selector.
   */
  public static class MyJobSelector implements JobSelector {
    /** */
    private static final long serialVersionUID = 1L;

    @Override
    public boolean accepts(final JPPFDistributedJob job) {
      final int n = job.getMetadata().getParameter("test", -1);
      return n % 2 != 0;
    }
  }
}
