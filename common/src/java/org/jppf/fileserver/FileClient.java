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

package org.jppf.fileserver;


/**
 * Interface for client side connections.
 * This interface only specifies the most basic operations of the file server client.
 * @author Laurent Cohen
 */
public interface FileClient extends Configurable
{
	/**
	 * Open this file client.
	 * @throws Exception if any error occurs.
	 */
	void open() throws Exception;
	/**
	 * CLose this file client.
	 * @throws Exception if any error occurs.
	 */
	void close() throws Exception;
	/**
	 * Download a remote file to a local location. 
	 * @param localPath the path to the resulting local file.
	 * @param remotePath the path of the remote file.
	 * @throws Exception if any error occurs during the file transfer.
	 */
	void download(String localPath, String remotePath) throws Exception;

	/**
	 * Upload a local file to a remote location. 
	 * @param localPath the path to the file to upload.
	 * @param remotePath the path of the resulting remote file.
	 * @throws Exception if any error occurs during the file transfer.
	 */
	void upload(String localPath, String remotePath) throws Exception;
}
