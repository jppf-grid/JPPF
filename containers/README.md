# JPPF containers.

This module holds:
- everything required to build [JPPF docker images](images/)
  - [driver](images/driver)
  - [node](images/node)
  - [web adminstration console](images/admin-web)
- [configuration files](swarm/) to deploy JPPF as a Docker service stack
- [helm charts](k8s/) to deploy JPPF to a Kubernetes cluster
