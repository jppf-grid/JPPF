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
 * Used to annotate MBean elements with a meaningful descritpion that can be retrieved at runtime.
 * @author Laurent Cohen
 */
//@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MBeanParamName {
  /**
   * @return the name of a parameter in an mbean method or constructor.
   */
  @DescriptorKey(MBeanInfoExplorer.PARAM_NAME_FIELD)
  String value();
}
