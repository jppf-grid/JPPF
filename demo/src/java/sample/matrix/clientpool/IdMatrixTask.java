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

package sample.matrix.clientpool;

import sample.matrix.MatrixTask;

/**
 * 
 * @author Laurent Cohen
 */
public class IdMatrixTask extends MatrixTask
{
	/**
	 * The id of the submitter for this task.
	 */
	private String submitId = null;

	/**
	 * Initialize this task with a specified row of values to multiply.
	 * @param rowValues the values as an array of <code>double</code> values.
	 * @param submitId the id of the submitter for this task.
	 */
	public IdMatrixTask(double[] rowValues, String submitId)
	{
		super(rowValues);
		this.submitId = submitId;
	}

	/**
	 * Get the id of the submitter for this task.
	 * @return the id as a string.
	 */
	public String getSubmitId()
	{
		return submitId;
	}
}
