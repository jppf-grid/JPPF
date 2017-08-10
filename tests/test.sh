#! /bin/sh

rm -f logs/*.zip
pattern=$1
if [ -z "$pattern" ]
then pattern=**/JPPFSuite.java
fi 
ant test.pattern -Dpattern=$pattern
