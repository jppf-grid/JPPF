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

package org.jppf.utils.configuration;

import java.util.*;
import java.util.regex.*;

import org.jppf.utils.*;

/**
 * Abstract implementation of the {@link JPPFProperty} interface.
 * @param <T> the type of the value of this property.
 * @author Laurent Cohen
 * @since 5.2
 */
public abstract class AbstractJPPFProperty<T> implements JPPFProperty<T> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Location of the localization resource bundles.
   */
  private String i18nBase = "org.jppf.utils.configuration.i18n.JPPFProperties";
  /**
   * Constant for an empty String array.
   */
  private  static final String[] NO_PARAM = new String[0];
  /**
   * The regex pattern for identifying parameters in a property name. This pattern uses explicit reluctant quantifiers, as opposed
   * to the default greedy quantifiers, to avoid problems when multiple property references are found in a single property value.
   */
  private static final Pattern PARAM_PATTERN = Pattern.compile("(?:\\<){1}?(.*?)\\>+?");
  /**
   * The name of this property.
   */
  private final String name;
  /**
   * Other names that may be given to this property (e.g. older names from previous versions).
   */
  private final String[] aliases;
  /**
   * The default value of this property.
   */
  private final T defaultValue;
  /**
   * The possible values for this property, if any.
   */
  private T[] possibleValues;
  /**
   * The tags that apply to this property.
   */
  private Set<String> tags;
  /**
   * Names of the parmaters used in the property's name, if any.
   */
  private final String[] paramNames;
  /**
   * The doc and short label for this property. Used for caching ot avoid localization lookups after the 1st time.
   */
  private String doc, label;

  /**
   * Initialize this property with the specified name and default value.
   * @param name the name of this property.
   * @param defaultValue the default value of this property, used when the proeprty is not defined.
   * @param aliases other names that may be given to this property (e.g. older names from previous versions).
   */
  public AbstractJPPFProperty(final String name, final T defaultValue, final String...aliases) {
    this.name = name;
    this.defaultValue = defaultValue;
    this.aliases = aliases;
    this.paramNames = parseParams(name);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public T getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String[] getAliases() {
    return aliases;
  }

  @Override
  public String toString(final T value) {
    return (value == null) ? null : value.toString();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", default=").append(defaultValue);
    sb.append(", aliases=").append(Arrays.asList(aliases));
    sb.append(", valueType=").append(valueType());
    if ((possibleValues != null) && (possibleValues.length > 0)) sb.append(", possibleValues=").append(Arrays.asList(possibleValues));
    sb.append(", description=").append(getDocumentation());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the possible values for this property, if any is defined.
   * @return an array of the possible values.
   */
  public T[] getPossibleValues() {
    return possibleValues;
  }

  /**
   * Set the possible values for this property.
   * @param possibleValues an array of the possible values.
   * @return this property.
   */
  public JPPFProperty<T> setPossibleValues(@SuppressWarnings("unchecked") final T... possibleValues) {
    this.possibleValues = possibleValues;
    return this;
  }

  @Override
  public String getDocumentation() {
    if (doc == null) doc = LocalizationUtils.getLocalized(i18nBase, name + ".doc");
    return doc;
  }

  @Override
  public String getShortLabel() {
    if (label == null) label = LocalizationUtils.getLocalized(i18nBase, name);
    return label;
  }

  @Override
  public Set<String> getTags() {
    if (tags == null) {
      tags = new TreeSet<>();
      final List<String> tokens = StringUtils.parseStrings(LocalizationUtils.getLocalized(i18nBase, name + ".tags"), ",", false);
      if (tokens != null) {
        for (String token: tokens) {
          final String t = token.trim();
          if (!tags.contains(t)) tags.add(t);
        }
      } else tags.add("");
    }
    return tags;
  }

  @Override
  public String[] getParameters() {
    return paramNames;
  }


  @Override
  public String getParameterDoc(final String param) {
    return LocalizationUtils.getLocalized(i18nBase, name + "." + param);
  }

  /**
   * @exclude
   */
  @Override
  public String resolveName(final String...params) {
    return resolveName(name, params);
  }

  /**
   * @exclude
   */
  @Override
  public String resolveName(final String alias, final String...params) {
    if ((paramNames.length <= 0) || (params == null) || (params.length <= 0)) return name;
    int n = paramNames.length;
    if (n > params.length) n = params.length;
    String s = alias;
    for (int i=0; i<n; i++) s = s.replace("<" + paramNames[i] + ">", params[i]);
    return s;
  }

  /**
   * Resolve the parameters, if any, included in the property's name.
   * @param name the name of the property to parse.
   * @return an array of parameter names, possibly empty.
   */
  private static String[] parseParams(final String name) {
    final List<String> params = new ArrayList<>();
    final Matcher matcher = PARAM_PATTERN.matcher(name);
    while (matcher.find()) {
      final String param = matcher.group(1);
      params.add(param);
    }
    return params.isEmpty() ? NO_PARAM : params.toArray(new String[params.size()]);
  }

  /**
   * @return the location of the localization resource bundles.
   * @exclude
   */
  public String getI18nBase() {
    return i18nBase;
  }

  /**
   * Set the location of the localization resource bundles.
   * @param i18nBase the location to set.
   * @exclude
   */
  public void setI18nBase(final String i18nBase) {
    this.i18nBase = i18nBase;
  }
}
