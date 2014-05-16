@echo off
call java -cp config;classes;../shared/lib/* -Xmx256m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties -Djava.util.logging.config.file=config/logging.properties org.jppf.example.fj.FibonacciFJ
