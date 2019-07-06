# JPPF Helm Chart

[JPPF](https://www.jppf.org/) enables applications with large processing power requirements to be run on any number of computers, in order to dramatically reduce their processing time.

## Introduction

This chart deploys and starts a [JPPF grid](https://www.jppf.org/doc/6.2/index.php?title=JPPF_Overview#Architecture_and_topology) on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager.

## Prerequisites

- Kubernetes 1.9+

## Installing the Chart

To install the JPPF chart with a release name "jppf" in the "jppf" Kubernetes namespace:

```bash
$ helm install --name jppf --namespace jppf jppf
```

This command deploys a JPPF grid which includes a JPPF driver, 2 nodes and a web administration console, all with default options.

## Uninstalling the Chart

To uninstall/delete the "jppf" deployment:

```bash
$ helm delete jppf
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Configuration

The following table lists the configurable values of th"e JPPF chart.

| Name                            | Description                                                       | Default                                 |
|---------------------------------|-------------------------------------------------------------------|---------------------                    |
| ***JPPF driver configuration*** |                                                                   |                                         |
| `driver.replicas`               | Number of driver instances. Only 1 is supported yet               | 1                                       |
| `driver.imageRepository`        | Docker image repository name                                      | jppf-driver                             |
| `driver.jvmOptions`             | Options given to the driver's JVM                                 | "-Xmx256m -Xms64m"                      |
| `driver.serviceType`            | Type of driver service, either NodePort or ClusterIP              | NodePort                                |
| `driver.servicePort`            | Plain port exposed on all K8s nodes (if serviceType is "NodePort")| 31111                                   |
| `driver.servicePortTLS`         | TLS port exposed on all K8s nodes (if serviceType is "NodePort")  | 31443                                   |
| ***JPPF node configuration***   |                                                                   |                                         |
| `node.replicas`                 | Number of nodes in the grid                                       | 2                                       |
| `node.imageRepository`          | Docker image repository name                                      | jppf-node                               |
| `node.jvmOptions`               | Options given to the node's JVM                                   | "-Xmx128m -Xms32m"                      |
| `node.processingThreads`        | Number of processing threads in a node                            | "1"                                     |
| ***JPPF web admin console***    |                                                                   |                                         |
| `admin.enabled`                 | whether the amdin console should be deployed                      | true                                    |
| `admin.replicas`                | Number of admin consoles to deploy in the K8s cluster             | 1                                       |
| `admin.imageRepository`         | Docker image repository name                                      | jppf-admin-web                          |
| `admin.servicePort`             | HTTP port exposed on all K8S nodes                                | 31180                                   |
| `admin.tomcatUsers`             | base64-encoded tomcat-users.xml file content                      | see [Tomcat users](#tomcat-users)       |
| ***Docker images common***      |                                                                   |                                         |
| `image.registryName`            | Docker image repository name                                      | jppfgrid                                |
| `image.tag`                     | Tag for all images in the cluster                                 | 6.2-alpha                               |
| `image.pullPolicy`              | Determines when images are pulled upon installation               | Always                                  |
| ***JPPF components common***    |                                                                   |                                         |
| `jppf.version`                  | Version of JPPF being deployed                                    | "6.2-alpha"                             |
| `jppf.serverHostName`           | Host name for the JPPF driver                                     | jppf-driver-service                     |
| `jppf.serialization`            | Serialization scheme | [JPPF default](https://www.jppf.org/doc/6.2/index.php?title=Specifying_alternate_serialization_schemes#Default_serialization) |
| ***TLS configuration***         |                                                                   |                                         |
| `tls.enabled`                   | Whether TLS communication is enabled                              | "false"                                 |
| `tls.storeType`                 | The type of the Java key- and trust-stores                        | "JKS"                                   |
| `tls.driver.keystore`           | Driver's base64-encoded keystore                                  | see [TLS credentials](#tls-credentials) |
| `tls.driver.keystorePassword`   | Driver keystore's base64-encoded password                         | see [TLS credentials](#tls-credentials) |
| `tls.driver.truststore`         | Driver's base64-encoded truststore                                | see [TLS credentials](#tls-credentials) |
| `tls.driver.truststorePassword` | Driver truststore's base64-encoded password                       | see [TLS credentials](#tls-credentials) |
| `tls.node.keystore`             | Node's base64-encoded keystore                                    | see [TLS credentials](#tls-credentials) |
| `tls.node.keystorePassword`     | Node keystore's base64-encoded password                           | see [TLS credentials](#tls-credentials) |
| `tls.node.truststore`           | Node's base64-encoded truststore                                  | see [TLS credentials](#tls-credentials) |
| `tls.node.truststorePassword`   | Node truststore's base64-encoded password                         | see [TLS credentials](#tls-credentials) |
| `tls.admin.keystore`            | Admin's base64-encoded keystore                                   | see [TLS credentials](#tls-credentials) |
| `tls.admin.keystorePassword`    | Admin keystore's base64-encoded password                          | see [TLS credentials](#tls-credentials) |
| `tls.admin.truststore`          | Admin's base64-encoded truststore                                 | see [TLS credentials](#tls-credentials) |
| `tls.admin.truststorePassword`  | Admin truststore base64-encoded password                          | see [TLS credentials](#tls-credentials) |


### TLS credentials:

For key- and trust- stores, the value is the store's content encoded in base64 format, such as obained from the output of this command:

```bash
$ base64 -w 0 tls/my_keystore.jks
```

For passwords, the value is the base64 encoding of the password string:

```bash
$ base64 <<< "password"
cGFzc3dvcmQK
```

### Specifying or overriding configuration values:

The values can be specified using the `--set <name>=<value>` flag of [`helm install`](https://helm.sh/docs/helm/#helm-install). For instance, the following configures 4 nodes in the JPPF grid:

```bash
$ helm install --name jppf --set node.replicas=4 jppf
```


Alternatively, values can be provided in a separate `.yaml` file, as in the following example:

```bash
$ helm install --name jppf -f values-overrides.yaml jppf
```

### Customizing the JPPF configuration

Custom configuration properties can be specified inside the `values.yaml` file:

```yaml
driver:
  configOverrides: |-
    custom.driver.prop.1 = driver value 1
    custom.driver.prop.2 = driver value 2

node:
  configOverrides: |-
    custom.node.prop.1 = node value 1
    custom.node.prop.2 = node value 2

admin:
  configOverrides: |-
    custom.admin.prop.1 = admin value 1
    custom.admin.prop.2 = admin value 2
```

### CPU and memory resources

The CPU and memory requests and limits are specified in the values.yaml (or a separate override file), as follows:

```yaml
driver:
  resources:
    requests:
      memory: "256Mi"
      cpu: "500m"
    limits:
      memory: "512Mi"
      cpu: "1000m"

node:
  resources:
    requests:
      memory: "128Mi"
      cpu: "250m"
    limits:
      memory: "256Mi"
      cpu: "500m"

admin:
  resources:
    requests:
      memory: "256Mi"
      cpu: "250m"
    limits:
      memory: "512Mi"
      cpu: "500m"
```

### Tomcat users

The web administration console is deployed in a Tomcat container. Since it requires pre-defined roles, a default user-to-role mapping is provided:

```xml
<?xml version='1.0' encoding='utf-8'?>
<tomcat-users>
  <role rolename="jppf-admin"/>
  <role rolename="jppf-manager"/>
  <role rolename="jppf-monitor"/>
  <user username="jppf"    password="jppf"    roles="jppf-manager,jppf-admin"/>
  <user username="admin"   password="admin"   roles="jppf-admin"/>
  <user username="manager" password="manager" roles="jppf-manager"/>
  <user username="monitor" password="monitor" roles="jppf-monitor"/>
</tomcat-users>
```
