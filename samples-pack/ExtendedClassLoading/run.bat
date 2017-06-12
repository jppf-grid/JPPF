@echo off
call java -cp config;classes;dynamicLibs;../shared/lib/* -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties org.jppf.example.extendedclassloading.client.MyRunner %1 %2 %3 %4 %5 %6 %7 %8 %9
