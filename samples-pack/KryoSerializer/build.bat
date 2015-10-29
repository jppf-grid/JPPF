@echo off

echo compiling ...
call javac -cp lib/*;../shared/lib/* -sourcepath src -g -source 1.6 -target 1.6 -d classes src/org/jppf/serialization/kryo/*.java

echo creating KryoSerializer.jar
call jar -cf KryoSerializer.jar -C classes .