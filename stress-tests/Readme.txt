------------------------------------------------------------------------
JPPF
Copyright (C) 2005-2013 JPPF Team. 
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
------------------------------------------------------------------------


Template files:
--------------

These are intended to be copied for each JPPF component, after substitution of certain property values marked as groovy expressions.
Property values starting with 'expr:' are considered groovy expressions.
The expression can make use of predefined variables:
- $n is the number of the driver or node, it defaults to 1 for the client
- $templates_dir is the path to the directory where the default templates are located (see below)
- $scenario_dir is the path to the scenario directory (see below)

Examples:
- log4j.appender.JPPF.File= expr: $templates_dir + '/driver-' + $n + '.log' ==> log4j.appender.JPPF.File=scenarios/templates/driver-1.log (for driver-1)
- log4j.appender.JPPF.File= expr: $scenario_dir  + '/driver-' + $n + '.log' ==> log4j.appender.JPPF.File=scenarios/s1/driver-1.log (for driver-1, scenario dir in scenarios/s1)
- jppf.server.port = expr: 11100 + $n ==> jppf.server.port = 11102 (for driver-2: 11000 + 2)
- jppf.server.port = expr: 11100 + ($n % 2 == 0 ? 1 : 2) ==> jppf.server.port = 11101 for all nodes with an even number
                                                         ==> jppf.server.port = 11102 for all nodes with an odd number

Default templates:
-----------------

The default templates directory is located in './scenarios/templates'.
It contains JPPF, Log4j and JDK configuration files for driver, node and client.
Each template is processed as follows:
- load the file
- substitute each expression 'expr: ...' with the computed value
- save the file in a temporary location. The file will be deleted when the JVM exits.


Default template override:
-------------------------

Each scenario directory can have its own template files, in which case they will be used instead of the default templates.
A template override file is exactly as a default template (see above section), except that it is local to a specific scenario.
Template override files can also use groovy expressions as for the default templates, including the predefined variables.


Configuration file override:
---------------------------

It is possible to override the configuration file of specific drivers or nodes, by creating a specifically named file in the scenario folder.
This configuration file will not just replace the corresponding template file instance.
Instead, the values in this configuration file will be added to those in the template, possibly overriding them.

The files must be named in the format '{<type_of_file>-}<type_of_jppf_component>{-<n>}.properties' where
- <type_of_file> can be either 'log4', 'logging' or empty for a jppf configuration file
- <type_of_jppf_component> is either 'driver', 'node' or 'client'
- <n> is an integer >= 1

Examples:
- 'client.properties' provides override values for the client JPPF configuration file
- 'driver-2.properties' provides override values for the driver number 2's JPPF configuration file
- 'log4j-node-3.properties' provides override values for the node number 3's log4j configuration file

Configuration override files can also use groovy expressions as for the default templates, including the predefined variables.


Scenarios:
---------

A scenario is made of two entities:
- a folder, which contains the scenario configuration file, and eventual template override files and/or configuration override files (see above).
- some code, which implements the behavior of the scenario, including the jobs to submit, and other actions such as programatic
  JMX-based management or monitoring. The access point for this code is an implementation of the interface "org.jppf.test.scenario.ScenarioRunner".
  It may also extend the abtsract class "org.jppf.test.scenario.AbstractScenarioRunner".


Scenario configuration file:
---------------------------

This file must be at the root of the scenaruio folder and must be named "scenario.properties".
It contains a number of predefined properties, as well as any number of custom properties that the ScenarioRunner can use.

The predefined properties are the following:
- jppf.scenario.name: name given to this scenario
- jppf.scenario.description: description for this scenario, can be multi-lines using the \ continuation character
- jppf.scenario.iterations = the number of times this scenario is executed, must be > 0, defaults to 1
- jppf.scenario.nbNodes: number of nodes to start, must be >= 0, defaults to 1
- jppf.scenario.nbDrivers: number of drivers to start, must be >= 0, defaults to 1
- jppf.scenario.runner.class: fully qualified name of a class implementing org.jppf.test.scenario.ScenarioRunner
- jppf.scenario.diagnostics.file = the output file where the diagnostics for all nodes and drivers are written.
  value can be "none", "out", "err" or any valid file path. Default is "out" (printed to the console as with System.out)
  "none" means that diagnostics are not fetched or printed

The values for these properties can be defined as Groovy expressions, which can use the variable $scenario_dir

The predefined values are accessible directly via an API, provided by the interface "org.jppf.test.scenario.ScenarioConfiguration".
The custom values can be obtained from the TypedProperties object resulting from ScenarioConfiguration.getProperties().

