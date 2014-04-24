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

package org.jppf.example.extendedclassloading;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * A filter for the content of a repository.
 * @author Laurent Cohen
 */
public interface RepositoryFilter extends Serializable {
  /**
   * Dtermine whether the specified repository entry is accepted by this filter.
   * @param name the name of the entry.
   * @param signature the signature of the entry.
   * @return <code>true</code> if the entry is accepted, <code>false</code> otherwise.
   */
  boolean accepts(String name, String signature);

  /**
   * A filter implementation which only accepts entries provided by a {@link ClassPath}.
   */
  public static class ClassPathFilter implements RepositoryFilter {
    /**
     * The class path providing the accepted entries.
     */
    private final ClassPath classpath;

    /**
     * Initialize this flter with the specified classpath.
     * @param classpath the class path providing the accepted entries. If null, no entry will be accepted.
     */
    public ClassPathFilter(final ClassPath classpath) {
      this.classpath = classpath;
    }

    @Override
    public boolean accepts(final String name, final String signature) {
      if (classpath == null) return false;
      String classpathSignature = classpath.getElementSignature(name);
      return ((classpathSignature == null) || !classpathSignature.equals(signature));
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "(" + classpath + ")";
    }
  }

  /**
   * A filter implementation which accepts entries according to a regular expression.
   * Only the name of each entry is matched against the regex and the signature is ignored.
   */
  public static class RegExFilter implements RepositoryFilter {
    /**
     * The regular expression to match against entry names for acceptance.
     */
    private final String regEx;
    /**
     * A regex pattern initialized upon the first use of this filter.
     */
    private transient Pattern pattern;

    /**
     * Initialize this flter with the specified regular expression interpreted as a simple 'wildcard'-based expression.
     * This is equivalent to <code>new RegExFilter(regEx, false)</code>.
     * @param regEx the regular expression to match against entry names for acceptance.
     */
    public RegExFilter(final String regEx) {
      this(regEx, false);
    }

    /**
     * Initialize this flter with the specified regex and regex type flag.
     * @param regEx the regular expression to match against entry names for acceptance.
     * @param regexSyntax if <code>true</code> then the regex is interpreted as defined in {@link Pattern},
     * otherwise it is built from a simpler 'wildcard'-based syntax which only uses
     * '*' and '?', with a meaning common to most OS shells.
     * For instance, <code>myLib*v1-?.jar</code> will be transformed into the regex <code>myLib.*v1-.?\\.jar</code> (note how the final '.' is escaped),
     * it will match <code>myLibrary-v1-1.jar</code> but not <code>myLibrary-v11.jar</code>
     */
    public RegExFilter(final String regEx, final boolean regexSyntax) {
      this.regEx = regexSyntax ? regEx : ClassPathHelper.wildcardToRegex(regEx);
    }

    @Override
    public boolean accepts(final String name, final String signature) {
      if (pattern == null) pattern = Pattern.compile(regEx);
      return pattern.matcher(name).matches();
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "(" + regEx + ")";
    }
  }
}
