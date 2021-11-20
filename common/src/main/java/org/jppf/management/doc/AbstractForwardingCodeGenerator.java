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

import java.io.*;
import java.util.*;

import javax.management.*;

import org.jppf.utils.*;

/**
 * Abstract super class for classes that Generate source code for forwarding to node MBeans.
 * @param <E> .
 * @author Laurent Cohen
 */
public abstract class AbstractForwardingCodeGenerator<E> extends AbstractMBeanInfoWriter<AbstractForwardingCodeGenerator<E>> {
  /**
   * Suffix appended to the MBean interface name to compute the name of the generated class.
   */
  final static Map<String, String> wrapperTypes = new HashMap<String, String>() {{
    put("void", "Void");
    put("boolean", "Boolean");
    put("byte", "Byte");
    put("short", "Short");
    put("int", "Integer");
    put("long", "Long");
    put("float", "Float");
    put("double", "Double");
  }};
  /**
   * Suffix appended to the MBean interface name to compute the name of the generated class.
   */
  String classNamePrefix = "";
  /**
   * Suffix appended to the MBean interface name to compute the name of the generated class.
   */
  String classNameSuffix = "Forwarder";
  /**
   * The directory where the generated files are crreated.
   */
  final String destinationDir;
  /**
   * Name of the package of the genrated clases.
   */
  final String packageName;
  /**
   * Holds the generated file header.
   */
  final String fileHader;
  /**
   * Set of types to import in the generated code.
   */
  final Set<String> importedTypes = new TreeSet<>();
  /**
   * Set of types to statically import in the generated code.
   */
  final Set<String> staticImportedTypes = new TreeSet<>();
  /**
   * The MBean forwarding proxy's class name.
   */
  String className;
  /**
   * The number of attributes and operations in the MBean.
   */
  int nbAttributes, nbOperations;
  /**
   * The MBeans notifications information, if any.
   */
  NotificationInfo notifInfo; 
  /**
   * The MBeans information, if any.
   */
  InterfaceInfo interfaceInfo; 

  /**
   * Initialize this visitor.
   * @param packageName the directory where the genrated source files are created.
   * @param sourceRootDir the root directory of where the sources are located.
   */
  public AbstractForwardingCodeGenerator(final String packageName, final String sourceRootDir) {
    super(new StringWriter());
    this.arraySuffix = "[]";
    this.packageName = packageName;
    this.destinationDir = sourceRootDir + "/" + packageName.replace(".", "/");
    fileHader = initFileHeader();
  }

  @Override
  public void startMBean(final ObjectName mbeanName, final MBeanInfo mbean) throws Exception {
    importedTypes.clear();
    typeCache.clear();
    interfaceInfo = new InterfaceInfo(mbean);
    notifInfo = new NotificationInfo(mbean);
    className = interfaceInfo.clsName;
    nbAttributes = 0;
    final String[] filteredAttributes = { "NotificationInfo" };
    for (final MBeanAttributeInfo attribute: mbean.getAttributes()) {
      if (!StringUtils.isOneOf(attribute.getName(), false, filteredAttributes)) nbAttributes++;
    }
    nbOperations = 0;
    final String[] filteredoperations = { "addNotificationListener", "removeNotificationListener", "sendNotification" };
    for (final MBeanOperationInfo operation: mbean.getOperations()) {
      if (!StringUtils.isOneOf(operation.getName(), false, filteredoperations)) nbOperations++;
    }
    System.out.println("processing " + interfaceInfo.interfaceName + ", nbAttributes =  " + nbAttributes + ", nboperations =  " + nbOperations);
    startMBean(interfaceInfo, mbeanName, mbean);
  }

  /**
   * Start the visit for the specified mbean.
   * @param info compute info on the mbean interface.
   * @param mbeanName the object name of the mbea,.
   * @param mbean full information on the mbena.
   * @throws Exception if any error occurs.
   */
  abstract void startMBean(final InterfaceInfo info, final ObjectName mbeanName, final MBeanInfo mbean) throws Exception;

  @Override
  public void endMBean(final ObjectName name, final MBeanInfo info) throws Exception {
    println("}");
    final StringBuilder sb = new StringBuilder(fileHader);
    sb.append("package ").append(packageName).append(";\n\n");
    finalizeImports(name, info);
    if (!staticImportedTypes.isEmpty()) {
      for (final String type: staticImportedTypes) {
        final int idx = type.lastIndexOf('.');
        if ((idx >= 0) && "java.lang".equals(type.substring(0, idx))) continue;
        sb.append("import static ").append(type).append(".*;\n");
      }
      sb.append('\n');
    }
    for (final String type: importedTypes) {
      final int idx = type.lastIndexOf('.');
      if ((idx >= 0) && "java.lang".equals(type.substring(0, idx))) continue;
      sb.append("import ").append(type).append(";\n");
    }
    sb.append('\n');
    sb.append(writer.toString());
    writer.close();
    writer = new StringWriter();
    FileUtils.writeTextFile(destinationDir + "/" + className + ".java", sb.toString());
  }

  /**
   * Generate the imports for the generated class.
   * @param name the object name of the mbean,
   * @param info full information on the mbena.
   * @throws Exception if any error occurs.
   */
  abstract void finalizeImports(final ObjectName name, final MBeanInfo info) throws Exception;

  @Override
  public void visitAttribute(final MBeanAttributeInfo attribute) throws Exception {
    visitAttribute(new AttributeInfo(attribute), attribute);
  }

