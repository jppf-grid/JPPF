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

import javax.management.*;

/**
 * Used to annotate MBean interfaces with a meaningful descritpion of the notifications they emit.
 * This description can then be retrieved at runtime.
 * @author Laurent Cohen
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MBeanNotif {
  /**
   * @return the description for the mbean notification.
   */
  @DescriptorKey(MBeanInfoExplorer.NOTIF_DESCRIPTION_FIELD)
  String description() default "";

  /**
   * @return the description for the mbean notification.
   */
  @DescriptorKey(MBeanInfoExplorer.NOTIF_CLASS_FIELD)
  Class<?> notifClass() default Notification.class;

  /**
   * @return the expected type of the notification's user dtaa.
   */
  @DescriptorKey(MBeanInfoExplorer.NOTIF_USER_DATA_CLASS_FIELD)
  Class<?> userDataType() default Object.class;

  /**
   * @return the description for the mbean notification's user data.
   */
  @DescriptorKey(MBeanInfoExplorer.NOTIF_USER_DATA_DESCRIPTION_FIELD)
  String userDataDescritpion() default "";
}
