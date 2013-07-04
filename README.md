JastAddParser
=============

JastAddParser is a preprocessor to the LR parser generator *Beaver*. It facilitates modularization of a parser specification into several files.

JastAddParser can be run using the command `$ java -jar JastAddParser.jar input-file output-file`. The resulting file will, assuming the input file is well formed, be a parser specification that Beaver can process. Note that if the input consists of separate files, these must be concatenated before JastAddParser is invoked. In most cases, the order of concatenation is not relevant.

Specification syntax
--------------------

JastAddParser input files use a syntax similar to Beaver's, with some slight differences. Beaver supports a number of options to be specified at the start of a parser specification. Of these options, JastAddParser recognizes `%header`, `%embed` and `%goal`. `%typeof` and `%terminals` are generated automatically from the JastAddParser input file.

Production rules are augmented by an (optional) Java class name at the start of the production, which JastAddParser uses to generate a corresponding `%typeof` option. JastAddParser also attempts to automatically generate variable aliases for all symbols in a production rule, in order to make them available in action routines.

For more details, see the unit tests in the `testcases` directory.

Building
--------

An Apache Ant script is provided in the root directory; build.xml.

* Ordinary build (generate and compile)

	> ant

* Create jar file (JastAddParser.jar)

	> ant jar

* Bootstrap JastAddParser (replace the jar file used to build JastAddParser with the newly generated one)

	> ant bootstrap

* Create source jar file (JastAddParser-src.jar)

	> ant source

* Build and test JastAddParser

	> ant test

* Remove generated files

	> ant clean

Test running
------------

Each test case has one of the following expected results:

* `JAP_PASS`         = JastAddParser successfully processes the input file(s)
* `JAP_ERR_OUTPUT`   = JastAddParser fails to process the input and terminates with specific error output
* `JAP_OUTPUT_PASS`  = JastAddParser successfully processes the input and the resulting file matches the expected file
* `EXEC_PASS`        = The generated parser successfully processes the parser input
* `EXEC_OUTPUT_PASS` = The generated parser successfully processes the parser input and produces the expected output

Test suite structure
--------------------

Each test is contained in its own folder. The bare minimum for a test is a file `result.test` (specifying the type of result that implies a successful test) and one or more `.parser` files (used as input to JastAddParser). The presence of `result.test` is what causes the test system to recognize the folder as a test.

Optionally, test folders may contain a short `description` file, a `.flex` file (JFlex scanner specification) and any number of `.ast`, `.jrag` and `.jadd` files (input files for JastAdd). The scanner specification is mandatory if the expected result is `EXEC_PASS` or `EXEC_OUTPUT_PASS`.

If the expected result is `JAP_ERR_OUTPUT`, `JAP_OUTPUT_PASS` or `EXEC_OUTPUT_PASS`, the test system looks for a file `test.output` and compares the relevant output with the contents of this file.
