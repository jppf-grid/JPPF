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
/*
 * @(#)file      ProfileProviderException.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.6
 * @(#)lastedit  07/03/08
 * @(#)build     @BUILD_TAG_PLACEHOLDER@
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

package com.sun.jmx.remote.generic;

/**
 * <p>Exception thrown by {@link ProfileClientFactory} and
 * {@link ProfileServerFactory} when a provider exists for the
 * required profile name but cannot be used for some reason.</p>
 */
public class ProfileProviderException extends Exception {
  /**
   * 
   */
  private static final long serialVersionUID = -4648936399123015595L;
  /**
   * 
   */
  private Exception cause = null;

  /**
   * <p>Constructs a <code>ProfileProviderException</code> with no
   * specified detail message.</p>
   */
  public ProfileProviderException() {
  }

  /**
   * <p>Constructs a <code>ProfileProviderException</code> with the
   * specified detail message.</p>
   *
   * @param message the detail message
   */
  public ProfileProviderException(final String message) {
    super(message);
  }

  /**
   * <p>Constructs a <code>ProfileProviderException</code> with the
   * specified detail message and nested exception.</p>
   *
   * @param message the detail message
   * @param cause the nested exception
   */
  public ProfileProviderException(final String message, final Exception cause) {
    super(message);
    this.cause = cause;
  }

  @Override
  public Throwable getCause() {
    return cause;
  }
}
