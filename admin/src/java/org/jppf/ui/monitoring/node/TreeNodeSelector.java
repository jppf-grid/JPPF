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

package org.jppf.ui.monitoring.node;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Selector interface that accepts or rejects a tree node.
 * @author Laurent Cohen
 */
public interface TreeNodeSelector
{
	/**
	 * Filter the specified node.
	 * @param node the node to filter.
	 * @return <code>true</code> if the node is accepted, <code>false</code> if it is rejected.
	 */
	boolean accept(DefaultMutableTreeNode node);
}
