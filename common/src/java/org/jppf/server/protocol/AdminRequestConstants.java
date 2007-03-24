/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.jppf.server.protocol;

/**
 * Constants used when a client sends an admin command to a server.
 * @author Laurent Cohen
 */
public interface AdminRequestConstants
{
	/**
	 * Admin command for scheduled shutdown of the server.
	 */
	String SHUTDOWN = "shutdown";
	/**
	 * Admin command for scheduled shutdown and restart of the server.
	 */
	String SHUTDOWN_RESTART = "shutdown.restart";
	/**
	 * Admin command for scheduled shutdown and restart of the server.
	 */
	String CHANGE_PASSWORD = "change.pwd";
	/**
	 * Admin command for setting the size of the task bundles used by the server and nodes.
	 */
	String CHANGE_SETTINGS = "set.bundle.size";
	/**
	 * Parameter name for the administration command to perform.
	 */
	String COMMAND_PARAM = "command";
	/**
	 * Parameter name for the key, in encrypted format, used to decrypt the password.
	 */
	String KEY_PARAM = "key";
	/**
	 * Parameter name for the administration password in encrypted format.
	 */
	String PASSWORD_PARAM = "pwd";
	/**
	 * Parameter name for the new administration password in encrypted format, for password change.
	 */
	String NEW_PASSWORD_PARAM = "pwd.new";
	/**
	 * Parameter name for the delay before shutting down the server.
	 */
	String SHUTDOWN_DELAY_PARAM = "shutdown.delay";
	/**
	 * Parameter name for the delay before restarting the server.
	 */
	String RESTART_DELAY_PARAM = "restart.delay";
	/**
	 * Parameter name for the response message to this request.
	 */
	String RESPONSE_PARAM = "response";
	/**
	 * Parameter name for the size of the task bundles used by the server and nodes.
	 */
	String BUNDLE_SIZE_PARAM = "bundle.size";
	/**
	 * Parameter to determine whether the tasks bundle size is determined manually or automatically.
	 */
	String BUNDLE_TUNING_TYPE_PARAM = "bundle.tuning.type";
	/**
	 * Autotuning parameter: minimum number of samples to analyse
	 */
	String MIN_SAMPLES_TO_ANALYSE = "MinSamplesToAnalyse";
	/**
	 * Autotuning parameter: minimum number of samples to check algorithm convergence
	 */
	String MIN_SAMPLES_TO_CHECK_CONVERGENCE = "MinSamplesToCheckConvergence";
	/**
	 * Autotuning parameter: maximum allowed deviation.
	 */
	String MAX_DEVIATION = "MaxDeviation";
	/**
	 * Autotuning parameter: maximum number of guesses before best size is deemed stable.
	 */
	String MAX_GUESS_TO_STABLE = "MaxGuessToStable";
	/**
	 * Autotuning parameter: size ration deviation.
	 */
	String SIZE_RATIO_DEVIATION = "SizeRatioDeviation";
	/**
	 * Autotuning parameter: decrease ratio.
	 */
	String DECREASE_RATIO = "DecreaseRatio";
}
