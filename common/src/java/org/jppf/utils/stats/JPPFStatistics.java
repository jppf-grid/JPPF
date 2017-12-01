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
package org.jppf.utils.stats;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.utils.concurrent.JPPFThreadFactory;

/**
 * Instances of this class hold statistics snapshots.
 * To create and add a new snapshot, <code>createSnapshot(String)</code> should be called first,
 * before any call to one of the <code>addXXX()</code> methods, in order to respect thread safety.
 * @author Laurent Cohen
 */
public class JPPFStatistics implements Serializable, Iterable<JPPFSnapshot> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * A filter which accepts all snapshots.
   */
  public static final Filter NOOP_FILTER = new Filter() {
    @Override
    public boolean accept(final JPPFSnapshot snapshot) {
      return true;
    }
  };
  /**
   * Contains all snapshots currently handled.
   */
  private final ConcurrentHashMap<String, JPPFSnapshot> snapshots = new ConcurrentHashMap<>();
  /**
   * The list of listeners.
   */
  private transient List<ListenerInfo> listeners = new CopyOnWriteArrayList<>();
  /**
   * 
   */
  private final transient ExecutorService executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("StatsEventDispatcher"));

  /**
   * Default constructor.
   * @exclude
   */
  public JPPFStatistics() {
  }

  /**
   * Copy constructor.
   * @param map the statistics map to copy.
   * @exclude
   */
  private JPPFStatistics(final Map<String, JPPFSnapshot> map) {
    for (Map.Entry<String, JPPFSnapshot> entry: map.entrySet()) snapshots.put(entry.getKey(), entry.getValue().copy());
  }

  /**
   * Get a snapshot specified by its label.
   * @param label the label of the snapshot to look up.
   * @return a {@link JPPFSnapshot} instance, or <code>null</code> if none could be found with the specified label.
   */
  public JPPFSnapshot getSnapshot(final String label) {
    return snapshots.get(label);
  }

  /**
   * Create a snapshot with the specified label if it doesn't exist.
   * If a snapshot with this label already exists, it is returned.
   * @param label the label of the snapshot to create.
   * @return a {@link JPPFSnapshot} instance representing the newly created snapshot or the exsting one.
   * @exclude
   */
  public JPPFSnapshot createSnapshot(final String label) {
    return createSnapshot(false, label);
  }

  /**
   * Create a snapshot with the specified label if it doesn't exist.
   * If a snapshot with this label already exists, it is returned.
   * @param label the label of the snapshot to create.
   * @param cumulative determines whether updates are accumulated instead of simply stored as latest value.
   * @return a {@link JPPFSnapshot} instance representing the newly created snapshot or the exsting one.
   * @exclude
   */
  public JPPFSnapshot createSnapshot(final boolean cumulative, final String label) {
    JPPFSnapshot newSnapshot = cumulative ? new CumulativeSnapshot(label) : new NonCumulativeSnapshot(label);
    JPPFSnapshot oldSnapshot = snapshots.putIfAbsent(label, newSnapshot);
    JPPFSnapshot snapshot = oldSnapshot == null ? newSnapshot : oldSnapshot;
    if (!listeners.isEmpty()) fireEvent(snapshot, EventType.ADDED);
    return snapshot;
  }

  /**
   * Create a single value snapshot with the specified label if it doesn't exist.
   * If a snapshot with this label already exists, it is returned.
   * @param label the label of the snapshot to create.
   * @return a {@link JPPFSnapshot} instance representing the newly created snapshot or the exsting one.
   * @exclude
   */
  public JPPFSnapshot createSingleValueSnapshot(final String label) {
    JPPFSnapshot newSnapshot = new SingleValueSnapshot(label);
    JPPFSnapshot oldSnapshot = snapshots.putIfAbsent(label, newSnapshot);
    JPPFSnapshot snapshot = oldSnapshot == null ? newSnapshot : oldSnapshot;
    if (!listeners.isEmpty()) fireEvent(snapshot, EventType.ADDED);
    return snapshot;
  }

  /**
   * Create an array of snapshots with the specified labels, if it doesn't exist.
   * If one of the snapshots already exists, it is returned.
   * @param labels the label of the snapshot to create.
   * @return an array of {@link JPPFSnapshot} instances representing the newly created or exsting snapshots, in the same order as the input labels.
   * @exclude
   */
  public JPPFSnapshot[] createSnapshots(final String...labels) {
    return createSnapshots(false, labels);
  }

  /**
   * Create an array of snapshots with the specified labels, if they don't exist.
   * If any of the snapshots already exists, it is returned.
   * @param labels the label of the snapshot to create.
   * @param cumulative determines whether updates are accumulated instead of simply stored as latest value.
   * @return an array of {@link JPPFSnapshot} instances representing the newly created or exsting snapshots, in the same order as the input labels.
   * @exclude
   */
  public JPPFSnapshot[] createSnapshots(final boolean cumulative, final String...labels) {
    JPPFSnapshot[] snapshots = new JPPFSnapshot[labels.length];
    for (int i=0; i<labels.length; i++) snapshots[i] = createSnapshot(cumulative, labels[i]);
    return snapshots;
  }

  /**
   * Create an array of single value snapshots with the specified labels, if they don't exist.
   * If any of the snapshots already exists, it is returned.
   * @param labels the label of the snapshot to create.
   * @return an array of {@link JPPFSnapshot} instances representing the newly created or exsting snapshots, in the same order as the input labels.
   * @exclude
   */
  public JPPFSnapshot[] createSingleValueSnapshots(final String...labels) {
    JPPFSnapshot[] snapshots = new JPPFSnapshot[labels.length];
    for (int i=0; i<labels.length; i++) snapshots[i] = createSingleValueSnapshot(labels[i]);
    return snapshots;
  }

  /**
   * Remove the snapshot with the specified label.
   * If a snapshot with this label already exists, it is returned.
   * @param label the label of the snapshot to create.
   * @return a {@link JPPFSnapshot} instance representing the removed snapshot or <code>null</code> if the label is not in the map.
   * @exclude
   */
  public JPPFSnapshot removeSnapshot(final String label) {
    JPPFSnapshot snapshot = snapshots.remove(label);
    if (!listeners.isEmpty()) fireEvent(snapshot, EventType.REMOVED);
    return snapshot;
  }

  /**
   * Add the specified value to the snapshot with the specified label.
   * @param label the label of the snapshot to add the value to.
   * @param value the accumulated sum of the values to add.
   * @return a reference to the updated {@link JPPFSnapshot} object.
   * @throws IllegalStateException if the snapshot does not exist.
   * @exclude
   */
  public JPPFSnapshot addValue(final String label, final double value) {
    return addValues(label, value, 1L);
  }

  /**
   * Add the specified values to the snapshot with the specified label.
   * @param label the label of the snapshot to add the values to.
   * @param accumulatedValues the accumulated sum of the values to add.
   * @param count the number of values in the accumalated values.
   * @return a reference to the updated {@link JPPFSnapshot} object.
   * @throws IllegalStateException if the snapshot does not exist.
   * @exclude
   */
  public JPPFSnapshot addValues(final String label, final double accumulatedValues, final long count) {
    JPPFSnapshot snapshot = snapshots.get(label);
    if (snapshot == null) throw new IllegalStateException("snapshot '" + label + "' was either not created or removed!");
    snapshot.addValues(accumulatedValues, count);
    if (!listeners.isEmpty()) fireEvent(snapshot, EventType.UPDATED);
    return snapshot;
  }

  /**
   * Build a copy of this stats object.
   * @return a new <code>JPPFStats</code> instance, populated with the current values
   * of the fields in this stats object.
   * @exclude
   */
  public JPPFStatistics copy() {
    return new JPPFStatistics(snapshots);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("JPPF statistics:");
    for (Map.Entry<String, JPPFSnapshot> entry: snapshots.entrySet()) sb.append('\n').append(entry.getValue());
    return sb.toString();
  }

  /**
   * Reset all contained snapshots to their initial values.
   * @exclude
   */
  public void reset() {
    reset(null);
  }

  /**
   * Reset all contained snapshots to their initial values.
   * @param filter determines which snapshots will be reset.
   * @exclude
   */
  public void reset(final JPPFStatistics.Filter filter) {
    for (Map.Entry<String, JPPFSnapshot> entry: snapshots.entrySet()) {
      JPPFSnapshot snapshot = entry.getValue();
      if ((filter == null) || filter.accept(snapshot)) snapshot.reset();
    }
  }

  /**
   * Remove all snapshots from this object.
   * @exclude
   */
  public void clear() {
    snapshots.clear();
  }

  /**
   * Get all the snapshots in this object.
   * @return a collection of {@link JPPFSnapshot} instances.
   */
  public Collection<JPPFSnapshot> getSnapshots() {
    return new ArrayList<>(snapshots.values());
  }

  /**
   * Get the snapshots which satisfy the specified filter.
   * @param filter determines which snapshots will be part of the returned collection.
   * @return a collection of {@link JPPFSnapshot} instances, possibly empty but never null.
   */
  public Collection<JPPFSnapshot> getSnapshots(final Filter filter) {
    List<JPPFSnapshot> list = new ArrayList<>(snapshots.size());
    for (Map.Entry<String, JPPFSnapshot> entry: snapshots.entrySet()) {
      JPPFSnapshot snapshot = entry.getValue();
      if ((filter == null) || filter.accept(snapshot)) list.add(snapshot);
    }
    return list;
  }

  /**
   * Add a listener to the list of listeners.
   * This is equivalent to calling {@link #addListener(JPPFStatisticsListener, Filter) addListener(listener, null)}.
   * @param listener the listener to add.
   */
  public void addListener(final JPPFStatisticsListener listener) {
    addListener(listener, null);
  }

  /**
   * Add a filtered listener to the list of listeners.
   * @param listener the listener to add.
   * @param filter the filter to apply to the listener. If {@code null}, then no filter is applied.
   */
  public void addListener(final JPPFStatisticsListener listener, final Filter filter) {
    if (listener != null) listeners.add(new ListenerInfo(listener, filter));
  }

  /**
   * Remove a listener to the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeListener(final JPPFStatisticsListener listener) {
    if (listener != null) {
      ListenerInfo toDelete = null;
      for (ListenerInfo info: listeners) {
        if (listener.equals(info.listener) && (info.filter == NOOP_FILTER)) {
          toDelete = info;
          break;
        }
      }
      if (toDelete != null) listeners.remove(toDelete);
    }
  }

  /**
   * Remove a listener to the list of listeners.
   * @param listener the listener to remove.
   * @param filter the filter associated ith the listener to remove.
   */
  public void removeListener(final JPPFStatisticsListener listener, final Filter filter ) {
    if (listener != null) listeners.remove(new ListenerInfo(listener, filter));
  }

  /**
   * Notify all listeners that a snapshot was created.
   * @param snapshot the snapshot for which an event occurs.
   * @param type the type of event: created, removd, update.
   */
  private void fireEvent(final JPPFSnapshot snapshot, final EventType type) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        JPPFStatisticsEvent event = new JPPFStatisticsEvent(JPPFStatistics.this, snapshot);
        List<ListenerInfo> accepted = new ArrayList<>(listeners.size());
        for (ListenerInfo info: listeners) {
          if (info.filter.accept(snapshot)) accepted.add(info);
        }
        switch(type) {
          case ADDED:   for (ListenerInfo info: accepted) info.listener.snapshotAdded(event); break;
          case REMOVED: for (ListenerInfo info: accepted) info.listener.snapshotRemoved(event); break;
          case UPDATED: for (ListenerInfo info: accepted) info.listener.snapshotUpdated(event); break;
        }
      }
    });
  }

  @Override
  public Iterator<JPPFSnapshot> iterator() {
    return snapshots.values().iterator();
  }

  /**
   * A filter interface for snapshots.
   */
  public interface Filter {
    /**
     * Determines whether the specified snapshot is accepted by this filter.
     * @param snapshot the snapshot to check.
     * @return <code>true</code> if the snapshot is accepted, <code>false</code> otherwise.
     */
    boolean accept(JPPFSnapshot snapshot);
  }

  /**
   * The possible types of events.
   */
  static enum EventType {
    /**
     * A snapshot was added.
     */
    ADDED,
    /**
     * A snapshot was removed.
     */
    REMOVED,
    /**
     * A snapshot was updated.
     */
    UPDATED
  }

  /**
   * Association of a listener and filter.
   */
  private static class ListenerInfo {
    /**
     * The listener to filter.
     */
    public final JPPFStatisticsListener listener;
    /**
     * The filter to apply to the listener.
     */
    public final Filter filter;

    /**
     * Initialize this object.
     * @param listener the listener to filter
     * @param filter the filter to apply to the listener.
     */
    public ListenerInfo(final JPPFStatisticsListener listener, final Filter filter) {
      if (listener == null) throw new IllegalArgumentException("the listener can never be null");
      this.listener = listener;
      this.filter = (filter == null) ? NOOP_FILTER : filter;
    }

    @Override
    public int hashCode() {
      int result = 31 + filter.hashCode();
      result = 31 * result + listener.hashCode();
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ListenerInfo other = (ListenerInfo) obj;
      return listener.equals(other.listener) && filter.equals(other.filter);
    }
  }

  /**
   * Saves the state of this object to a stream.
   * @param oos the stream to write to.
   * @throws IOException if an I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
  }

  /**
   * Restore the state of this object from a stream.
   * @param ois the stream to read from.
   * @throws IOException if an I/O error occurs.
   * @throws ClassNotFoundException if a class cannot be found or initialized during deserialization.
   */
  private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
    listeners = new CopyOnWriteArrayList<>();
    ois.defaultReadObject();
  }
}
