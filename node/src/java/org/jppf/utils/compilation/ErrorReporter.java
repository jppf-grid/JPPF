/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.utils.compilation;

import java.util.*;

import javax.tools.*;

/**
 * A simple <code>DiagnosticListener</code> which exposes the collected <code>Diagnostic</code>s via a <code>List</code>
 * and allows resetting its state.
 * @param <S> the type of source object on which the diagnostics are reported.
 * @author Laurent Cohen
 */
class ErrorReporter<S> implements DiagnosticListener<S>
{
  /**
   * A list of diagnostics.
   */
  private List<Diagnostic> diagnosticList = new ArrayList<Diagnostic>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void report(final Diagnostic<? extends S> diagnostic)
  {
    diagnosticList.add(diagnostic);
  }

  /**
   * Get the diagnostics gathered by this <code>ErrorReporter</code>.
   * @return  a list of {@link Diagnostic} objects.
   */
  public List<Diagnostic> getDiagnostics()
  {
    return new ArrayList<Diagnostic>(diagnosticList);
  }

  /**
   * Clear the diagnostics previously gathered by this <code>ErrorReporter</code>.
   */
  public void clear()
  {
    diagnosticList.clear();
  }
}
