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

package org.jppf.admin.web.health;

/**
 *
 * @author Laurent Cohen
 */
public class AlertThresholds {
  /**
   * The warning and critical threshold value.
   */
  private double warning, critical;

  /**
   *
   * @param warning the warning level value.
   * @param critical the critical level value.
   */
  public AlertThresholds(final double warning, final double critical) {
    this.warning = warning;
    this.critical = critical;
  }

  /**
   * @return the warning threshold value.
   */
  public synchronized double getWarning() {
    return warning;
  }

  /**
   * Set the warning threshold value.
   * @param warning the value to set.
   */
  public synchronized void setWarning(final double warning) {
    this.warning = warning;
  }

  /**
   * @return the critical threshold value.
   */
  public synchronized double getCritical() {
    return critical;
  }

  /**
   * Set the critical threshold value.
   * @param critical the value to set.
   */
  public synchronized void setCritical(final double critical) {
    this.critical = critical;
  }
}
