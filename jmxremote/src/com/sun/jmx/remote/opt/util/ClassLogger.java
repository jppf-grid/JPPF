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
/*
 * @(#)ClassLogger.java	1.3
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jmx.remote.opt.util;

import java.util.logging.*;

/**
 *
 */
public class ClassLogger {
  /** */
  private final String className;
  /** */
  private final Logger logger;

  /**
   *
   * @param subsystem .
   * @param className .
   */
  public ClassLogger(final String subsystem, final String className) {
    logger = Logger.getLogger(subsystem);
    this.className = className;
  }

  /**
   *
   * @return .
   */
  public final boolean traceOn() {
    return finerOn();
  }

  /**
   *
   * @return .
   */
  public final boolean debugOn() {
    return finestOn();
  }

  /**
   *
   * @return .
   */
  public final boolean warningOn() {
    return logger.isLoggable(Level.WARNING);
  }

  /**
   *
   * @return .
   */
  public final boolean infoOn() {
    return logger.isLoggable(Level.INFO);
  }

  /**
   *
   * @return .
   */
  public final boolean configOn() {
    return logger.isLoggable(Level.CONFIG);
  }

  /**
   *
   * @return .
   */
  public final boolean fineOn() {
    return logger.isLoggable(Level.FINE);
  }

  /**
   *
   * @return .
   */
  public final boolean finerOn() {
    return logger.isLoggable(Level.FINER);
  }

  /**
   *
   * @return .
   */
  public final boolean finestOn() {
    return logger.isLoggable(Level.FINEST);
  }

  /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void debug(final String func, final String msg, final Object...params) {
   finest(func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void debug(final String func, final Throwable t, final Object...params) {
   finest(func, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void debug(final String func, final String msg, final Throwable t, final Object...params) {
   finest(func, msg, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void trace(final String func, final String msg, final Object...params) {
   finer(func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void trace(final String func, final Throwable t, final Object...params) {
   finer(func, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void trace(final String func, final String msg, final Throwable t, final Object...params) {
   finer(func, msg, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void error(final String func, final String msg, final Object...params) {
   severe(func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void error(final String func, final Throwable t, final Object...params) {
   severe(func, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void error(final String func, final String msg, final Throwable t, final Object...params) {
   severe(func, msg, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void finest(final String func, final String msg, final Object...params) {
   logp(Level.FINEST, func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void finest(final String func, final Throwable t, final Object...params) {
   logp(Level.FINEST, func, t.toString(), t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void finest(final String func, final String msg, final Throwable t, final Object...params) {
   logp(Level.FINEST, func, msg, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void finer(final String func, final String msg, final Object...params) {
   logp(Level.FINER, func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void finer(final String func, final Throwable t, final Object...params) {
   logp(Level.FINER, func, t.toString(), t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void finer(final String func, final String msg, final Throwable t, final Object...params) {
   logp(Level.FINER, func, msg, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void fine(final String func, final String msg, final Object...params) {
   logp(Level.FINE, func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void fine(final String func, final Throwable t, final Object...params) {
   logp(Level.FINE, func, t.toString(), t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void fine(final String func, final String msg, final Throwable t, final Object...params) {
   logp(Level.FINE, func, msg, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void config(final String func, final String msg, final Object...params) {
   logp(Level.CONFIG, func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void config(final String func, final Throwable t, final Object...params) {
   logp(Level.CONFIG, func, t.toString(), t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void config(final String func, final String msg, final Throwable t, final Object...params) {
   logp(Level.CONFIG, func, msg, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void info(final String func, final String msg, final Object...params) {
   logp(Level.INFO, func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void info(final String func, final Throwable t, final Object...params) {
   logp(Level.INFO, func, t.toString(), t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void info(final String func, final String msg, final Throwable t, final Object...params) {
   logp(Level.INFO, func, msg, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void warning(final String func, final String msg, final Object...params) {
   logp(Level.WARNING, func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void warning(final String func, final Throwable t, final Object...params) {
   logp(Level.WARNING, func, t.toString(), t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void warning(final String func, final String msg, final Throwable t, final Object...params) {
   logp(Level.WARNING, func, msg, t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param params the message parameters.
  */
 public final void severe(final String func, final String msg, final Object...params) {
   logp(Level.SEVERE, func, msg, params);
 }

 /**
  *
  * @param func .
  * @param t .
  * @param params the message parameters.
  */
 public final void severe(final String func, final Throwable t, final Object...params) {
   logp(Level.SEVERE, func, t.toString(), t, params);
 }

 /**
  *
  * @param func .
  * @param msg .
  * @param t .
  * @param params the message parameters.
  */
 public final void severe(final String func, final String msg, final Throwable t, final Object...params) {
   logp(Level.SEVERE, func, msg, t, params);
 }

  /**
   *
   * @param level .
   * @param func .
   * @param format .
   * @param params the message parameters.
   */
  public void logp(final Level level, final String func, final String format, final Object...params) {
    logger.logp(level, className, func, getMsg(format, params));
  }

  /**
   *
   * @param level .
   * @param func .
   * @param format .
   * @param t .
   * @param params the message parameters.
   */
  public void logp(final Level level, final String func, final String format, final Throwable t, final Object...params) {
    logger.logp(level, className, func, getMsg(format, params), t);
  }

  /**
   * Get a formatted message.
   * @param format the format.
   * @param params the format parameters, if any.
   * @return the formatted message.
   */
  private String getMsg(final String format, final Object...params) {
    return ((params == null) || (params.length <= 0)) ? format : String.format(format, params);
  }
}
