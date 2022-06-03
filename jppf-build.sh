#! /bin/sh

set -e

# build and tests
mvn clean install $JPPF_BUILD_OPTS

# aggregated javadoc
mvn javadoc:aggregate $JPPF_BUILD_OPTS -Pno-samples

# package redistributable zips
mvn package $JPPF_BUILD_OPTS -Pjppf-release
