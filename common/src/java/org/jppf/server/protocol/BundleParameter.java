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

package org.jppf.server.protocol;

/**
 * Constants used when a client sends an admin command to a server.
 * @author Laurent Cohen
 */
public enum  BundleParameter
{
	/**
	 * Admin command for scheduled shutdown of the server.
	 */
	SHUTDOWN,
	/**
	 * Admin command for scheduled shutdown and restart of the server.
	 */
	SHUTDOWN_RESTART,
	/**
	 * Admin command for scheduled shutdown and restart of the server.
	 */
	CHANGE_PASSWORD,
	/**
	 * Admin command for setting the size of the task bundles used by the server and nodes.
	 */
	CHANGE_SETTINGS,
	/**
	 * Admin command for getting the bundle size settings from the server.
	 */
	REFRESH_SETTINGS,
	/**
	 * Admin command for getting the latest statistics fropm the server.
	 */
	READ_STATISTICS,
	/**
	 * Parameter name for the administration command to perform.
	 */
	COMMAND_PARAM,
	/**
	 * Parameter name for the key, in encrypted format, used to decrypt the password.
	 */
	KEY_PARAM,
	/**
	 * Parameter name for the administration password in encrypted format.
	 */
	PASSWORD_PARAM,
	/**
	 * Parameter name for the new administration password in encrypted format, for password change.
	 */
	NEW_PASSWORD_PARAM,
	/**
	 * Parameter name for the delay before shutting down the server.
	 */
	SHUTDOWN_DELAY_PARAM,
	/**
	 * Parameter name for the delay before restarting the server.
	 */
	RESTART_DELAY_PARAM,
	/**
	 * Parameter name for the response message to this request.
	 */
	RESPONSE_PARAM,
	/**
	 * Parameter name for the size of the task bundles used by the server and nodes.
	 */
	BUNDLE_SIZE_PARAM,
	/**
	 * Parameter to determine whether the tasks bundle size is determined manually or automatically.
	 */
	BUNDLE_TUNING_TYPE_PARAM,
	/**
	 * Autotuning parameter: minimum number of samples to analyse
	 */
	MIN_SAMPLES_TO_ANALYSE,
	/**
	 * Autotuning parameter: minimum number of samples to check algorithm convergence
	 */
	MIN_SAMPLES_TO_CHECK_CONVERGENCE,
	/**
	 * Autotuning parameter: maximum allowed deviation.
	 */
	MAX_DEVIATION,
	/**
	 * Autotuning parameter: maximum number of guesses before best size is deemed stable.
	 */
	MAX_GUESS_TO_STABLE,
	/**
	 * Autotuning parameter: size ration deviation.
	 */
	SIZE_RATIO_DEVIATION,
	/**
	 * Autotuning parameter: decrease ratio.
	 */
	DECREASE_RATIO,
	/**
	 * To determine whether a node connection is for a peer driver or an actual execution node.
	 */
	IS_PEER
}
