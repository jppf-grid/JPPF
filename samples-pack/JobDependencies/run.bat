@echo off
set JVM_OPTS=-cp config;classes;../shared/lib/* -Xmx256m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties -Djava.util.logging.config.file=config/logging.properties
call java  %JVM_OPTS% org.jppf.example.job.dependencies.JobDependenciesRunner
