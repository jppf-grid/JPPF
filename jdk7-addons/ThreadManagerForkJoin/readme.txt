--------------------------------------------------------------------------------
  JPPF
  Copyright (C) 2005-2012 JPPF Team. 
  http://www.jppf.org

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--------------------------------------------------------------------------------

Readme file for the ForkJoinThreadManager add-on.

1. What is it?

This is a node add-on which replaces the standard node processing thread pool
with a fork/join thread pool such as defined at:
http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ForkJoinPool.html

This allows JPPF tasks to locally (in the node) spawn ForkJoinTask (or any of
its subclasses) instances and have them processed as expected for a ForkJoinPool

2. Deploying the jar file

The file "ThreadManagerForkJoin.jar" can be deployed either in the JPPF server's
or node's classpath. If deployed in the server's classpath, it will be available
to all nodes.

3. Configuring the nodes

For a node to use this add-on, add the following property to its configuration
file:

  jppf.thread.manager.class = org.jppf.server.node.fj.ThreadManagerForkJoin

4. Requirements

To use this add-on within a node, it is required to start the node with a JDK 7
or later JVM.