/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.client.taskwrapper;

import static org.jppf.client.taskwrapper.TaskObjectWrapper.MethodType.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.security.AccessController;

import org.jppf.JPPFException;
import org.jppf.node.protocol.JPPFRunnable;

/**
 * Wrapper class for a task not implementing {@link org.jppf.node.protocol.Task Task<T>}.
 * @author Laurent Cohen
 * @exclude
 */
class AnnotatedTaskWrapper extends AbstractTaskObjectWrapper {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The runnable object to execute.
   */
  private Object taskObject = null;
  /**
   * The arguments of the method to execute.
   */
  private Object[] args = null;
  /**
   * The name of the object's class if the method is static or a constructor.
   */
  private String className = null;

  /**
   * Initialize this wrapper with the specified task object and arguments.
   * @param taskObject the runnable object to execute.
   * @param args the arguments of the method to execute.
   * @throws JPPFException if an error is raised while initializing this task wrapper.
   */
  public AnnotatedTaskWrapper(final Object taskObject, final Object... args) throws JPPFException {
    final boolean isClass = taskObject instanceof Class;
    final Class<?> clazz = isClass ? (Class<?>) taskObject : taskObject.getClass();
    final AnnotatedElement elt = getJPPFAnnotatedElement(clazz);
    if (elt == null) throw new JPPFException("object '" + taskObject + "' is not a JPPFTask nor JPPF-annotated");
    if (elt instanceof Method) {
      if (isClass) {
        methodType = STATIC;
        className = clazz.getName();
      } else {
        methodType = INSTANCE;
        this.taskObject = taskObject;
      }
    } else {
      methodType = CONSTRUCTOR;
      className = clazz.getName();
    }
    this.args = args;
  }

  /**
   * Execute the run method of this runnable task.
   * @return the result of the execution.
   * @throws Exception if an error occurs during the execution.
   * @see org.jppf.client.taskwrapper.TaskObjectWrapper#execute()
   */
  @Override
  public Object execute() throws Exception {
    final Class<?> clazz = (INSTANCE== methodType) ? taskObject.getClass() : getTaskobjectClass(className);
    Object result = null;
    AbstractPrivilegedAction<?> action = null;
    switch (methodType) {
      case CONSTRUCTOR:
        final Constructor<?> c = (Constructor<?>) getJPPFAnnotatedElement(clazz);
        action = new PrivilegedConstructorAction(c, args);
        break;

      case INSTANCE:
      case STATIC:
      default:
        final Method m = (Method) getJPPFAnnotatedElement(clazz);
        action = new PrivilegedMethodAction(m, taskObject, args);
        break;
    }
    result = AccessController.doPrivileged(action);
    if (methodType == CONSTRUCTOR) taskObject = result;
    if (action.getException() != null) throw action.getException();
    return result;
  }

  /**
   * Return the object on which a method or constructor is called.
   * @return an object or null if the invoked method is static.
   * @see org.jppf.client.taskwrapper.TaskObjectWrapper#getTaskObject()
   */
  @Override
  public Object getTaskObject() {
    return taskObject;
  }

  /**
   * Determines whether a class has a JPPF-annotated method and can be executed as a task.
   * @param clazz the class to check.
   * @return true if the class can be executed as a task, false otherwise.
   */
  private static AnnotatedElement getJPPFAnnotatedElement(final Class<?> clazz) {
    if (clazz == null) return null;
    for (Method m : clazz.getDeclaredMethods()) {
      if (isJPPFAnnotated(m)) return m;
    }
    for (Constructor<?> c : clazz.getDeclaredConstructors()) {
      if (isJPPFAnnotated(c)) return c;
    }
    return null;
  }

  /**
   * Determines whether a method is JPPF-annotated and can be executed as a task.
   * @param annotatedElement the method to check.
   * @return true if the method can be executed as a task, false otherwise.
   */
  private static boolean isJPPFAnnotated(final AnnotatedElement annotatedElement) {
    if (annotatedElement == null) return false;
    final Annotation[] annotations = annotatedElement.getAnnotations();
    for (Annotation a : annotations) {
      if (JPPFRunnable.class.equals(a.annotationType())) return true;
    }
    return false;
  }
}
