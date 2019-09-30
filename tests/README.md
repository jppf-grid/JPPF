# JPPF tests module



## Running the tests

Follow these steps:

- clone the repository: `git clone git@github.com:jppf-grid/JPPF.git`<br> or `git clone https://github.com/jppf-grid/JPPF.git`
- build JPPF and run the tests: from the root JPPF installation folder:<br> `ant build test`

Depending on your hardware, the full test suite can take between 15-25mn to complete. This can seem a very long time for day-to-day operations.
To reduce the time spent testing, you can run a lighter test suite, which is not as exhaustive as the full suite but still covers the esentials: `ant buid test.lite`

The "lite" suite takes around 2.5x less time than the full suite (i.e. 6-10mn).


## How to get it


## Environment properties

#### Misc:



#### TLS properties:


#### Authentication and authorization

