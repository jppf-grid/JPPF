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
 * Used to annotate MBeans and MBean elements to specify whther they should be included in the reference documentation.
 * @author Laurent Cohen
 */
//@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MBeanExclude {
  /**
   * @return the exlucde form doc flag.
   */
  @DescriptorKey(MBeanInfoExplorer.EXCLUDE_FIELD)
  boolean value() default true;
}
