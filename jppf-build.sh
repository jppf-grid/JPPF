#! /bin/sh

set -e

OPTS=-DskipTests

# build and tests
call mvn clean install $OPTS
# aggregted javadoc
mvn javadoc:aggregate $OPTS -Pno-samples
# package redistributable zips
mvn package $OPTS -Pjppf-release
