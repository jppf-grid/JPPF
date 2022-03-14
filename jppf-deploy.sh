#! /bin/sh

set -e

OPTS=-DskipTests

# deploy jppf artifacts to Sonatype nexus
mvn deploy $OPTS -Pdeployment
