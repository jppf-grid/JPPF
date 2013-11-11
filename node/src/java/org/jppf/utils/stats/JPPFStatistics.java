/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package org.jppf.utils.stats;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Instances of this class hold statistics snapshots.
 * To create and add a new snapshot, <code>createSnapshot(String)</code> should be called first,
 * before any call to one of the <code>addXXX()</code> methods, in order to respect thread safety.
 * @author Laurent Cohen
 */
public class JPPFStatistics implements Serializable, Iterable<JPPFSnapshot>
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Contains all snapshots currently handled.
   */
  private final ConcurrentHashMap<String, JPPFSnapshot> snapshots = new ConcurrentHashMap<>();
  /**
   * The list of liteners.
   */
  private List<JPPFStatisticsListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Default constructor.
   */
  public JPPFStatistics()
  {
  }

  /**
   * Copy constructor.
   * @param map the statistics map to copy.
   */
  private JPPFStatistics(final Map<String, JPPFSnapshot> map)
  {
    for (Map.Entry<String, JPPFSnapshot> entry: map.entrySet())
      snapshots.put(entry.getKey(), entry.getValue().copy());
  }

  /**
   * Get a snapshot specified by its label.
   * @param label the label of the snapshot to look up.
   * @return a {@link JPPFSnapshot} instance, or <code>null</code> if none could be found with the specified label.
   */
  public JPPFSnapshot getSnapshot(final String label)
  {
    return snapshots.get(label);
  }

  /**
   * Create a snapshot with the specified label if it doesn't exist.
   * If a snapshot with this label already exists, it is returned.
   * @param label the label of the snapshot to create.
   * @return a {@link JPPFSnapshot} instance representing the newly created snapshot or the exsting one.
   */
  public JPPFSnapshot createSnapshot(final String label)
  {
    return createSnapshot(false, label);
  }

  /**
   * Create a snapshot with the specified label if it doesn't exist.
   * If a snapshot with this label already exists, it is returned.
   * @param label the label of the snapshot to create.
   * @param cumulative determines whether updates are accumulated instead of simply stored as latest value.
   * @return a {@link JPPFSnapshot} instance representing the newly created snapshot or the exsting one.
   */
  public JPPFSnapshot createSnapshot(final boolean cumulative, final String label)
  {
    JPPFSnapshot newSnapshot = cumulative ? new CumulativeSnapshot(label) : new NonCumulativeSnapshot(label);
    JPPFSnapshot oldSnapshot = snapshots.putIfAbsent(label, newSnapshot);
    JPPFSnapshot snapshot = oldSnapshot == null ? newSnapshot : oldSnapshot;
    //fireSnapshotAdded(snapshot.copy());
    return snapshot;
  }

  /**
   * Create a single value snapshot with the specified label if it doesn't exist.
   * If a snapshot with this label already exists, it is returned.
   * @param label the label of the snapshot to create.
   * @return a {@link JPPFSnapshot} instance representing the newly created snapshot or the exsting one.
   */
  public JPPFSnapshot createSingleValueSnapshot(final String label)
  {
    JPPFSnapshot newSnapshot = new SingleValueSnapshot(label);
    JPPFSnapshot oldSnapshot = snapshots.putIfAbsent(label, newSnapshot);
    JPPFSnapshot snapshot = oldSnapshot == null ? newSnapshot : oldSnapshot;
    //fireSnapshotAdded(snapshot);
    return snapshot;
  }

  /**
   * Create an array of snapshots with the specified labels, if it doesn't exist.
   * If one of the snapshots already exists, it is returned.
   * @param labels the label of the snapshot to create.
   * @return an array of {@link JPPFSnapshot} instances representing the newly created or exsting snapshots, in the smaez order as the input labels.
   */
  public JPPFSnapshot[] createSnapshots(final String...labels)
  {
    return createSnapshots(false, labels);
  }

  /**
   * Create an array of snapshots with the specified labels, if they don't exist.
   * If any of the snapshots already exists, it is returned.
   * @param labels the label of the snapshot to create.
   * @param cumulative determines whether updates are accumulated instead of simply stored as latest value.
   * @return an array of {@link JPPFSnapshot} instances representing the newly created or exsting snapshots, in the smaez order as the input labels.
   */
  public JPPFSnapshot[] createSnapshots(final boolean cumulative, final String...labels)
  {
    JPPFSnapshot[] snapshots = new JPPFSnapshot[labels.length];
    for (int i=0; i<labels.length; i++) snapshots[i] = createSnapshot(cumulative, labels[i]);
    return snapshots;
  }

  /**
   * Create an array of single value snapshots with the specified labels, if they don't exist.
   * If any of the snapshots already exists, it is returned.
   * @param labels the label of the snapshot to create.
   * @return an array of {@link JPPFSnapshot} instances representing the newly created or exsting snapshots, in the smaez order as the input labels.
   */
  public JPPFSnapshot[] createSingleValueSnapshots(final String...labels)
  {
    JPPFSnapshot[] snapshots = new JPPFSnapshot[labels.length];
    for (int i=0; i<labels.length; i++) snapshots[i] = createSingleValueSnapshot(labels[i]);
    return snapshots;
  }

  /**
   * Remove the snapshot with the specified label.
   * If a snapshot with this label already exists, it is returned.
   * @param label the label of the snapshot to create.
   * @return a {@link JPPFSnapshot} instance representing the removed snapshot or <code>null</code> if the label is not in the map.
   */
  public JPPFSnapshot removeSnapshot(final String label)
  {
    JPPFSnapshot snapshot = snapshots.remove(label);
    //fireSnapshotRemoved(snapshot.copy());
    return snapshot;
  }

  /**
   * Add the specified value to the snapshot with the specified label.
   * @param label the label of the snapshot to add the value to.
   * @param value the accumulated sum of the values to add.
   * @return a reference to the updated {@link JPPFSnapshot} object.
   * @throws IllegalStateException if the snapshot does not exist.
   */
  public JPPFSnapshot addValue(final String label, final double value)
  {
    return addValues(label, value, 1L);
  }

  /**
   * Add the specified values to the snapshot with the specified label.
   * @param label the label of the snapshot to add the values to.
   * @param accumulatedValues the accumulated sum of the values to add.
   * @param count the number of values in the accumalated values.
   * @return a reference to the updated {@link JPPFSnapshot} object.
   * @throws IllegalStateException if the snapshot does not exist.
   */
  public JPPFSnapshot addValues(final String label, final double accumulatedValues, final long count)
  {
    JPPFSnapshot snapshot = snapshots.get(label);
    if (snapshot == null) throw new IllegalStateException("snapshot '" + label + "' was either not created or removed!");
    snapshot.addValues(accumulatedValues, count);
    //fireSnapshotUpdated(snapshot.copy());
    return snapshot;
  }

  /**
   * Build a copy of this stats object.
   * @return a new <code>JPPFStats</code> instance, populated with the current values
   * of the fields in this stats object.
   */
  public JPPFStatistics copy()
  {
    return new JPPFStatistics(snapshots);
  }

  /**
   * Get a string representation of this stats object.
   * @return a string display the various stats values.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("JPPF statistics:\n");
    for (Map.Entry<String, JPPFSnapshot> entry: snapshots.entrySet()) sb.append(entry.getValue()).append('\n');
    return sb.toString();
  }

  /**
   * Reset all contained snapshots to their initial values.
   */
  public void reset()
  {
    reset(null);
  }

  /**
   * Reset all contained snapshots to their initial values.
   * @param filter determines which snapshots will be reset.
   */
  public void reset(final JPPFSnapshot.Filter filter)
  {
    for (Map.Entry<String, JPPFSnapshot> entry: snapshots.entrySet())
    {
      JPPFSnapshot snapshot = entry.getValue();
      if ((filter == null) || filter.accept(snapshot)) snapshot.reset();
    }
  }

  /**
   * Remove all snapshots from this object.
   */
  public void clear()
  {
    snapshots.clear();
  }

  /**
   * Get all the snapshots in this object.
   * @return a collection of {@link JPPFSnapshot} instances.
   */
  public Collection<JPPFSnapshot> getSnapshots()
  {
    return snapshots.values();
  }

  /**
   * Get the snapshots in this object using the specified filter.
   * @param filter determines which snapshots will be part of the returned collection.
   * @return a collection of {@link JPPFSnapshot} instances, possibly empty but never null.
   */
  public Collection<JPPFSnapshot> getSnapshots(final JPPFSnapshot.Filter filter)
  {
    List<JPPFSnapshot> list = new ArrayList<>(snapshots.size());
    for (Map.Entry<String, JPPFSnapshot> entry: snapshots.entrySet())
    {
      JPPFSnapshot snapshot = entry.getValue();
      if ((filter == null) || filter.accept(snapshot)) list.add(snapshot);
    }
    return list;
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addListener(final JPPFStatisticsListener listener)
  {
    if (listener != null) listeners.add(listener);
  }

  /**
   * Remove a listener to the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeListener(final JPPFStatisticsListener listener)
  {
    if (listener != null) listeners.remove(listener);
  }

  /**
   * Notify all listeners that a snapshot was created.
   * @param snapshot a copy of the created snapshot.
   */
  private void fireSnapshotAdded(final JPPFSnapshot snapshot)
  {
    JPPFStatisticsEvent event = new JPPFStatisticsEvent(snapshot);
    for (JPPFStatisticsListener listener: listeners) listener.snapshotAdded(event);
  }

  /**
   * Notify all listeners that a snapshot was updated.
   * @param snapshot a copy of the updated snapshot.
   */
  private void fireSnapshotUpdated(final JPPFSnapshot snapshot)
  {
    JPPFStatisticsEvent event = new JPPFStatisticsEvent(snapshot);
    for (JPPFStatisticsListener listener: listeners) listener.snapshotUpdated(event);
  }

  /**
   * Notify all listeners that a snapshot was removed.
   * @param snapshot a copy of the removed snapshot.
   */
  private void fireSnapshotRemoved(final JPPFSnapshot snapshot)
  {
    JPPFStatisticsEvent event = new JPPFStatisticsEvent(snapshot);
    for (JPPFStatisticsListener listener: listeners) listener.snapshotRemoved(event);
  }

  @Override
  public Iterator<JPPFSnapshot> iterator()
  {
    return snapshots.values().iterator();
  }
}
