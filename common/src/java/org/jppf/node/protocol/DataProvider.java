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
package org.jppf.node.protocol;

import org.jppf.utils.collections.Metadata;

/**
 * Instances of this class provide a way for tasks to share common data.
 * The objective is to avoid data duplication through marshaling/unmarshaling of the data,
 * which can cause crashes due to insufficient available memory in a node.
 * @author Laurent Cohen
 */
public interface DataProvider extends Metadata
{
}
