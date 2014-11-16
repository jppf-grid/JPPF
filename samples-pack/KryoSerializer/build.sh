#! /bin/sh

echo compiling ...
javac -cp lib/*:../shared/lib/* -sourcepath src -g -source 1.6 -target 1.6 -d classes src/org/jppf/serialization/kryo/*.java

echo creating KryoSerializer.jar
jar -cf KryoSerializer.jar -C classes .
