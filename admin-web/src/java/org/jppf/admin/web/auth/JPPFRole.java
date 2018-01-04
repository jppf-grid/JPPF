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

package org.jppf.admin.web.auth;

import java.util.*;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;

/**
 * 
 * @author Laurent Cohen
 */
public enum JPPFRole {
  /**
   * Monitor role.
   */
  MONITOR(JPPFRoles.MONITOR),
  /**
   * Manager role.
   */
  MANAGER(JPPFRoles.MANAGER, MONITOR),
  /**
   * Admin role.
   */
  ADMIN(JPPFRoles.ADMIN, MONITOR);

  /**
   * The role name.
   */
  private final String roleName;
  /**
   * The implied roles, if any.
   */
  private final JPPFRole[] impliedRoles;
  
  /**
   * Initialize this role with the specified role name and implied roles.
   * @param roleName the name of this role.
   * @param impliedRoles the roles this one implies, if any.
   */
  private JPPFRole(final String roleName, final JPPFRole...impliedRoles) {
    this.roleName = roleName;
    this.impliedRoles = (impliedRoles == null) ? new JPPFRole[0] : impliedRoles;
  }

  /**
   * Determine whether this role implies the specified one.
   * @param role the role to check for implication.
   * @return {@code true} if the role is implied, {@code false} otherwise.
   */
  public boolean implies(final JPPFRole role) {
    if (role == null) return false;
    if (role == this) return true;
    for (JPPFRole r: impliedRoles) {
      if (r == role) return true;
    }
    return false;
  }

  /**
   * @return the role name.
   */
  public String getRoleName() {
    return roleName;
  }

  /**
   * @return the implied roles.
   */
  public JPPFRole[] getImpliedRoles() {
    if (impliedRoles.length <= 0) return impliedRoles;
    final JPPFRole[] tmp = new JPPFRole[impliedRoles.length];
    System.arraycopy(impliedRoles, 0, tmp, 0, impliedRoles.length);
    return tmp;
  }

  /**
   * Get the role associated with the specified role name.
   * @param roleName the role name to lookup.
   * @return a {@link JPPFRole} enum value, or {@code null} if there is no matching role.
   */
  public static JPPFRole getRole(final String roleName) {
    if (roleName != null) {
      for (JPPFRole role: JPPFRole.values()) {
        if (roleName.equals(role.getRoleName())) return role;
      }
    }
    return null;
  }

  /**
   * Get the roles the current user has.
   * @param roles the roles determined from the current user session.
   * @return a set of roles, possibly empty.
   */
  public static Set<String> getRoles(final Roles roles) {
    final Set<String> set = new HashSet<>();
    if (roles != null) {
      for (final JPPFRole role: JPPFRole.values()) {
        if (roles.hasRole(role.getRoleName())) set.add(role.getRoleName());
      }
    }
    return set;
  }
}
