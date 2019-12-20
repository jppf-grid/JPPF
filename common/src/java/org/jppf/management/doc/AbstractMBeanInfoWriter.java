/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.management.doc;

import java.io.Writer;
import java.util.*;

import javax.management.Descriptor;

/**
 * Visits the JPPF MBean in a remote JVM and generates a reference documentation page in wikimedia format.
 * @param <E> .
 * @author Laurent Cohen
 */
abstract class AbstractMBeanInfoWriter<E extends AbstractMBeanInfoWriter<E>> extends MBeanInfoVisitorAdapter {
  /**
   * The writer in which to print the generated wiki code.
   */
  Writer writer;
  /**
   * A cache of converted types, for performance optimization.
   */
  final Map<String, String> typeCache = new HashMap<>();
  /**
   * 
   */
  String arraySuffix;
  /**
   * 
   */
  String lt, gt;

  /**
   * Initialize this visitor witht he specified {@link Writer}.
   * @param writer the writer in which to print the generated wiki code.
   */
  public AbstractMBeanInfoWriter(final Writer writer) {
    if (writer == null) throw new NullPointerException("writer cannot be null");
    this.writer = writer;
    this.arraySuffix = "&#91;&#93;";
    lt = "<";
    gt = ">";
  }

  /**
   * Print a formatted message.
   * @param format the message format.
   * @param params the message parameters.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  AbstractMBeanInfoWriter<E> print(final String format, final Object...params) throws Exception {
    final String msg = String.format(format, params);
    writer.write(msg);
    return this;
  }

  /**
   * Print a message, appending a line terminator.
   * @param format the message format.
   * @param params the message parameters.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  AbstractMBeanInfoWriter<E> println(final String format, final Object...params) throws Exception {
    return print(format, params).println();
  }

  /**
   * Print a formatted message.
   * @param msg the message format.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  AbstractMBeanInfoWriter<E> print(final String msg) throws Exception {
    writer.write(msg);
    return this;
  }

  /**
   * Print a message, appending a line terminator.
   * @param msg the message format.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  AbstractMBeanInfoWriter<E> println(final String msg) throws Exception {
    return print(msg).println();
  }

  /**
   * Print a blank line.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  AbstractMBeanInfoWriter<E> println() throws Exception {
    writer.write("\n");
   return this;
  }

  /**
   * Handle a generic type with instances of its type parmaters.
   * @param type the type to describe.
   * @param descriptor the descriptor providing rerquired information on the raw type and type paramater instances. 
   * @return a formatted string.
   */
  String handleType(final String type, final Descriptor descriptor) {
    final String rawType = (String) descriptor.getFieldValue(MBeanInfoExplorer.RAW_TYPE_FIELD);
    if ((rawType == null) || rawType.isEmpty()) return formatType(type);
    final StringBuilder sb = new StringBuilder(formatType(rawType));
    final String[] typeParams = (String[]) descriptor.getFieldValue(MBeanInfoExplorer.RAW_TYPE_PARAMS_FIELD);
    if ((typeParams != null) && (typeParams.length > 0)) {
      sb.append(lt);
      for (int i=0; i<typeParams.length; i++) {
        if (i > 0) sb.append(", ");
        sb.append(formatType(typeParams[i]));
      }
      sb.append(gt);
    }
    return sb.toString();
  }

  /**
   * COnvert the specified string into a url pointing to either a JPPF ajavadoc page or a J2SE jaavdoc page, or a primitive type name.
   * @param type the type to convert.
   * @return the converted type.
   */
  String formatType(final String type) {
    String result =  typeCache.get(type);
    if (result != null) return result;
    if (type.startsWith("[")) result = formatArrayType(type);
    else if (type.startsWith("L") || type.contains(".")) result = formatObjectType(type);
    //else if (type.contains("<")) result = formatGenericType(type);
    else result = type;
    typeCache.put(type, result);
    return result;
  }

  /**
   * Convert the specified string into a javadoc URL.
   * @param type the type to convert.
   * @return the converted type.
   */
  abstract String formatObjectType(final String type);

  /**
   * Convert the specified string into a java-like syntax representing an array type.
   * @param type the type to convert.
   * @return the converted type.
   */
  String formatArrayType(final String type) {
    int count = 0;
    while (type.charAt(count) == '[') count++;
    final String compSig = type.substring(count);
    final String compName = compSig.startsWith("L") ? formatObjectType(compSig): compSig;
    final StringBuilder sb = new StringBuilder();
    for (int i=0; i<count; i++) sb.append(arraySuffix);
    return compName + sb.toString();
  }

  /**
   * Convert the specified string into a java-like syntax representing a generic type.
   * @param type the type to convert.
   * @return the converted type.
   */
  String formatGenericType(final String type) {
    final int idx = type.indexOf("<");
    return (idx < 0) ? type : type.substring(0, idx);
  }

  /**
   * Determine whether the specified string is either {@code null} or empty.
   * @param s the string to check.
   * @return {@code true} if the string is empty, {@code false} otherwise.
   */
  static boolean isEmpty(final String s) {
    return (s == null) || s.isEmpty();
  }
}
