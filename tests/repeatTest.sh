#! /bin/sh

set -e

# Repeat the specified tests a specified number of times
# and stop at the first failure, if any
#
# Parameters:
# $1 is a pattern supplied to maven as the value of the "-Dtest=..." property
# $2 is the number of repeats


if [ -z $1 ]; then
  echo "you must specify the tests to run as first argument"
  exit 1
else
  PATTERN="$1"
fi

if [ -z $2 ]; then
  NB_REPEATS=1
else
  NB_REPEATS=$2
fi

echo -n "start time: " > time.tmp
date --iso-8601='seconds' >> time.tmp

rm -f logs/*.zip > nul.tmp

echo "PATTERN = $PATTERN, repeats = $2"

for i in `seq 1 $NB_REPEATS`; do
  echo ""
  echo "===== Test run $i ====="
  rm -f *.log > nul.tmp
  mvn test -Dtest=$PATTERN -q
done

echo -n "end time:   " >> time.tmp
date --iso-8601='seconds' >> time.tmp

echo ""
cat time.tmp
rm -f *.tmp