  /**
   * Visit the specified mbean attribute.
   * @param info computed info on the attribute.
   * @param attribute info on the mbean attribute
   * @throws Exception if any error occurs.
   */
  abstract void visitAttribute(final AttributeInfo info, final MBeanAttributeInfo attribute) throws Exception;

  @Override
  public void visitOperation(final MBeanOperationInfo operation) throws Exception {
    visitOperation(new OperationInfo(operation), operation);
  }

  /**
   * Visit the specified mbean operation.
   * @param info pre-computed information on the operation.
   * @param operation info on the mbean operation.
   * @throws Exception if any error occurs.
   */
  abstract void visitOperation(final OperationInfo info, final MBeanOperationInfo operation) throws Exception;

  @Override
  String formatObjectType(final String type) {
    final String name = type.startsWith("L") ? type.substring(1, type.length() - 1) : type;
    importedTypes.add(formatGenericType(name));
    return name.substring(name.lastIndexOf('.') + 1);
  }

  /**
   * Compute the file header.
   * @return a string with the stanadard JPPF source file header.
   */
  static String initFileHeader() {
    final String dir = "../common/src/java/" + AbstractForwardingCodeGenerator.class.getPackage().getName().replace(".", "/");
    try (Reader reader = new BufferedReader(new FileReader(dir + "/JavaSourceHeader.txt"))) {
      final String fileHeader = FileUtils.readTextFile(reader);
      final int year = Calendar.getInstance().get(Calendar.YEAR);
      return fileHeader.replace("@current_year@", Integer.toString(year));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the wrapper tyoe, if any.
   * @param type the type for whuch to lookup.
   * @return the wrapper type, or the input type.
   */
  static String wrapperType(final String type) {
    final String result = wrapperTypes.get(type);
    return result == null ? type : result;
  }

  /**
   * Add a dot tot he end of the specified string if needed.
   * @param source the string to dot.
   * @return the string with a dot at the end.
   */
  static String dottedEnd(final String source) {
    return ((source == null) || source.endsWith(".")) ? source : source + ".";
  }

  /**
   * Capitalize the first character of the specified string.
   * @param source the string to transform.
   * @return the transfrmed string.
   */
  static String capitalizeFirstChar(final String source) {
    return (source == null) ? null : "" + Character.toUpperCase(source.charAt(0)) + source.substring(1);
  }

  /** */
  class OperationInfo {
    /** */
    final Descriptor descriptor;
    /** */
    final MBeanParameterInfo[] params;
    /** */
    final String desc, returnType, wrappedReturnType;
    /** */
    final String[] paramNames, paramTypes, genericTypes;

    /**
     * @param operation the {@link MBeanOperationInfo}.
     * @throws Exception if any error occurs.
     */
    OperationInfo(final MBeanOperationInfo operation) throws Exception {
      descriptor = operation.getDescriptor();
      desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
      params = operation.getSignature();
      paramNames = new String[params.length];
      paramTypes = new String[params.length];
      genericTypes = new String[params.length];
      int count = 0;
      for (final MBeanParameterInfo param: params) {
        paramTypes[count] = formatType(param.getType());
        genericTypes[count] = handleType(param.getType(), param.getDescriptor());
        final String name = (String) param.getDescriptor().getFieldValue(MBeanInfoExplorer.PARAM_NAME_FIELD);
        paramNames[count] = (name != null) ? name : "p" + (count + 1);
        count++;
      }
      returnType = handleType(operation.getReturnType(), descriptor);
      wrappedReturnType = wrapperType(returnType);
    }
  }

  /** */
  class AttributeInfo {
    /** */
    final Descriptor descriptor;
    /** */
    final String desc, type, wrappedType;
    /** */
    final boolean readable, writable;

    /**
     * @param attribute the {@link MBeanAttributeInfo}.
     * @throws Exception if any error occurs.
     */
    AttributeInfo(final MBeanAttributeInfo attribute) throws Exception {
      descriptor = attribute.getDescriptor();
      desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
      type = handleType(attribute.getType(), descriptor);
      wrappedType = wrapperType(type);
      readable = attribute.isReadable();
      writable = attribute.isWritable();
    }
  }

  /** */
  class InterfaceInfo {
    /** */
    final Descriptor descriptor;
    /** */
    final String desc, interfaceName, interfaceSimpleName, clsName;

    /**
     * @param mbean the {@link MBeanInfo}.
     * @throws Exception if any error occurs.
     */
    InterfaceInfo(final MBeanInfo mbean) throws Exception {
      descriptor = mbean.getDescriptor();
      desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
      interfaceName = (String) descriptor.getFieldValue("interfaceClassName");
      interfaceSimpleName = formatType(interfaceName);
      clsName = classNamePrefix + interfaceSimpleName + classNameSuffix;
    }
  }

  /** */
  class NotificationInfo {
    /** */
    final String desc, userDataDesc, userDataType;

    /**
     * @param mbean the {@link MBeanInfo}.
     * @throws Exception if any error occurs.
     */
    NotificationInfo(final MBeanInfo mbean) throws Exception {
      final Descriptor descriptor = mbean.getDescriptor();
      desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_DESCRIPTION_FIELD);
      userDataDesc = (desc != null) ? (String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_USER_DATA_DESCRIPTION_FIELD) : null;
      userDataType = (desc != null) ? (String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_USER_DATA_CLASS_FIELD) : null;
    }
  }
}
