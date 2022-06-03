#! /bin/sh

JVM_OPTS=-cp config:target/classes:target/lib/* -Xmx256m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties -Djava.util.logging.config.file=config/logging.properties
java $JVM_OPTS org.jppf.example.job.dependencies.JobDependenciesRunner
