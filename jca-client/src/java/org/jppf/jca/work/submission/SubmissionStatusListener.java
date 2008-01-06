/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.jca.work.submission;

import java.util.EventListener;

/**
 * Listener interface for receiving submission status change notifications.
 * @author Laurent Cohen
 */
public interface SubmissionStatusListener extends EventListener
{
	/**
	 * Called when the status of a submission has changed.
	 * @param event the event encapsulating the change of status.
	 */
	void submissionStatusChanged(SubmissionStatusEvent event);
}
