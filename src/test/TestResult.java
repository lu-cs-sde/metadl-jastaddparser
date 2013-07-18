package test;
/**
 * Possible results from a JastAddParser test.
 */
public enum TestResult {

	/**
	 * JastAddParser successfully processes the input file(s)
	 */
	JAP_PASS,

	/**
	 * JastAddParser fails to process the input and terminates with specific
	 * error output
	 */
	JAP_ERR_OUTPUT,

	/**
	 * JastAddParser successfully processes the input and the resulting file
	 * matches the expected file. Standard output is ignored.
	 */
	JAP_OUTPUT_PASS,

	/**
	 * The generated parser successfully processes the parser input and outputs
	 * nothing
	 */
	EXEC_PASS,

	/**
	 * The generated parser successfully processes the parser input and produces
	 * the expected output
	 */
	EXEC_OUTPUT_PASS,

	/**
	 * Used internally by the test runner. The test will continue to the next
	 * step if the process terminates normally, and fail otherwise.
	 */
	STEP_PASS
}