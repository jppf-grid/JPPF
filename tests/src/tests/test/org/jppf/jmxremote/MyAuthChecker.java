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

import javax.management.*;

import org.jppf.jmxremote.JMXAuthorizationCheckerAdapter;

/**
 * Simple authroization check (aka ACL) for testing.
 */
public class MyAuthChecker extends JMXAuthorizationCheckerAdapter {
  @Override
  public void checkInvoke(final ObjectName name, final String operationName, final Object[] params, final String[] signature) throws Exception {
    for (final Principal principal: subject.getPrincipals()) {
      final String user = principal.getName();
      if ("jppf1".equals(user) && operationName.endsWith("2")) throw new SecurityException("user 'jppf1' cannot invoke " + operationName);
      else if ("jppf2".equals(user) && operationName.endsWith("1")) throw new SecurityException("user 'jppf2' cannot invoke " + operationName);
    }
  }

  @Override
  public void checkGetAttribute(final ObjectName name, final String attribute) throws Exception {
    for (final Principal principal: subject.getPrincipals()) {
      final String user = principal.getName();
      if ("jppf2".equals(user)) throw new SecurityException("user 'jppf2' cannot get attribute " + attribute);
    }
  }

  @Override
  public void checkSetAttribute(final ObjectName name, final Attribute attribute) throws Exception {
    for (final Principal principal: subject.getPrincipals()) {
      final String user = principal.getName();
      if ("jppf2".equals(user)) throw new SecurityException("user 'jppf2' cannot set attribute " + attribute.getName());
    }
  }

  @Override
  public void checkSetAttributes(final ObjectName name, final AttributeList attributes) throws Exception {
    for (Attribute attr: attributes.asList()) checkSetAttribute(name, attr);
  }
}