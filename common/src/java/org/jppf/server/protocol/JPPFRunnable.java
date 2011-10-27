/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.protocol;

import java.lang.annotation.*;

/**
 * Annotation to determine which method in a class is the task's main method.
 * @author Laurent Cohen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.CONSTRUCTOR } )
public @interface JPPFRunnable
{
	/**
	 * Specifies the execution order, in the case where multiple methods are annotated in the same class.<br>
	 * When specified orders are the same, the ordering is the same as the one used in {@link java.lang.Class#getDeclaredMethods() Class.getDeclaredMethods()}
	 */
	int value() default 0;
}
