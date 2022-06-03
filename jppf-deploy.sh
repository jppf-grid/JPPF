#! /bin/sh

set -e

# deploy jppf artifacts to Sonatype nexus
mvn deploy $JPPF_BUILD_OPTS -Pdeployment
