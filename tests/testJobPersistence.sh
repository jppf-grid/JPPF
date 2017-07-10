#! /bin/sh

rm -f logs/*.zip
ant test.pattern -Dpattern=test/org/jppf/job/persistence/Test*.java
