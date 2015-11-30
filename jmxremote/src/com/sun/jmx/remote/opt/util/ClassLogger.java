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
   */
  public final void debug(final String func, final String msg) {
    finest(func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void debug(final String func, final Throwable t) {
    finest(func, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void debug(final String func, final String msg, final Throwable t) {
    finest(func, msg, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   */
  public final void trace(final String func, final String msg) {
    finer(func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void trace(final String func, final Throwable t) {
    finer(func, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void trace(final String func, final String msg, final Throwable t) {
    finer(func, msg, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   */
  public final void error(final String func, final String msg) {
    severe(func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void error(final String func, final Throwable t) {
    severe(func, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void error(final String func, final String msg, final Throwable t) {
    severe(func, msg, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   */
  public final void finest(final String func, final String msg) {
    logger.logp(Level.FINEST, className, func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void finest(final String func, final Throwable t) {
    logger.logp(Level.FINEST, className, func, t.toString(), t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void finest(final String func, final String msg, final Throwable t) {
    logger.logp(Level.FINEST, className, func, msg, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   */
  public final void finer(final String func, final String msg) {
    logger.logp(Level.FINER, className, func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void finer(final String func, final Throwable t) {
    logger.logp(Level.FINER, className, func, t.toString(), t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void finer(final String func, final String msg, final Throwable t) {
    logger.logp(Level.FINER, className, func, msg, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   */
  public final void fine(final String func, final String msg) {
    logger.logp(Level.FINE, className, func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void fine(final String func, final Throwable t) {
    logger.logp(Level.FINE, className, func, t.toString(), t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void fine(final String func, final String msg, final Throwable t) {
    logger.logp(Level.FINE, className, func, msg, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   */
  public final void config(final String func, final String msg) {
    logger.logp(Level.CONFIG, className, func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void config(final String func, final Throwable t) {
    logger.logp(Level.CONFIG, className, func, t.toString(), t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void config(final String func, final String msg, final Throwable t) {
    logger.logp(Level.CONFIG, className, func, msg, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   */
  public final void info(final String func, final String msg) {
    logger.logp(Level.INFO, className, func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void info(final String func, final Throwable t) {
    logger.logp(Level.INFO, className, func, t.toString(), t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void info(final String func, final String msg, final Throwable t) {
    logger.logp(Level.INFO, className, func, msg, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   */
  public final void warning(final String func, final String msg) {
    logger.logp(Level.WARNING, className, func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void warning(final String func, final Throwable t) {
    logger.logp(Level.WARNING, className, func, t.toString(), t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void warning(final String func, final String msg, final Throwable t) {
    logger.logp(Level.WARNING, className, func, msg, t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   */
  public final void severe(final String func, final String msg) {
    logger.logp(Level.SEVERE, className, func, msg);
  }

  /**
   * 
   * @param func .
   * @param t .
   */
  public final void severe(final String func, final Throwable t) {
    logger.logp(Level.SEVERE, className, func, t.toString(), t);
  }

  /**
   * 
   * @param func .
   * @param msg .
   * @param t .
   */
  public final void severe(final String func, final String msg, final Throwable t) {
    logger.logp(Level.SEVERE, className, func, msg, t);
  }
}
