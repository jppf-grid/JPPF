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

package org.jppf.management;

import java.lang.annotation.*;

import javax.management.DescriptorKey;

/**
 * Used to annotate MBean elements with a meaningful descritpion that can be retrieved at runtime.
 * @author Laurent Cohen
 */
//@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JPPFMBeanDescription {
  /**
   * @return the description for the mbean or mbean element.
   */
  @DescriptorKey("description")
  String value();

  /**
   * @return the base resource bundle name for localization.
  @DescriptorKey("i18n.base")
  String i18nBase() default "";
   */

  /**
   * @return the key in the base resource bundle for localization.
  @DescriptorKey("i18n.key")
  String i18nKey() default "";
   */
}
