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

/**
 * Classes supporting the persistence of the state of a load-balancer.
 * <p>This is particularly useful for adaptive load-balancers, as it allows
 * them to be immediately in an optimal efficiency state and  skip (most of) the convergence phase.
 * <p>Interfaces and classes in this package are common to server-side and client-side load-balancing.
 * In particular, identical mechanisms are used for the configuration, implementation and management of the load-balancers persistence.
 * <p>Found in: <b>jppf-common.jar</b> 
 */
package org.jppf.load.balancer.persistence;
