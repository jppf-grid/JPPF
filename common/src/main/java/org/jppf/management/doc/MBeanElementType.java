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

import java.lang.annotation.*;

import javax.management.DescriptorKey;

/**
 * This annotation describes MBean method return types or parameters types, when the types are instances of generic types.
 * For instance, a method whch returns {@code List<String>} would be annotated with {@code @MBeanElement(type = List.class, parameters = { "java.lang.String" })}.
 * @author Laurent Cohen
 */
//@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MBeanElementType {
  /**
   * @return the description for the mbean or mbean element.
   */
  @DescriptorKey(MBeanInfoExplorer.RAW_TYPE_FIELD)
  Class<?> type();
  /**
   * @return the description for the mbean or mbean element.
   */
  @DescriptorKey(MBeanInfoExplorer.RAW_TYPE_PARAMS_FIELD)
  String[] parameters() default {};
}
