#! /bin/sh

rm -f logs/*.zip
ant test.pattern -Dpattern=$1
