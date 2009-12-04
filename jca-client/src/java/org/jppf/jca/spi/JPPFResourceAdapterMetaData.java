/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.jca.spi;

import javax.resource.cci.ResourceAdapterMetaData;

/**
 * Implementation of the {@link javax.resource.spi.ResourceAdapterMetaData ResourceAdapterMetaData} interface for
 * the JPPF resource adapter.
 * @author Laurent Cohen
 */
public class JPPFResourceAdapterMetaData implements ResourceAdapterMetaData
{
	/**
	 * Get the name of this adapter.
	 * @return the name as a string.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getAdapterName()
	 */
	public String getAdapterName()
	{
		return "JPPF Adapter";
	}

	/**
	 * Get a short description of this adapter.
	 * @return the short description as a string.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getAdapterShortDescription()
	 */
	public String getAdapterShortDescription()
	{
		return "JPPF Adapter";
	}

	/**
	 * Get the vendor name of this adapter.
	 * @return the vendor name as a string.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getAdapterVendorName()
	 */
	public String getAdapterVendorName()
	{
		return "JPPF.org";
	}

	/**
	 * Get the version of this adapter.
	 * @return the version as a string.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getAdapterVersion()
	 */
	public String getAdapterVersion()
	{
		return "1.0";
	}

	/**
	 * Get a list of supported interactions.
	 * @return the list as a array of strings.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getInteractionSpecsSupported()
	 */
	public String[] getInteractionSpecsSupported()
	{
		return new String[] { "ExampleInteraction" };
	}

	/**
	 * Get the version of the specs of this adapter.
	 * @return the version of the specs as a string.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getSpecVersion()
	 */
	public String getSpecVersion()
	{
		return "1.0";
	}

	/**
	 * Determine whether input and output record interactions are supported.
	 * @return false
	 * @see javax.resource.cci.ResourceAdapterMetaData#supportsExecuteWithInputAndOutputRecord()
	 */
	public boolean supportsExecuteWithInputAndOutputRecord()
	{
		return false;
	}

	/**
	 * Determine whether input record only interactions are supported.
	 * @return false
	 * @see javax.resource.cci.ResourceAdapterMetaData#supportsExecuteWithInputRecordOnly()
	 */
	public boolean supportsExecuteWithInputRecordOnly()
	{
		return false;
	}

	/**
	 * Determine whether local transactions are supported.
	 * @return false.
	 * @see javax.resource.cci.ResourceAdapterMetaData#supportsLocalTransactionDemarcation()
	 */
	public boolean supportsLocalTransactionDemarcation()
	{
		return false;
	}
}
