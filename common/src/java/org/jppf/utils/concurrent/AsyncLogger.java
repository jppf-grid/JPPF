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

package org.jppf.utils.concurrent;

import org.slf4j.*;

/**
 * An asynchronous logger that delegates to an underlying SLF4J logger asychronously. This is to be used for debugging purposes,
 * essentially when activating fine-grained logging prevents from reproducing an issue (happens a lot in case of race conditions).
 * @exclude
 */
public class AsyncLogger implements Logger {
  /**
   * The thread pool used to perform asynchronous operations.
   */
  private static JPPFThreadPool executor = new JPPFThreadPool(1, 1, 5000L, new JPPFThreadFactory("AsyncLogger"));
  /**
   * The logger to delegate to.
   */
  private final Logger delegate;

  /**
   * @param delegate the logger to delegate to.
   */
  public AsyncLogger(final Logger delegate) {
    this.delegate = delegate;
  }

  /**
   * @param loggerClass the class of the logger to delegate to.
   */
  public AsyncLogger(final Class<?> loggerClass) {
    this.delegate = LoggerFactory.getLogger(loggerClass);
  }

  @Override
  public void debug(final Marker arg0, final String arg1, final Object arg2, final Object arg3) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0, arg1, arg2, arg3);
      }
    });
  }

  @Override
  public void debug(final Marker arg0, final String arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void debug(final Marker arg0, final String arg1, final Object...arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void debug(final Marker arg0, final String arg1, final Throwable arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void debug(final Marker arg0, final String arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0, arg1);
      }
    });
  }

  @Override
  public void debug(final String arg0, final Object arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void debug(final String arg0, final Object arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0, arg1);
      }
    });
  }

  @Override
  public void debug(final String arg0, final Object...arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0, arg1);
      }
    });
  }

  @Override
  public void debug(final String arg0, final Throwable arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0, arg1);
      }
    });
  }

  @Override
  public void debug(final String arg0) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.debug(arg0);
      }
    });
  }

  @Override
  public void error(final Marker arg0, final String arg1, final Object arg2, final Object arg3) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0, arg1, arg2, arg3);
      }
    });
  }

  @Override
  public void error(final Marker arg0, final String arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void error(final Marker arg0, final String arg1, final Object...arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void error(final Marker arg0, final String arg1, final Throwable arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void error(final Marker arg0, final String arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0, arg1);
      }
    });
  }

  @Override
  public void error(final String arg0, final Object arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void error(final String arg0, final Object arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0, arg1);
      }
    });
  }

  @Override
  public void error(final String arg0, final Object...arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0, arg1);
      }
    });
  }

  @Override
  public void error(final String arg0, final Throwable arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0, arg1);
      }
    });
  }

  @Override
  public void error(final String arg0) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.error(arg0);
      }
    });
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public void info(final Marker arg0, final String arg1, final Object arg2, final Object arg3) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0, arg1, arg2, arg3);
      }
    });
  }

  @Override
  public void info(final Marker arg0, final String arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void info(final Marker arg0, final String arg1, final Object...arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void info(final Marker arg0, final String arg1, final Throwable arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void info(final Marker arg0, final String arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0, arg1);
      }
    });
  }

  @Override
  public void info(final String arg0, final Object arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void info(final String arg0, final Object arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0, arg1);
      }
    });
  }

  @Override
  public void info(final String arg0, final Object...arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0, arg1);
      }
    });
  }

  @Override
  public void info(final String arg0, final Throwable arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0, arg1);
      }
    });
  }

  @Override
  public void info(final String arg0) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.info(arg0);
      }
    });
  }

  @Override
  public boolean isDebugEnabled() {
    return delegate.isDebugEnabled();
  }

  @Override
  public boolean isDebugEnabled(final Marker arg0) {
    return delegate.isDebugEnabled(arg0);
  }

  @Override
  public boolean isErrorEnabled() {
    return delegate.isErrorEnabled();
  }

  @Override
  public boolean isErrorEnabled(final Marker arg0) {
    return delegate.isErrorEnabled(arg0);
  }

  @Override
  public boolean isInfoEnabled() {
    return delegate.isInfoEnabled();
  }

  @Override
  public boolean isInfoEnabled(final Marker arg0) {
    return delegate.isInfoEnabled(arg0);
  }

  @Override
  public boolean isTraceEnabled() {
    return delegate.isTraceEnabled();
  }

  @Override
  public boolean isTraceEnabled(final Marker arg0) {
    return delegate.isTraceEnabled(arg0);
  }

  @Override
  public boolean isWarnEnabled() {
    return delegate.isWarnEnabled();
  }

  @Override
  public boolean isWarnEnabled(final Marker arg0) {
    return delegate.isWarnEnabled(arg0);
  }

  @Override
  public void trace(final Marker arg0, final String arg1, final Object arg2, final Object arg3) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0, arg1, arg2, arg3);
      }
    });
  }

  @Override
  public void trace(final Marker arg0, final String arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void trace(final Marker arg0, final String arg1, final Object...arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void trace(final Marker arg0, final String arg1, final Throwable arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void trace(final Marker arg0, final String arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0, arg1);
      }
    });
  }

  @Override
  public void trace(final String arg0, final Object arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void trace(final String arg0, final Object arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0, arg1);
      }
    });
  }

  @Override
  public void trace(final String arg0, final Object...arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0, arg1);
      }
    });
  }

  @Override
  public void trace(final String arg0, final Throwable arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0, arg1);
      }
    });
  }

  @Override
  public void trace(final String arg0) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.trace(arg0);
      }
    });
  }

  @Override
  public void warn(final Marker arg0, final String arg1, final Object arg2, final Object arg3) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0, arg1, arg2, arg3);
      }
    });
  }

  @Override
  public void warn(final Marker arg0, final String arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void warn(final Marker arg0, final String arg1, final Object...arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void warn(final Marker arg0, final String arg1, final Throwable arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void warn(final Marker arg0, final String arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0, arg1);
      }
    });
  }

  @Override
  public void warn(final String arg0, final Object arg1, final Object arg2) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0, arg1, arg2);
      }
    });
  }

  @Override
  public void warn(final String arg0, final Object arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0, arg1);
      }
    });
  }

  @Override
  public void warn(final String arg0, final Object...arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0, arg1);
      }
    });
  }

  @Override
  public void warn(final String arg0, final Throwable arg1) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0, arg1);
      }
    });
  }

  @Override
  public void warn(final String arg0) {
    executor.execute(new Runnable() {
      @Override public void run() {
        delegate.warn(arg0);
      }
    });
  }
}
