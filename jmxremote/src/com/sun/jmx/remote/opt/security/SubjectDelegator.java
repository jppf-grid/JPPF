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
 * @(#)SubjectDelegator.java	1.3
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

import javax.management.remote.SubjectDelegationPermission;
import javax.security.auth.Subject;

import com.sun.jmx.remote.opt.util.CacheMap;

/**
 * 
 */
public class SubjectDelegator {
  /**
   * 
   */
  private static final int PRINCIPALS_CACHE_SIZE = 10;
  /**
   * 
   */
  private static final int ACC_CACHE_SIZE = 10;
  /**
   * 
   */
  private CacheMap<Subject, Principal[]> principalsCache;
  /**
   * 
   */
  private CacheMap<Subject, AccessControlContext> accCache;

  /**
   * Return the AccessControlContext appropriate to execute an operation on behalf of the delegatedSubject.
   * If the authenticatedAccessControlContext does not have permission to delegate to that subject, throw SecurityException.
   * @param authenticatedACC .
   * @param delegatedSubject .
   * @return .
   * @throws SecurityException .
   */
  public synchronized AccessControlContext delegatedContext(final AccessControlContext authenticatedACC, final Subject delegatedSubject) throws SecurityException {
    if (principalsCache == null || accCache == null) {
      principalsCache = new CacheMap<>(PRINCIPALS_CACHE_SIZE);
      accCache = new CacheMap<>(ACC_CACHE_SIZE);
    }
    // Retrieve the principals for the given delegated subject from the cache
    Principal[] delegatedPrincipals = principalsCache.get(delegatedSubject);
    // Convert the set of principals stored in the delegated subject into an array of principals and store it in the cache
    if (delegatedPrincipals == null) {
      delegatedPrincipals = delegatedSubject.getPrincipals().toArray(new Principal[0]);
      principalsCache.put(delegatedSubject, delegatedPrincipals);
    }
    // Retrieve the access control context for the given delegated subject from the cache
    AccessControlContext delegatedACC = accCache.get(delegatedSubject);
    // Build the access control context to be used when executing code as the delegated subject and store it in the cache
    if (delegatedACC == null) {
      final JMXSubjectDomainCombiner sdc = new JMXSubjectDomainCombiner(delegatedSubject);
      delegatedACC = new AccessControlContext(AccessController.getContext(), sdc);
      accCache.put(delegatedSubject, delegatedACC);
    }
    // Check if the subject delegation permission allows the authenticated subject to assume the identity of each principal in the delegated subject
    final Principal[] dp = delegatedPrincipals;
    final PrivilegedAction<Object> action = new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        for (int i = 0; i < dp.length; i++) {
          final String pname = dp[i].getClass().getName() + "." + dp[i].getName();
          final Permission sdp = new SubjectDelegationPermission(pname);
          AccessController.checkPermission(sdp);
        }
        return null;
      }
    };
    AccessController.doPrivileged(action, authenticatedACC);
    return delegatedACC;
  }
}
