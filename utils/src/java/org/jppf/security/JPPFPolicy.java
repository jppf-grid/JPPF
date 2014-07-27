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
package org.jppf.security;

import java.security.*;

import org.slf4j.*;

/**
 * Security policy for JPPF Nodes.
 * @author Laurent Cohen
 */
public class JPPFPolicy extends Policy {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFPolicy.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * ClassLoader used to retrieve the policy file.
   */
  private ClassLoader classLoader = null;

  /**
   * Initialize this policy.
   * @param classLoader the <code>ClassLoader</code> used to retrieve the policy file
   * through a call to <code>getResourceAsStream(String)</code>; may be null.
   * @see java.lang.ClassLoader#getResourceAsStream(String).
   */
  public JPPFPolicy(final ClassLoader classLoader) {
    this.classLoader = classLoader;
    PermissionsFactory.getPermissions(classLoader);
  }

  /**
   * Get the permissions for this policy.
   * @param codesource not used.
   * @return a collection of permissions.
   * @see java.security.Policy#getPermissions(java.security.CodeSource)
   */
  @Override
  public PermissionCollection getPermissions(final CodeSource codesource) {
    if (debugEnabled) log.debug("in getPermissions(CodeSource) : " + toString(codesource));
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if ((cl == classLoader) || !cl.getClass().getName().contains("org.jppf"))
      return PermissionsFactory.getExtendedPermissions(classLoader);
    return PermissionsFactory.getPermissions(classLoader);
  }

  /**
   * Get the permissions for a specified protection domain.
   * @param domain the protection domain to get the permissions for.
   * @return a <code>PermissionCollection</code> instance.
   * @see java.security.Policy#getPermissions(java.security.ProtectionDomain)
   */
  @Override
  public PermissionCollection getPermissions(final ProtectionDomain domain) {
    // domain.toString() causes a StackOverflowException - because it makes its own security checks that invoke this policy
    if (debugEnabled) log.debug("in getPermissions(ProtectionDomain) : " + toString(domain));
    ClassLoader cl = domain.getClassLoader();
    if ((cl == classLoader) || !cl.getClass().getName().contains("org.jppf"))
      return PermissionsFactory.getExtendedPermissions(classLoader);
    return PermissionsFactory.getPermissions(classLoader);
  }

  /**
   * This method does nothing.
   * @see java.security.Policy#refresh()
   */
  @Override
  public void refresh() {
  }

  /**
   * Get a string representation of a <code>CodeSource</code> object.
   * @param code the code source to print.
   * @return a string representing the specified code source.
   */
  private static String toString(final CodeSource code) {
    if (code == null) return "null";
    StringBuilder sb = new StringBuilder().append("location = ").append(code.getLocation());
    return sb.toString();
  }

  /**
   * Get a string representation of a <code>ProtectionDomain</code> object.
   * @param domain the protection domain to print.
   * @return a string representing the specified protection domain.
   */
  private static String toString(final ProtectionDomain domain) {
    StringBuilder sb = new StringBuilder().append("class loader = ").append(domain.getClassLoader());
    sb.append(", code source = [").append(toString(domain.getCodeSource())).append(']');
    return sb.toString();
  }

  /**
   * .
   * @param domain .
   * @param permission .
   * @return .
   * @see java.security.Policy#implies(java.security.ProtectionDomain, java.security.Permission)
   */
  @Override
  public boolean implies(final ProtectionDomain domain, final Permission permission) {
    if (debugEnabled) {
      if (permission instanceof RuntimePermission) {
        RuntimePermission rp = (RuntimePermission) permission;
        String action = rp.getActions();
        if ((action != null) && (action.contains("exitVM"))) {
          log.debug("in implies(exitVM)", new Exception());
        }
      }
    }
    return super.implies(domain, permission);
  }
}
