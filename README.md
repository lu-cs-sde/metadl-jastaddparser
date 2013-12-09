JastAddParser
=============

JastAddParser is a preprocessor to the LR parser generator
[Beaver](http://beaver.sourceforge.net). It facilitates modularization
of a parser specification into several files. It also works as a
bridge between the APIs of Beaver and [JastAdd](http://jastadd.org)'s
predefined AST classes.

JastAddParser can be run using the command
`$ java -jar JastAddParser.jar <options> <input file> <output
file>`. The resulting file will, assuming the input file is well
formed, be a parser specification that Beaver can process. Note that
if the input consists of separate files, these must be concatenated
before JastAddParser is invoked. In most cases, the order of
concatenation is not relevant. 

The available options are `--version` and
`--no-beaver-symbol`. `--version` will print out version information
and exit. If the option `--no-beaver-symbol` is specified, the 
generated parser will not assume that the classes of the AST are
sub-classes of Beaver's `Symbol` class. It will, however, assume that
the AST classes are generated using JastAdd's `--lineColumnNumbers`
switch. Thus the typical ways of using JastAdd and JastAddParser are:

* invoking JastAdd with `--beaver` (to extend `beaver.Symbol` and use
it to store line and column numbers), and

* invoking JastAdd with `--lineColumnNumbers` and JastAddParser with
`--no-beaver-symbol` (to store line and column information in
JastAdd's AST classes instead, making them independent of
`beaver.Symbol`).

License
-------

Copyright (c) 2005-2013, The JastAdd Team. All rights reserved.

JastAddParser is covered by the modified BSD License. For the full license 
text see the LICENSE file.

Dependencies
------------

JastAddParser requires a Java Runtime Environment (JRE) to run, and a JDK to
build. The minimum required Java version for JastAddParser is Java SE 6.

JastAddParser uses JastAdd2, Beaver, JFlex, JUnit and Apache Ant. JastAdd2, 
Beaver, JFlex and JUnit are included in the source tree of JastAddParser, so 
the only external tools needed are Java and Ant. See licences/ for the full 
license text for the included tools.

Specification Syntax
--------------------

JastAddParser input files use a syntax similar to [Beaver
specifications](http://beaver.sourceforge.net/spec.html), with some slight
differences. Beaver supports a number of options to be specified at the start
of a parser specification. Of these options, JastAddParser recognizes
`%header`, `%embed`, `%goal`, `%left`, `%right`, `%nonassoc`. The `%typeof` and
`%terminals` declarations are generated automatically from the JastAddParser
input file.

Production rules are augmented by an (optional) Java class name at the start of
the production, which JastAddParser uses to generate a corresponding `%typeof`
option. In productions containing optional symbols or lists, JastAddParser will
automatically introduce subclasses to JastAdd's `ASTNode`. JastAddParser also
attempts to automatically generate variable aliases for all symbols in a
production rule, in order to make them available in action routines.

For more details, see the unit tests in the `testcases` directory.

Modular Parser Support
----------------------

Multiple parser specifications can be combined with JastAddParser. If a
production occurs more than once between all included specifications, the
productions are either combined or replaced.

For example, if two separate files files include these two productions:

    a = A;
    a = B;

then they are combined into the single production `a = A | B`. If the second
production instead uses `:=` rather than `=` then the second production
replaces the first.

Building
--------

An Apache Ant script, `build.xml`, is provided in the root directory.

* Ordinary build (generate and compile)

		> ant

* Create jar file (JastAddParser.jar)

		> ant jar

* Bootstrap JastAddParser (replace the jar file used to build JastAddParser with
the newly generated one)

		> ant bootstrap

* Create source jar file (JastAddParser-src.jar)

		> ant source

* Remove generated files

		> ant clean

* Build JastAddParser release (will produce two jar files for the website and
commit and tag the new release)

		> ant release

In the `testcases` directory there is an additional Ant script that, when run,
will execute the entire test suite. This can also be accomplished by running
`test.TestJastAddParser` as a JUnit test case.

Test running
------------

Each test case has one of the following expected results:

* `JAP_PASS`         = JastAddParser successfully processes the input file(s)
* `JAP_ERR_OUTPUT`   = JastAddParser fails to process the input and terminates with specific error output
* `JAP_OUTPUT_PASS`  = JastAddParser successfully processes the input and the resulting file matches the expected file. Standard output is ignored.
* `EXEC_PASS`        = The generated parser successfully processes the parser input and outputs nothing
* `EXEC_OUTPUT_PASS` = The generated parser successfully processes the parser input and produces the expected output

Test suite structure
--------------------

Each test is contained in its own folder. The bare minimum for a test is a file
`test.properties` (specifying among other things the type of result that
implies a successful test) and one or more `.parser` files (used as input to
JastAddParser). The presence of `test.properties` is what causes the test system
to recognize the folder as a test.

Optionally, test folders may contain a short `description` file, a `.flex` file
(JFlex scanner specification) and any number of `.ast`, `.jrag` and `.jadd`
files (input files for JastAdd). The scanner specification is mandatory if the
expected result is `EXEC_PASS` or `EXEC_OUTPUT_PASS`.

If the expected result is `JAP_ERR_OUTPUT`, `JAP_OUTPUT_PASS` or
`EXEC_OUTPUT_PASS`, the test system looks for a file `test.output` and compares
the relevant output with the contents of this file.
