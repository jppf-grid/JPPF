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
 * @(#)file      ProfileClientFactory.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.8
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

import java.security.*;
import java.util.*;

/**
 * <p>Factory to create profiles. There are no instances of this class.
 * <p>Each profile is created by an instance of {@link ProfileClientProvider}. This instance is found as follows. Suppose the given <code><em>profile</em></code> looks like <code>TLS</code>. Then the
 * factory will attempt to find the appropriate {@link ProfileClientProvider} for <code><em>tls</em></code>. Suppose the given <code><em>profile</em></code> looks like <code>SASL/PLAIN</code>. Then
 * the factory will attempt to find the appropriate {@link ProfileClientProvider} for <code><em>sasl</em></code>. The <code><em>profile</em></code> string passed in to the factory is converted into
 * lowercase and all the characters after the <code>/</code> character are discarded.
 * <p>A <em>provider package list</em> is searched for as follows:
 * <ol>
 * <li>If the <code>environment</code> parameter to {@link #createProfile(String, Map) createProfile} contains the key <code>jmx.remote.profile.provider.pkgs</code> then the associated value is the
 * provider package list.
 * <li>Otherwise, if the system property <code>jmx.remote.profile.provider.pkgs</code> exists, then its value is the provider package list.
 * <li>Otherwise, there is no provider package list.
 * </ol>
 * <p>The provider package list is a string that is interpreted as a list of non-empty Java package names separated by vertical bars (<code>|</code>). If the string is empty, then so is the provider
 * package list. If the provider package list is not a String, or if it contains an element that is an empty string, a {@link ProfileProviderException} is thrown.
 * <p>If the provider package list exists and is not empty, then for each element <code><em>pkg</em></code> of the list, the factory will attempt to load the class
 * <blockquote> <code><em>pkg</em>.<em>profile</em>.ClientProvider</code> </blockquote>
 * <p>If the <code>environment</code> parameter to {@link #createProfile(String, Map) createProfile} contains the key <code>jmx.remote.profile.provider.class.loader</code> then the associated value is
 * the class loader to use to load the provider. If the associated value is not an instance of {@link java.lang.ClassLoader}, an {@link java.lang.IllegalArgumentException} is thrown.
 * <p>If the <code>jmx.remote.profile.provider.class.loader</code> key is not present in the <code>environment</code> parameter, the class loader that loaded the <code>ProfileClientFactory</code> class
 * is used.
 * <p>If the attempt to load this class produces a {@link ClassNotFoundException}, the search for a provider continues with the next element of the list.
 * </p>
 * <p>
 * Otherwise, a problem with the found provider is signalled by a {@link ProfileProviderException} whose {@link ProfileProviderException#getCause() <em>cause</em>} indicates the underlying exception,
 * as follows:
 * <ul>
 * <li>if the attempt to load the class produces an exception other than <code>ClassNotFoundException</code>, that is the <em>cause</em>;
 * <li>if {@link Class#newInstance()} for the class produces an exception, that is the <em>cause</em>.
 * </ul>
 * <p>If no provider is found by the above steps, including the default case where there is no provider package list, then the implementation will use its own provider for <code><em>profile</em></code>,
 * or it will throw a <code>IllegalArgumentException</code> if there is none.
 * <p>Once a provider is found, the result of the <code>createProfile</code> method is the result of calling {@link ProfileClientProvider#createProfile(String,Map) createProfile} on the provider.
 * <p>
 * The <code>Map</code> parameter passed to the <code>ProfileClientProvider</code> is a new read-only copy of the <code>environment</code> parameter to {@link #createProfile(String, Map)
 * ProfileClientFactory.createProfile}, or an empty <code>Map</code> if that parameter is null. If the <code>jmx.remote.profile.provider.class.loader</code> key is not present in the
 * <code>environment</code> parameter, it is added to the new read-only <code>Map</code>. The associated value is the class loader that loaded the <code>ProfileClientFactory</code> class.
 */
public final class ProfileClientFactory {

  /**
   * Name of the attribute that specifies the provider packages that are consulted when looking for the provider for a profile. The value associated with this attribute is a string with package names
   * separated by vertical bars (<code>|</code>).
   */
  public static final String PROFILE_PROVIDER_PACKAGES = "jmx.remote.profile.provider.pkgs";
  /**
   * Name of the attribute that specifies the class loader for loading profile providers. The value associated with this attribute is an instance of {@link ClassLoader}.
   */
  public static final String PROFILE_PROVIDER_CLASS_LOADER = "jmx.remote.profile.provider.class.loader";
  /**
   *
   */
  private static final String PROFILE_PROVIDER_DEFAULT_PACKAGE = "com.sun.jmx.remote.profile";

  /**
   * There are no instances of this class.
   */
  private ProfileClientFactory() {
  }

  /**
   * <p>Create a profile.</p>
   * @param profile the name of the profile to be created.
   * @param environment a read-only Map containing named attributes to determine how the profile is created. Keys in this map must be Strings.
   * The appropriate type of each associated value depends on the attribute.</p>
   * @return a <code>ProfileClient</code> representing the new profile. Each successful call to this method produces a different object.
   * @exception NullPointerException if <code>profile</code> is null.
   * @throws ProfileProviderException .
   */
  public static ProfileClient createProfile(final String profile, final Map<String, Object> environment) throws ProfileProviderException {
    final String pkgs = resolvePkgs(environment);
    Map<String, Object> env = environment;
    final ClassLoader loader = resolveClassLoader(environment);
    if (env == null) env = new HashMap<>();
    else env = new HashMap<>(environment);
    env.put(PROFILE_PROVIDER_CLASS_LOADER, loader);
    env = Collections.unmodifiableMap(env);
    final ProfileClientProvider provider = getProvider(profile, pkgs, loader);
    if (provider == null) throw new IllegalArgumentException("Unsupported profile: " + profile);
    return provider.createProfile(profile, env);
  }

  /**
   * 
   * @param env .
   * @return .
   */
  private static String resolvePkgs(final Map<String, Object> env) {
    String pkgs = null;
    if (env != null) pkgs = (String) env.get(PROFILE_PROVIDER_PACKAGES);
    if (pkgs == null) pkgs = AccessController.doPrivileged(new PrivilegedAction<String>() {
      @Override
      public String run() {
        return System.getProperty(PROFILE_PROVIDER_PACKAGES);
      }
    });
    if (pkgs == null || pkgs.trim().equals("")) pkgs = PROFILE_PROVIDER_DEFAULT_PACKAGE;
    else pkgs += "|" + PROFILE_PROVIDER_DEFAULT_PACKAGE;
    return pkgs;
  }

  /**
   * 
   * @param profile .
   * @param pkgs .
   * @param loader .
   * @return .
   * @throws ProfileProviderException .
   */
  private static ProfileClientProvider getProvider(final String profile, final String pkgs, final ClassLoader loader) throws ProfileProviderException {
    Class<?> providerClass = null;
    final ProfileClientProvider provider = null;
    Object obj = null;
    final StringTokenizer tokenizer = new StringTokenizer(pkgs, "|");
    String p = profile.toLowerCase();
    if (p.indexOf("/") != -1) {
      p = p.substring(0, p.indexOf("/"));
    }
    while (tokenizer.hasMoreTokens()) {
      final String pkg = tokenizer.nextToken();
      final String className = (pkg + "." + p + ".ClientProvider");
      try {
        providerClass = loader.loadClass(className);
      } catch (@SuppressWarnings("unused") final ClassNotFoundException e) {
        continue;
      }
      try {
        obj = providerClass.newInstance();
      } catch (final Exception e) {
        final String msg = "Exception when instantiating provider [" + className + "]";
        throw new ProfileProviderException(msg, e);
      }
      if (!(obj instanceof ProfileClientProvider)) {
        final String msg = "Provider not an instance of " + ProfileClientProvider.class.getName() + ": " + obj.getClass().getName();
        throw new IllegalArgumentException(msg);
      }
      return (ProfileClientProvider) obj;
    }
    return null;
  }

  /**
   * 
   * @param environment .
   * @return .
   */
  private static ClassLoader resolveClassLoader(final Map<String, Object> environment) {
    ClassLoader loader = null;
    if (environment != null) {
      try {
        loader = (ClassLoader) environment.get(PROFILE_PROVIDER_CLASS_LOADER);
      } catch (@SuppressWarnings("unused") final ClassCastException e) {
        final String msg = "ClassLoader not an instance of java.lang.ClassLoader : " + loader.getClass().getName();
        throw new IllegalArgumentException(msg);
      }
    }
    if (loader == null) loader = ProfileClientFactory.class.getClassLoader();
    return loader;
  }
}
