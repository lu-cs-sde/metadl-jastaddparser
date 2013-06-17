JastAddParser
=============

JastAddParser is a preprocessor to the LR parser generator *beaver*.

To build JastAddParser, run `$ ant`. To obtain a runnable jar file,
run `$ ant jar`.

To test JastAddparser, run `$ ant test`. This will build and jar
JastAddParser before the test suite is run. Note that some tests will
fail due to the current test architecture's lack of support for test cases
that are intended to fail.
