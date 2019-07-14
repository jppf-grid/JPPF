# JPPF containers

## Module layout

This module holds:
- everything required to build [JPPF docker images](images/)
  - [driver](images/driver)
  - [node](images/node)
  - [web adminstration console](images/admin-web)
- [configuration files](swarm/) to deploy JPPF as a Docker service stack
- [helm charts](k8s/) to deploy JPPF to a Kubernetes cluster

## Docker images registry

The latest JPPF docker images are available on [Docker Hub](https://hub.docker.com/?) in the [JPPF docker registry](https://cloud.docker.com/u/jppfgrid/repository/list)

We currently have images and corresponding `Dockerfile`s for:

- JPPF [drivers](./images/driver/Dockerfile) 
- JPPF [nodes](images/node/Dockerfile)
- the JPPF [web administration console](images/admin-web/Dockerfile)

## JPPF Helm chart

JPPF docker images can be deployed in a Kubernetes cluster using the [JPPF helm chart](k8s/jppf).

JPPF hosts its own Helm charts repository at `https://www.jppf.org//helm-charts`.

To add this repository under the name "jppf-repo":

```bash
$ helm repo add jppf-repo https://www.jppf.org//helm-charts
```

To list all the charts in the JPPF repository:

```bash
$ helm search jppf-repo/
```

To install from the repository:

```bash
$ helm install --name jppf --namespace jppf jppf-repo/jppf
```
 
## JPPF service stack

JPPF can also be deployed in a [Docker swarm](https://docs.docker.com/engine/swarm/) cluster with Docker stack.
The deployment relies on a [`docker-compose.yml`](swarm/docker-compose.yml) file which defines the JPPF services to deploy,
and a [`.env`](swarm/.env) file which defines the environment variables the services depend on. 

To deploy JPPF in a swarm cluster with a stack name "jppf":

```bash
$ docker stack deploy -c ./docker-compose.yml jppf
```

To shutdown the JPPF service stack:

```bash
$ docker stack rm jppf
```

