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

import static org.junit.Assert.*;

import java.util.*;

import javax.management.*;
import javax.management.remote.*;

import org.jppf.jmxremote.*;
import org.jppf.utils.*;
import org.junit.*;

/**
 * Tests for the jmxremote-nio connector.
 * @author Laurent Cohen
 */
public class TestStandaloneConnectorAuth extends AbstractTestStandaloneConnector {
  /**
   * Performs setup before each test.
   * @throws Exception if any error occurs.
   */
  @Before
  public void beforeInstance() throws Exception {
    print(false, false, "***** starting connector server *****");
    final Map<String, Object> env = new HashMap<>();
    env.put(JMXConnectorServer.AUTHENTICATOR, new MyAuthenticator());
    env.put(JPPFJMXConnectorServer.AUTHORIZATION_CHECKER, MyAuthChecker.class);
    server = createConnectorServer(env);
    registerMBeans();
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthenticationFailureWrongCredentialType() throws Exception {
    final Exception ex = initConnector(25);
    assertNotNull(ex);
    final Throwable cause = ex.getCause();
    assertTrue("cause = " + cause, cause instanceof SecurityException);
    assertEquals("wrong type for credentials", cause.getMessage());
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthenticationFailureWrongCredentialArrayLength() throws Exception {
    final Exception ex = initConnector("jppf1", "pwd_jppf1", "whatever");
    assertNotNull(ex);
    final Throwable cause = ex.getCause();
    assertTrue("cause = " + cause, cause instanceof SecurityException);
    assertEquals("credentials array should have length of 2 but has length of 3", cause.getMessage());
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthenticationFailureNullUser() throws Exception {
    final Exception ex = initConnector(null, "pwd_jppf1");
    assertNotNull(ex);
    final Throwable cause = ex.getCause();
    assertTrue("cause = " + ExceptionUtils.getStackTrace(cause), cause instanceof SecurityException);
    assertEquals("null user is not allowed", cause.getMessage());
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthenticationFailureUnauthorizedUser() throws Exception {
    final Exception ex = initConnector("jppf27", "pwd_jppf1");
    assertNotNull(ex);
    final Throwable cause = ex.getCause();
    assertTrue("cause = " + ExceptionUtils.getStackTrace(cause), cause instanceof SecurityException);
    assertEquals("user 'jppf27' is not allowed", cause.getMessage());
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthenticationFailureWrongPassword() throws Exception {
    final Exception ex = initConnector("jppf1", "hello_jppf1");
    assertNotNull(ex);
    final Throwable cause = ex.getCause();
    assertTrue("cause = " + ExceptionUtils.getStackTrace(cause), cause instanceof SecurityException);
    assertEquals("wrong password for user 'jppf1'", cause.getMessage());
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthenticationSuccess() throws Exception {
    final Exception ex = initConnector("jppf1", "pwd_jppf1");
    assertNull(ex);
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthorizationFailureInvoke() throws Exception {
    initConnector("jppf1", "pwd_jppf1");
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    Object result = null;
    Exception ex = null;
    try {
      result = mbsc.invoke(connectorTestName, "test2", arrayOf("test_string"), arrayOf(String.class.getName()));
    } catch (final Exception e) {
      ex = e;
    }
    assertNull(result);
    assertNotNull(ex);
    final Throwable cause = ex.getCause();
    assertTrue("ex = " + ExceptionUtils.getMessage(ex) + ", cause = " + ExceptionUtils.getMessage(cause), cause instanceof SecurityException);
    assertEquals("user 'jppf1' cannot invoke test2", cause.getMessage());
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthorizationSuccessInvoke() throws Exception {
    initConnector("jppf2", "pwd_jppf2");
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    Object result = null;
    Exception ex = null;
    try {
      result = mbsc.invoke(connectorTestName, "test2", arrayOf("test_string"), arrayOf(String.class.getName()));
    } catch (final Exception e) {
      ex = e;
    }
    assertNotNull(result);
    assertEquals("[test_string]", result);
    assertNull(ex);
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthorizationFailureGetAttribute() throws Exception {
    initConnector("jppf2", "pwd_jppf2");
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    Object result = null;
    Exception ex = null;
    try {
      result = mbsc.getAttribute(connectorTestName, "StringParam");
    } catch (final Exception e) {
      ex = e;
    }
    assertNull(result);
    assertNotNull(ex);
    final Throwable cause = ex.getCause();
    assertTrue("ex = " + ExceptionUtils.getMessage(ex) + ", cause = " + ExceptionUtils.getMessage(cause), cause instanceof SecurityException);
    assertEquals("user 'jppf2' cannot get attribute StringParam", cause.getMessage());
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthorizationSuccessGetAttribute() throws Exception {
    initConnector("jppf1", "pwd_jppf1");
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    Object result = null;
    Exception ex = null;
    try {
      result = mbsc.getAttribute(connectorTestName, "StringParam");
    } catch (final Exception e) {
      ex = e;
    }
    assertNotNull(result);
    assertEquals("initial_value", result);
    assertNull(ex);
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthorizationFailureSetAttribute() throws Exception {
    initConnector("jppf2", "pwd_jppf2");
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    Exception ex = null;
    try {
      mbsc.setAttribute(connectorTestName, new Attribute("StringParam", "new value"));
    } catch (final Exception e) {
      ex = e;
    }
    assertNotNull(ex);
    final Throwable cause = ex.getCause();
    assertTrue("ex = " + ExceptionUtils.getMessage(ex) + ", cause = " + ExceptionUtils.getMessage(cause), cause instanceof SecurityException);
    assertEquals("user 'jppf2' cannot set attribute StringParam", cause.getMessage());
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthorizationSuccessSetAttribute() throws Exception {
    initConnector("jppf1", "pwd_jppf1");
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    Exception ex = null;
    try {
      mbsc.setAttribute(connectorTestName, new Attribute("StringParam", "new value"));
    } catch (final Exception e) {
      ex = e;
    }
    assertNull(ex);
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthorizationFailureSetAttributes() throws Exception {
    initConnector("jppf2", "pwd_jppf2");
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    Exception ex = null;
    try {
      final AttributeList list = new AttributeList();
      list.add(new Attribute("IntParam", 12));
      list.add(new Attribute("StringParam", "new value"));
      mbsc.setAttributes(connectorTestName, list);
    } catch (final Exception e) {
      ex = e;
    }
    assertNotNull(ex);
    final Throwable cause = ex.getCause();
    assertTrue("ex = " + ExceptionUtils.getMessage(ex) + ", cause = " + ExceptionUtils.getMessage(cause), cause instanceof SecurityException);
    assertEquals("user 'jppf2' cannot set attribute IntParam", cause.getMessage());
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAuthorizationSuccessSetAttributes() throws Exception {
    initConnector("jppf1", "pwd_jppf1");
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    Exception ex = null;
    try {
      final AttributeList list = new AttributeList();
      list.add(new Attribute("IntParam", 12));
      list.add(new Attribute("StringParam", "new value"));
      mbsc.setAttributes(connectorTestName, list);
    } catch (final Exception e) {
      ex = e;
    }
    assertNull(ex);
  }

  /**
   * @param creds .
   * @return .
   */
  private Exception initConnector(final Object...creds) {
    final Map<String, Object> env = new HashMap<>();
    env.put(JMXConnector.CREDENTIALS, creds);
    try {
      clientConnector = createConnectorClient(env);
    } catch(final Exception e) {
      return e;
    }
    return null;
  }

  /**
   * @param creds .
   * @return .
   */
  private Exception initConnector(final String...creds) {
    final Map<String, Object> env = new HashMap<>();
    env.put(JMXConnector.CREDENTIALS, creds);
    try {
      clientConnector = createConnectorClient(env);
    } catch(final Exception e) {
      return e;
    }
    return null;
  }

  /**
   * Convenience method to create an array of any object type.
   * @param <T> the type of the elements in the array.
   * @param array the elements of the array to provide.
   * @return an array of the specified type.
   */
  static <T> T[] arrayOf(@SuppressWarnings("unchecked") final T...array) {
    return array;
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
