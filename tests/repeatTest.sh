#! /bin/sh

# Repeat the specified tests a specified number of times
# and stop at the first failure, if any
#
# Parameters:
# $1 is an Ant fileset pattern for the tests to execute
# $2 is the number of repeats

echo -n "start time: " > time.tmp
date --iso-8601='seconds' >> time.tmp

rm -f logs/*.zip > nul.tmp

if [ -z $1 ]; then
  PATTERN="**/Test*.java"
else
  PATTERN="$1"
fi

if [ -z $2 ]; then
  NB_REPEATS=1
else
  NB_REPEATS=$2
fi

echo "PATTERN = $PATTERN, repeats = $2"

for i in `seq 1 $NB_REPEATS`; do
  echo ""
  echo "===== Test run $i ====="
  rm -f *.log > nul.tmp
  ant test.pattern2 -Dpattern=$PATTERN
  if [ -e failure.log ]; then
    rm -f failure.log
    break
  fi
done

echo -n "end time:   " >> time.tmp
date --iso-8601='seconds' >> time.tmp

echo ""
cat time.tmp
rm -f *.tmp
