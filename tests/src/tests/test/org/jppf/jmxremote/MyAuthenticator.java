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

package test.org.jppf.jmxremote;

import java.security.Principal;
import java.util.*;

import javax.management.remote.*;
import javax.security.auth.Subject;

import org.jppf.utils.Pair;

/**
 * A simple authenticator for testing.
 */
public class MyAuthenticator implements JMXAuthenticator {
  /**
   * The set of authorized users.
   */
  private final Set<String> authorizedUsers = new HashSet<>(Arrays.asList("jppf1", "jppf2"));

  @Override
  public Subject authenticate(final Object credentials) {
    if (!(credentials instanceof String[])) throw new SecurityException("wrong type for credentials");
    final String[] cred = (String[]) credentials;
    if (cred.length != 2) throw new SecurityException("credentials array should have length of 2 but has length of " + cred.length);
    final String user = cred[0];
    if (user == null) throw new SecurityException("null user is not allowed");
    if (!authorizedUsers.contains(user)) throw new SecurityException("user '" + user + "' is not allowed");
    final String pwd = cred[1];
    if (pwd == null) throw new SecurityException("null password for user '" + user + "'");
    if (!("pwd_" + user).equals(pwd)) throw new SecurityException("wrong password for user '" + user + "'");
    final Set<Principal> principals = new HashSet<>();
    principals.add(new JMXPrincipal(user));
    final Set<Object> privateCredentials = new HashSet<>();
    privateCredentials.add(new UserPwd(user, pwd));
    return new Subject(true, principals, new HashSet<>(), privateCredentials);
  }

  /** */
  public static class UserPwd extends Pair<String, String> {
    /**
     * @param user .
     * @param pwd .
     */
    public UserPwd(final String user, final String pwd) { super(user, pwd); }
  
    /**
     * @return the user.
     */
    public String user() { return first(); }
  
    /**
     * @return the passord.
     */
    public String pwd() { return second(); }
  }
}