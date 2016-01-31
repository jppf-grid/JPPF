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

package test.org.jppf.utils.stats;

import static org.junit.Assert.*;
import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.*;

import org.jppf.utils.stats.*;
import org.jppf.utils.stats.JPPFStatistics.Filter;
import org.junit.Test;

/**
 * Unit tests for {@link JPPFStatisticsListener} and {@link JPPFFilteredStatisticsListener}.
 * @author Laurent Cohen
 */
public class TestJPPFStatisticsListener {
  /**
   * Labels of stats snapshots used in the tests.
   */
  private final static String[] LABELS = { TASK_QUEUE_COUNT, JOB_COUNT, CLIENTS, NODES };
  /**
   * Number of times each snapshot is updated.
   */
  private static final int UPDATES_PER_SNAPSHOT = 5;
  /**
   * Used to randomly select a snapshot to filter out.
   */
  private final Random rand = new Random(System.nanoTime());

  /**
   * Test that the stats listner receives all expected events.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testSimpleListener() throws Exception {
    MyStatsListener listener = new MyStatsListener(null);
    JPPFStatistics stats = createAndPopulateStats(listener);
    int expectedCount = LABELS.length;
    checkCounts(listener, expectedCount);
    for (String label: LABELS) assertTrue(listener.labels.contains(label));
  }

  /**
   * Test that the stats listener only receives events according to its associated filter.
   * The filter excludes a single randomly chosen snapshot.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testFilteredListener() throws Exception {
    int expectedCount = LABELS.length - 1;
    int excludedIndex = rand.nextInt(LABELS.length);
    final String excludedLabel = LABELS[excludedIndex];
    System.out.printf("testFilteredListener() excludedIndex=%d; excludedLabel=%s%n", excludedIndex, excludedLabel);
    JPPFStatistics.Filter filter = new Filter() {
      @Override
      public boolean accept(final JPPFSnapshot snapshot) {
        return !excludedLabel.equals(snapshot.getLabel());
      }
    };
    MyStatsListener listener = new MyStatsListener(filter);
    JPPFStatistics stats = createAndPopulateStats(listener);
    checkCounts(listener, expectedCount);
    for (int i=0; i<expectedCount; i++) {
      if (i != excludedIndex) assertTrue(listener.labels.contains(LABELS[i]));
      else assertFalse(listener.labels.contains(LABELS[i]));
    }
    assertFalse(listener.labels.contains(excludedLabel));
  }

  /**
   * 
   * @param listener the listner to check.
   * @param expectedCount the expected count of snapshots for which events were received.
   * @throws Exception if any error occurs.
   */
  private void checkCounts(final MyStatsListener listener, final int expectedCount) throws Exception {
    assertEquals(expectedCount, listener.createCount);
    assertEquals(UPDATES_PER_SNAPSHOT * expectedCount, listener.updateCount);
    assertEquals(expectedCount, listener.removeCount);
    assertEquals(expectedCount, listener.labels.size());
    assertEquals(1, listener.getFilterCount);
  }

  /**
   * A {@link JPPFFilteredStatisticsListener} which counts the number of cretae, delete and update toits snapshots.
   */
  public static class MyStatsListener extends JPPFFilteredStatisticsListener {
    /**
     * Numbers of creation, deletion and update events
     */
    public int createCount=0, removeCount=0, updateCount=0, getFilterCount=0;
    /**
     * Holds the labels of the snapshots for which an event occured.
     */
    public final Set<String> labels = new HashSet<>();
    /**
     * An optional statistics filter for this listener.
     */
    private final JPPFStatistics.Filter filter;

    /**
     * Initialize this listener.
     */
    public MyStatsListener() {
      this(null);
    }

    /**
     * Initialize this listener with the specified filter.
     * @param filter a filter associated with this listener, may be {@code null}.
     */
    public MyStatsListener(final JPPFStatistics.Filter filter) {
      this.filter = filter;
    }

    @Override
    public void snapshotAdded(final JPPFStatisticsEvent event) {
      createCount++;
      labels.add(event.getSnapshot().getLabel());
    }

    @Override
    public void snapshotRemoved(final JPPFStatisticsEvent event) {
      removeCount++;
      labels.add(event.getSnapshot().getLabel());
    }

    @Override
    public void snapshotUpdated(final JPPFStatisticsEvent event) {
      updateCount++;
      labels.add(event.getSnapshot().getLabel());
    }

    @Override
    public Filter getFilter() {
      getFilterCount++;
      return filter;
    }
  }

  /**
   * Initialize a {@link JPPFStatistics} object and populate it wvia updates to its snapshots.
   * @param listener a listner to add to the statistic object.
   * @return a populated {@link JPPFStatistics}.
   * @throws Exception if any error occurs.
   */
  private JPPFStatistics createAndPopulateStats(final MyStatsListener listener) throws Exception {
    JPPFStatistics stats = new JPPFStatistics();
    JPPFStatistics.Filter filter = listener.getFilter();
    stats.addListener(listener, filter);
    for (int i=0; i<LABELS.length; i++) stats.createSnapshot(LABELS[i]);
    for (int i=0; i<UPDATES_PER_SNAPSHOT*LABELS.length; i++) stats.addValue(LABELS[i % LABELS.length], i + 1);
    for (int i=0; i<LABELS.length; i++) stats.removeSnapshot(LABELS[i]);
    return stats;
  }
}
