/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
 * @(#)JMXSubjectDomainCombiner.java	1.3
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

package com.sun.jmx.remote.opt.security;

import java.security.*;

import javax.security.auth.*;

/**
 * <p>This class represents an extension to the {@link SubjectDomainCombiner} and is used to add a new {@link ProtectionDomain},
 * comprised of a null codesource/signers and an empty permission set, to the access control context with which this combiner is combined.
 * <p>When the {@link #combine} method is called the {@link ProtectionDomain} is augmented with the permissions granted to the set of principals present in the supplied {@link Subject}.
 */
public class JMXSubjectDomainCombiner extends SubjectDomainCombiner {
  /**
   *
   */
  private static final CodeSource nullCodeSource = new CodeSource(null, (java.security.cert.Certificate[]) null);
  /**
   *
   */
  private static final ProtectionDomain pd = new ProtectionDomain(nullCodeSource, new Permissions());

  /**
   *
   * @param s .
   */
  public JMXSubjectDomainCombiner(final Subject s) {
    super(s);
  }

  /**
   *
   * @param current .
   * @param assigned .
   * @return .
   */
  @Override
  public ProtectionDomain[] combine(final ProtectionDomain[] current, final ProtectionDomain[] assigned) {
    // Add a new ProtectionDomain with the null codesource/signers, and the empty permission set, to the end of the array containing the
    // 'current' protections domains, i.e. the ones that will be augmented with the permissions granted to the set of principals present inthe supplied subject.
    ProtectionDomain[] newCurrent;
    if (current == null || current.length == 0) {
      newCurrent = new ProtectionDomain[1];
      newCurrent[0] = pd;
    } else {
      newCurrent = new ProtectionDomain[current.length + 1];
      for (int i = 0; i < current.length; i++) newCurrent[i] = current[i];
      newCurrent[current.length] = pd;
    }
    return super.combine(newCurrent, assigned);
  }
}
