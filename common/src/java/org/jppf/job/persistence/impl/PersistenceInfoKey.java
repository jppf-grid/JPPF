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

package org.jppf.job.persistence.impl;

import org.jppf.job.persistence.*;

/**
 * Instances of this class represent a key in the cache.
 * @exclude
 */
public class PersistenceInfoKey {
  /**
   * The job uuid.
   */
  final String uuid;
  /**
   * The type of persisted object.
   */
  final PersistenceObjectType type;
  /**
   * The position of the object in the job if applicable, otherwise -1.
   */
  final int position;

  /**
   * Intiialize this cache key.
   * @param uuid the job uuid.
   * @param type the type of persisted object.
   * @param position the position of the object in the job if applicable, otherwise {@code -1}.
   */
  public PersistenceInfoKey(final String uuid, final PersistenceObjectType type, final int position) {
    this.uuid = uuid;
    this.type = type;
    this.position = position;
  }

  /**
   * Intiialize this cache key.
   * @param info the info from which to initialize.
   */
  public PersistenceInfoKey(final PersistenceInfo info) {
    this(info.getJobUuid(), info.getType(), info.getTaskPosition());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + position;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PersistenceInfoKey other = (PersistenceInfoKey) obj;
    if (position != other.position) return false;
    if (type != other.type) return false;
    if (uuid == null) {
      if (other.uuid != null) return false;
    } else if (!uuid.equals(other.uuid)) return false;
    return true;
  }

  /**
   * @return the job uuid.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * @return the type of persisted object.
   */
  public PersistenceObjectType getType() {
    return type;
  }

  /**
   * @return the position of the object in the job if applicable, otherwise -1.
   */
  public int getPosition() {
    return position;
  }
}