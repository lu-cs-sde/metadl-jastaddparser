/* Copyright (c) 2005-2015, The JastAdd Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jastadd.jastaddparser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class TestRunner {

	private static String SYS_FILE_SEP = System.getProperty("file.separator");

	/**
	 * Build necessary source files, compile and run test
	 *
	 * @param testRoot
	 *            common path to test input files
	 * @param testName
	 *            directory name of this test
	 * @param tmpRoot
	 *            path for generated code
	 */
	public static void runTest(String testRoot, String testName, String tmpRoot) {
		Properties properties = getProperties(testRoot + SYS_FILE_SEP + testName);
		TestResult expected = getResult(properties);

		setupTestDir(tmpRoot, testName);

		invokeJastAddParser(testRoot, testName, tmpRoot, properties, expected);

		if (expected != TestResult.EXEC_PASS && expected != TestResult.EXEC_OUTPUT_PASS) {
			return;
		}

		invokeJFlex(testRoot, testName, tmpRoot);
		invokeJastAdd(testRoot, testName, tmpRoot, properties);
		invokeBeaver(testRoot, testName, tmpRoot);
		compileSourceFiles(testRoot, testName, tmpRoot);
		runParser(testRoot, testName, expected, tmpRoot);
	}

	private static void setupTestDir(String tmpRoot, String testName) {
		File testDir = new File(tmpRoot, testName);
		if (!testDir.exists()) {
			testDir.mkdirs();
		} else {
			cleanDirectory(testDir);
		}
	}

	private static void cleanDirectory(File testDir) {
		for (File file : testDir.listFiles()) {
			if (file.isFile())
				file.delete();
			else
				cleanDirectory(file);
		}
	}

	private static Properties getProperties(String testPath) {
		Properties props = new Properties();
		try {
			FileInputStream in = new FileInputStream(new File(testPath, "test.properties"));
			props.load(in);
			in.close();
		} catch (FileNotFoundException e) {
			fail("Could not find properties file in " + testPath);
		} catch (IOException e) {
			fail("Could not read properties file in " + testPath);
		}
		return props;
	}

	/**
	 * Reads the expected test result from the result file in the test
	 * directory.
	 *
	 * @param testPath
	 *            the path to the test directory
	 * @return the expected result
	 */
	private static TestResult getResult(Properties properties) {
		String result = properties.getProperty("result");

		if (result.equals("JAP_PASS")) {
			return TestResult.JAP_PASS;
		} else if (result.equals("JAP_ERR_OUTPUT")) {
			return TestResult.JAP_ERR_OUTPUT;
		} else if (result.equals("JAP_OUTPUT_PASS")) {
			return TestResult.JAP_OUTPUT_PASS;
		} else if (result.equals("EXEC_PASS")) {
			return TestResult.EXEC_PASS;
		} else if (result.equals("EXEC_OUTPUT_PASS")) {
			return TestResult.EXEC_OUTPUT_PASS;
		} else {
			fail("Invalid test result option: " + result);
			return TestResult.JAP_PASS;
		}
	}

	/**
	 * Invoke JastAdd with the relevant files from the test directory
	 *
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 * @param properties
	 */
	private static void invokeJastAdd(String testRoot, String testName,
			String tmpRoot, Properties properties) {
		File testDir = new File(testRoot, testName);
		File[] files = testDir.listFiles();
		StringBuffer fileArgs = new StringBuffer();
		for (File f : files) {
			String name = f.getAbsolutePath();
			if (name.endsWith(".ast") | name.endsWith(".jadd") | name.endsWith(".jrag")) {
				fileArgs.append(' ').append(name);
			}
		}

		if (fileArgs.length() == 0) {
			return;
		}
		testName = testName.replace(SYS_FILE_SEP, ".");
		StringBuffer command = new StringBuffer("java -jar tools/jastadd2.jar");
		command.append(" --package=").append(testName).append(".ast");
		command.append(" --o=").append(tmpRoot);

		String options = properties.getProperty("JastAddOptions");
		if (options != null) {
			command.append(' ').append(options).append(' ');
		}

		command.append(fileArgs);

		executeCommand(command.toString(), "JastAdd invocation failed",
				TestResult.STEP_PASS);
	}

	/**
	 * Invoke JFlex on a .flex file assumed to be present in the test directory
	 *
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 */
	private static void invokeJFlex(String testRoot, String testName,
			String tmpRoot) {
		List<String> fList = collectFilesWithSuffix(testRoot + SYS_FILE_SEP + testName,
				".flex", false);
		String fileName = fList.get(0);

		StringBuffer command = new StringBuffer("java -jar tools/JFlex.jar");
		command.append(" -d ").append(tmpRoot).append(SYS_FILE_SEP).append(testName).append("/scanner");
		command.append(" -nobak ").append(fileName);
		executeCommand(command.toString(), "Scanner generation failed", TestResult.STEP_PASS);
	}

	/**
	 * Invoke JastAddParser with the .parser file(s) in the test directory. The
	 * test will fail if no such files are present or if the expected result was
	 * not obtained.
	 *
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 * @param properties
	 * @param expected the expected result
	 */
	private static void invokeJastAddParser(String testRoot, String testName,
			String tmpRoot, Properties properties, TestResult expected) {

		String options = properties.getProperty("JAPOptions");

		String fileName = buildJastAddParserInput(testRoot, testName, tmpRoot);

		StringBuffer command = new StringBuffer("java -jar JastAddParser.jar");
		if (options != null) {
			command.append(' ').append(options);
		}
		command.append(' ').append(fileName).append(' ');
		command.append(tmpRoot).append(SYS_FILE_SEP).append(testName).append(SYS_FILE_SEP);
		command.append("TestParser.beaver");
		executeCommand(testRoot, testName, tmpRoot,
				command.toString(), "JastAddParser invocation failed", expected);
	}

	/**
	 * Concatenate all .parser files in the test directory and write the result
	 * to the temporary directory.
	 *
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 * @return the path to the concatenated file, relative to the JastAddParser
	 *         base directory
	 */
	private static String buildJastAddParserInput(String testRoot,
			String testName, String tmpRoot) {
		List<String> files = collectFilesWithSuffix(testRoot + SYS_FILE_SEP + testName, ".parser", false);
		if (files.isEmpty()) {
			fail("Could not find JastAddParser input specification");
		}
		if (files.size() == 1) {
			return files.get(0);
		}

		// sort file names lexicographically
		Collections.sort(files);
		File concatFile = new File(tmpRoot + SYS_FILE_SEP + testName, "TestParser.all");

		try {
			FileWriter out = new FileWriter(concatFile);
			for (String s : files) {
				FileReader file = new FileReader(s);
				char[] buf = new char[1024];
				int read = file.read(buf);
				while (read != -1) {
					out.write(buf, 0, read);
					read = file.read(buf);
				}
				file.close();
			}
			out.close();
		} catch (IOException e) {
			fail("Unable to access temporary folder: " + e);
		}

		return concatFile.getPath();
	}

	/**
	 * Invoke Beaver with the previously generated .beaver file.
	 *
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 */
	private static void invokeBeaver(String testRoot, String testName,
			String tmpRoot) {
		StringBuffer fileNameBuf = new StringBuffer(tmpRoot);
		fileNameBuf.append(SYS_FILE_SEP).append(testName);
		String testDir = fileNameBuf.toString();
		fileNameBuf.append(SYS_FILE_SEP).append("TestParser.beaver");

		StringBuffer parserDirBuf = new StringBuffer(testDir);
		parserDirBuf.append("/parser");
		String parserPath = parserDirBuf.toString();
		File parserDir = new File(parserPath);
		parserDir.mkdirs();

		StringBuffer command = new StringBuffer("java -jar tools/beaver-cc.jar");
		command.append(" -d ").append(parserPath);
		command.append(" -t -w -c ").append(fileNameBuf);

		executeCommand(command.toString(), "Parser generation failed",
				TestResult.STEP_PASS);
	}

	private static void executeCommand(String command, String errorMsg,
			TestResult expected) {
		executeCommand("", "", "", command, errorMsg, expected);
	}

	/**
	 * Fork a process using the specified command.
	 *
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 * @param command the command to execute
	 * @param errorMsg error message for JUnit in case of test failure
	 * @param expected the expected result from the process
	 */
	private static void executeCommand(String testRoot, String testName, String tmpRoot, String command, String errorMsg,
			TestResult expected) {
//		 System.out.println(command);
		List<String> output = new ArrayList<String>();
//		List<String> errors = new ArrayList<String>();
		try {
			Process p = Runtime.getRuntime().exec(command);

			int exitValue = p.waitFor();

			Scanner outputScan = new Scanner(p.getInputStream());
			while (outputScan.hasNextLine()) {
				String strippedLine = clean(outputScan.nextLine());
				if (!strippedLine.isEmpty()) {
					output.add(strippedLine);
				}
			}
			outputScan.close();
			Scanner err = new Scanner(p.getErrorStream());
			while (err.hasNextLine()) {
				String strippedLine = clean(err.nextLine());
				if (!strippedLine.isEmpty()) {
					output.add(strippedLine);
				}
			}
			err.close();

			if (exitValue == 0) {
				if (expected == TestResult.JAP_ERR_OUTPUT) {
					fail("JastAddParser succeeded when expected to fail");
				} else if (expected == TestResult.JAP_OUTPUT_PASS) {
					File parserSpec = new File(tmpRoot + SYS_FILE_SEP + testName, "TestParser.beaver");
					List<String> lines = readFileLineByLine(parserSpec);
					compareOutput(testRoot + SYS_FILE_SEP + testName, lines);
				} else if (expected != TestResult.STEP_PASS) {
					if (!output.isEmpty()) {
						StringBuffer msg = new StringBuffer("Process output not empty:\n");
						for (String s : output) {
							msg.append(s).append('\n');
						}
						fail(msg.toString());
					}
				}
			} else {
				if (expected == TestResult.JAP_ERR_OUTPUT) {
					compareOutput(testRoot + SYS_FILE_SEP + testName, output);
				} else {
					StringBuffer fullErrorMsg = new StringBuffer(errorMsg).append(':');
					for (String line : output) {
						fullErrorMsg.append('\n').append(line);
					}
					fullErrorMsg.append("\nProcess exited with value ").append(exitValue);
					fail(fullErrorMsg.toString());
				}
			}
		} catch (IOException e) {
			fail(errorMsg + ":\n" + e);
		} catch (InterruptedException e) {
			fail(errorMsg + ":\n" + e);
		}
	}

	/**
	 * Compare the contents of the output file in the test directory with the
	 * received output from a process. Fail the test if it does not match.
	 *
	 * @param testDir the test directory containing the expected output
	 * @param output the received output lines in the form of a list of strings
	 */
	private static void compareOutput(String testDir, List<String> output) {
		List<String> lines = readFileLineByLine(new File(testDir, "output.test"));
		StringBuffer expected = new StringBuffer();
		for (String line : lines) {
			String strippedLine = clean(line);
			if (!strippedLine.isEmpty()) {
				expected.append(strippedLine).append('\n');
			}
		}
		StringBuffer actual = new StringBuffer();
		for (String line : output) {
			String strippedLine = clean(line);
			if (!strippedLine.isEmpty()) {
				actual.append(strippedLine).append('\n');
			}
		}
		assertEquals("Output did not match expected", expected.toString(), actual.toString());
	}

	/**
	 * Remove unwanted components of an output line.
	 *
	 * @param line
	 * @return
	 */
	private static String clean(String line) {
		if (line.startsWith("There were errors in")) {
			return "";
		} else if (line.contains("Parser specification") && line.contains("generated from")) {
			return "";
		}
		String noEOLComments = line.split("\\s*//")[0];
		return noEOLComments.trim();
	}

	/**
	 * Read a file into a list of strings
	 * @param file the reader to read from
	 * @return a List containing all the lines of the file as separate String entries
	 */
	private static List<String> readFileLineByLine(File file) {
		List<String> ans = new ArrayList<String>();
		Reader reader = null;
		try {
			reader = new FileReader(file);
			ans = readLineByLine(reader);
		} catch (FileNotFoundException e) {
			fail("Could not access test result file: " + e);
		}
		return ans;
	}

	/**
	 * Read a source into a list of strings
	 *
	 * @param reader the reader to read from
	 * @return a List containing all the lines of the file as separate String entries
	 */
	private static List<String> readLineByLine(Reader reader) {
		ArrayList<String> ans = new ArrayList<String>();
		Scanner scan = new Scanner(reader);
		scan.useDelimiter("\\n");
		while (scan.hasNext()) {
			ans.add(scan.next());
		}
		scan.close();
		return ans;
	}

	/**
	 * Compile all Java source files in the test directory
	 *
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 */
	private static void compileSourceFiles(String testRoot, String testName,
			String tmpRoot) {
		File path = new File(tmpRoot, testName);

		List<String> sourceFiles = collectFilesWithSuffix(path.getPath(),
				".java", true);
		StringBuffer arguments = new StringBuffer("-cp tools/beaver-rt.jar -g");
		for (String s : sourceFiles) {
			arguments.append(' ').append(s);
		}

		/*StringBuffer command = new StringBuffer("javac -cp tools/beaver-rt.jar -g");
		command.append(fileArgs);
		executeCommand(command.toString(),
				"Compilation of generated source files failed", TestResult.STEP_PASS);*/
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			fail("No Java compiler found");
		}
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int exitValue = compiler.run(in, out, err, arguments.toString().split(" "));
		if (exitValue != 0) {
			fail("Compilation of generated source files failed:\n" + err.toString());
		}
	}

	/**
	 * Invoke the generated parser with the generated scanner on the test input
	 * file.
	 *
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 */
	private static void runParser(
			String testRoot, String testName, TestResult expected, String tmpRoot) {
		String testPath = testRoot + SYS_FILE_SEP + testName;
		File inputFile = new File(testPath, "input.test");

		PrintStream oldOut = null;
		PrintStream oldErr = null;
		ByteArrayOutputStream baos = null;
		oldOut = System.out;
		oldErr = System.err;
		baos = new ByteArrayOutputStream(1024);
		PrintStream newOut = new PrintStream(baos);
		System.setOut(newOut);
		System.setErr(newOut);

		beaver.Scanner scanner;
		beaver.Parser parser;
		try {
			URLClassLoader cl = new URLClassLoader(new URL[] { new File(tmpRoot).toURI().toURL() });
			String testPackage = testName.replace(SYS_FILE_SEP, ".");
			Class<beaver.Scanner> scannerClass = (Class<beaver.Scanner>) cl.loadClass(testPackage + ".scanner.TestScanner");

			Constructor<beaver.Scanner> scannerCon = scannerClass.getConstructor(java.io.Reader.class);
			scanner = scannerCon.newInstance(new FileReader(inputFile));

			Class<beaver.Parser> parserClass = (Class<beaver.Parser>) cl.loadClass(testPackage + ".parser.TestParser");
			Constructor<beaver.Parser> parserCon = parserClass.getConstructor();
			parser = parserCon.newInstance();
			parser.parse(scanner);
		} catch (java.lang.Exception e) {
			fail("Parser execution failed: " + e);
		}
		System.setOut(oldOut);
		System.setErr(oldErr);
		String output = baos.toString();
		if (expected == TestResult.EXEC_OUTPUT_PASS) {
			List<String> actual = readLineByLine(new StringReader(output));
			compareOutput(testPath, actual);
		} else if(!output.isEmpty()) {
			fail("Process output not empty:\n" + output);
		}
	}

	/**
	 * Collect file names ending with a specific suffix from the specified
	 * directory
	 *
	 * @param dirPath
	 *            the directory to search
	 * @param suffix
	 *            the suffix to match
	 * @param recursive
	 *            if set to true, the whole directory sub-tree will be searched
	 * @return a java.util.List of the complete file names relative to the
	 *         system root directory
	 */
	private static List<String> collectFilesWithSuffix(String dirPath,
			String suffix, boolean recursive) {
		ArrayList<String> ans = new ArrayList<String>();
		File dir = new File(dirPath);

		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix)) {
				ans.add(f.getAbsolutePath());
			} else if (recursive && f.isDirectory()) {
				ans.addAll(collectFilesWithSuffix(f.getAbsolutePath(), suffix,
						recursive));
			}
		}

		return ans;
	}

	public static void main(String[] args) {
		TestRunner.runTest(args[0], args[1], args[2]);
		System.out.println("Test \"" + args[1] + "\" successful");
	}

	/**
	 * Find all valid test directories (directories containing a file with the
	 * name result.test) in the directory tree below the specified root directory
	 *
	 * @param rootDir
	 *            the root of the tree to search
	 * @param currentDir
	 *            the current directory to search
	 * @return path names relative to rootDir, represented as Object arrays in a
	 *         collection
	 */
	public static Collection<String> getTests(File currentDir, File rootDir) {
		List<String> tests = new ArrayList<String>();
		for (File f : currentDir.listFiles()) {
			if (f.isDirectory()) {
				tests.addAll(getTests(f, rootDir));
			} else if (f.getName().equals("test.properties")) {
				String rootPath = rootDir.getPath();
				String testPath = currentDir.getPath();
				if (testPath.startsWith(rootPath + SYS_FILE_SEP)) {
					testPath = testPath.substring(rootPath.length() + 1);
				}
				tests.add(testPath);
			}
		}
		// sort result
		Collections.sort(tests);
		return tests;
	}

}
