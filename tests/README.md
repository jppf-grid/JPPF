# JPPF tests module


## Running the tests

Follow these steps:

- clone the repository: `git clone git@github.com:jppf-grid/JPPF.git` <br>
  or `git clone https://github.com/jppf-grid/JPPF.git`
- build JPPF and run the tests: from the root JPPF installation folder:<br> `mvn clean test`

There are 3 different sets of tests that you can run, each in a separate maven profile:
- `test.full`: the fullset of JPPF tests will be run. Depending on your hardware, the full test suite can take between
  15-25mn to complete. This can seem a very long time for day-to-day operations. To reduce the time spent testing, you can
  run a lighter test suite, which is not as exhaustive as the full suite but still covers the esentials.
- `test.lite`: the "lite" suite takes around 2.5x less time than the full suite (i.e. 6-10mn)
- `test.min`: this is the minimum set of tests, if only checks a small subset of the JPPF features and lasts around 1mn.
  This is the default set of tests if none is specified.

To run a specific set of tests, run the tests with the corresponding profile name: <br>
`mvn clean test -P<profile_name>`