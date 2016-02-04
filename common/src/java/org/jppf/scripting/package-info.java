/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

/**
 * Support for embedded scripting within JPPF.
 * <p>Discovery of available script languages is made via SPI:
 * <ul>
 * <li>each language provider must be cdecalred in a file named {@code META-INF/services/org.jppf.scripting.ScriptRunner},
 * where each line contains the fully qualified name of a class implementing {@link org.jppf.scripting.ScriptRunner ScriptRunner} with a no-args constructor</li>
 * <li>the discovered script runners are grouped by language and can be accessed via the {@link org.jppf.scripting.ScriptRunnerFactory ScriptRunnerFactory} static methods</li>
 * </ul>  
 * @exclude
 */
package org.jppf.scripting;
