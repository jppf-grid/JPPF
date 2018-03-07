#------------------------------------------------------------------------------#
# JPPF.                                                                        #
# Copyright (C) 2005-2018 JPPF Team.                                           #
# http://www.jppf.org                                                          #
#                                                                              #
# Licensed under the Apache License, Version 2.0 (the "License");              #
# you may not use this file except in compliance with the License.             #
# You may obtain a copy of the License at                                      #
#                                                                              #
#    http://www.apache.org/licenses/LICENSE-2.0                                #
#                                                                              #
# Unless required by applicable law or agreed to in writing, software          #
# distributed under the License is distributed on an "AS IS" BASIS,            #
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.     #
# See the License for the specific language governing permissions and          #
# limitations under the License.                                               #
#------------------------------------------------------------------------------#

JPPF JMX remote connector.

List of available environment properties:

jppf.jmxremote.request.timeout,                        Maximum time in milliseconds to wait for a JMX request to succeed, default to 15,000 ms

jppf.jmx.remote.tls.enabled,                           whether to use secure connections via TLS protocol, defaults to false 
jppf.jmx.remote.tls.context.protocol,                  javax.net.ssl.SSLContext protocol, defaults to TLSv1.2
jppf.jmx.remote.tls.enabled.protocols,                 A list of space-separated enabled protocols, defaults to TLSv1.2
jppf.jmx.remote.tls.enabled.cipher.suites,             Space-separated list of enabled cipher suites, defaults to SSLContext.getDefault().getDefaultSSLParameters().getCipherSuites()
jppf.jmx.remote.tls.client.authentication,             SSL client authentication level: one of 'none', 'want', 'need', defaults to 'none'
jppf.jmx.remote.tls.client.distinct.truststore,        Whether to use a separate trust store for client certificates (server only), defaults to 'false'
jppf.jmx.remote.tls.client.truststore.password,        Plain text client trust store password, defaults to null
jppf.jmx.remote.tls.client.truststore.password.source, Client trust store location as an arbitrary source, default to null
jppf.jmx.remote.tls.client.truststore.file,            Path to the client trust store in the file system or classpath, defaults to null
jppf.jmx.remote.tls.client.truststore.source,          Client trust store location as an arbitrary source, defaults to null
jppf.jmx.remote.tls.client.truststore.type,            Trust store format, defaults to 'jks'
jppf.jmx.remote.tls.truststore.password,               Plain text trust store password, defaults to null
jppf.jmx.remote.tls.truststore.password.source,        Trust store password as an arbitrary source, defaults to null
jppf.jmx.remote.tls.truststore.file,                   Path to the trust store in the file system or classpath, defaults to null
jppf.jmx.remote.tls.truststore.source,                 Trust store location as an arbitrary source, defaults to null
jppf.jmx.remote.tls.truststore.type,                   Trust store format, defaults to 'jks'
jppf.jmx.remote.tls.keystore.password,                 Plain text key store password, defaults to null
jppf.jmx.remote.tls.keystore.password.source,          Key store password as an arbitrary source, defaults to null
jppf.jmx.remote.tls.keystore.file,                     Path to the key store in the file system or classpath, defaults to null
jppf.jmx.remote.tls.keystore.source,                   Key store location as an arbitrary source, defaults to null
jppf.jmx.remote.tls.keystore.type,                     Key store format, defaults to 'jks'
