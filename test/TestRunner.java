import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

//import beaver.Parser.Exception;

public class TestRunner {

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
		TestResult.Result expected = getResult(testRoot + '/' + testName);

		File testTmpDir = new File(tmpRoot, testName);
		testTmpDir.mkdirs();

		invokeJastAddParser(testRoot, testName, tmpRoot, expected);
		
		if (expected != TestResult.Result.EXEC_PASS && expected != TestResult.Result.EXEC_OUTPUT_PASS) {
			return;
		}

		invokeJFlex(testRoot, testName, tmpRoot);
		invokeJastAdd(testRoot, testName, tmpRoot);
		invokeBeaver(testRoot, testName, tmpRoot);
		compileSourceFiles(testRoot, testName, tmpRoot);
		runParser(testRoot, testName);
	}

	/**
	 * Reads the expected test result from the result file in the test
	 * directory.
	 * 
	 * @param testPath
	 *            the path to the test directory
	 * @return the expected result
	 */
	private static TestResult.Result getResult(String testPath) {
		String result = null;
		try {
			Scanner scan = new Scanner(new File(testPath, "result.test"));
			result = scan.next();
			scan.close();
		} catch (FileNotFoundException e) {
			fail("Could not find result file in " + testPath);
		}
		if (result.equals("JAP_PASS")) {
			return TestResult.Result.JAP_PASS;
		} else if (result.equals("JAP_ERR_OUTPUT")) {
			return TestResult.Result.JAP_ERR_OUTPUT;
		} else if (result.equals("JAP_OUTPUT_PASS")) {
			return TestResult.Result.JAP_OUTPUT_PASS;
		} else if (result.equals("EXEC_PASS")) {
			return TestResult.Result.EXEC_PASS;
		} else if (result.equals("EXEC_OUTPUT_PASS")) {
			return TestResult.Result.EXEC_OUTPUT_PASS;
		} else {
			fail("Invalid test result option: " + result);
			return TestResult.Result.JAP_PASS;
		}
	}

	/**
	 * Invoke JastAdd with the relevant files from the test directory
	 * 
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 * @return true if JastAdd was invoked, false if no relevant files were
	 *         found
	 */
	private static boolean invokeJastAdd(String testRoot, String testName,
			String tmpRoot) {
		File testDir = new File(testRoot, testName);
		File[] files = testDir.listFiles();
		StringBuffer fileArgs = new StringBuffer();
		for (File f : files) {
			String name = f.getAbsolutePath();
			if (name.endsWith("ast") | name.endsWith("jadd")
					| name.endsWith("jrag")) {
				fileArgs.append(' ').append(name);
			}
		}
		if (fileArgs.length() == 0) {
			return false;
		}

		StringBuffer command = new StringBuffer("java -jar tools/jastadd2.jar");
		command.append(" --package=").append(testName).append(".ast");
		command.append(" --o=").append(tmpRoot);
		command.append(" --beaver").append(fileArgs);

		executeCommand(command.toString(), "JastAdd invocation failed",
				TestResult.Result.STEP_PASS);
		return true;
	}

	/**
	 * Invoke JFlex if a .flex file is present in the test directory
	 * 
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 * @return true if JFlex was invoked, false otherwise
	 */
	private static boolean invokeJFlex(String testRoot, String testName,
			String tmpRoot) {
		List<String> fList = collectFilesWithSuffix(testRoot + '/' + testName,
				".flex", false);
		if (fList.isEmpty()) {
			return false;
		}
		String fileName = fList.get(0);

		// StringBuffer fileName = new StringBuffer(testRoot);
		// fileName.append('/').append(testName).append('/').append(testName).append(".flex");
		// File file = new File(fileName.toString());
		// if (!file.exists()) {
		// return false;
		// }

		StringBuffer command = new StringBuffer("java -jar tools/JFlex.jar");
		command.append(" -d ").append(tmpRoot).append('/').append(testName).append("/scanner");
		command.append(" -nobak ").append(fileName);
		executeCommand(command.toString(), "Scanner generation failed", TestResult.Result.STEP_PASS);
		return true;
	}

	/**
	 * Invoke JastAddParser with the .parser file in the test directory. The
	 * test will fail is no such file is present or if the expected result was
	 * not obtained.
	 * 
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 * @param expected the expected result
	 */
	private static void invokeJastAddParser(String testRoot, String testName,
			String tmpRoot, TestResult.Result expected) {
		String fileName = buildJastAddParserInput(testRoot, testName, tmpRoot);

		StringBuffer command = new StringBuffer("java -jar tools/JastAddParser.jar");
		command.append(' ').append(fileName).append(' ');
		command.append(tmpRoot).append('/').append(testName).append('/').append("TestParser.beaver");
		executeCommand(command.toString(), "JastAddParser invocation failed",
				expected);
	}

	/**
	 * Concatenate any .parser files in the test directory and write the result
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
		List<String> files = collectFilesWithSuffix(testRoot + '/' + testName, ".parser", false);
		if (files.isEmpty()) {
			fail("Could not find JastAddParser input specification");
		}
		if (files.size() == 1) {
			return files.get(0);
		}

		// sort file names lexicographically
		Collections.sort(files);
		File concatFile = new File(tmpRoot + '/' + testName, "TestParser.all");

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
	 * Invoke Beaver if a .beaver file has been generated earlier
	 * 
	 * @param testRoot
	 * @param testName
	 * @param tmpRoot
	 */
	private static void invokeBeaver(String testRoot, String testName,
			String tmpRoot) {
		StringBuffer fileNameBuf = new StringBuffer(tmpRoot);
		fileNameBuf.append('/').append(testName);
		String testDir = fileNameBuf.toString();
		fileNameBuf.append('/').append("TestParser.beaver");
		File file = new File(fileNameBuf.toString());
		if (!file.exists()) {
			return;
		}

		StringBuffer parserDirBuf = new StringBuffer(testDir);
		parserDirBuf.append("/parser");
		String parserPath = parserDirBuf.toString();
		File parserDir = new File(parserPath);
		parserDir.mkdirs();

		StringBuffer command = new StringBuffer("java -jar tools/beaver.jar");
		command.append(" -d ").append(parserPath);
		command.append(" -t -w -c ").append(fileNameBuf);

		executeCommand(command.toString(), "Parser generation failed",
				TestResult.Result.STEP_PASS);
	}
	
	private static void executeCommand(String command, String errorMsg,
			TestResult.Result expected) {
		executeCommand(command, errorMsg, expected, "");
	}

	/**
	 * Fork a process using the specified command.
	 * 
	 * @param command
	 *            the command to execute
	 * @param errorMsg
	 *            error message for JUnit in case of test failure
	 * @param expected
	 *            the expected result from the process
	 * @param testDir
	 * 			  path to the current test directory
	 */
	private static void executeCommand(String command, String errorMsg,
			TestResult.Result expected, String testDir) {
		// System.out.println(command);
		StringBuffer errors = new StringBuffer();
		try {
			Process p = Runtime.getRuntime().exec(command);
			Scanner err = new Scanner(p.getErrorStream());
			while (err.hasNextLine()) {
				errors.append(err.nextLine());
				errors.append("\n");
			}
			err.close();
			int exitValue = p.waitFor();

			if (exitValue == 0) {
				if (expected == TestResult.Result.JAP_ERR_OUTPUT) {
					// TODO implement using commented code below
					fail(errorMsg);
				} else if (expected == TestResult.Result.JAP_OUTPUT_PASS) {
					// TODO compare file output
					fail("JAP_OUTPUT_PASS not implemented");
				} else if (expected == TestResult.Result.EXEC_OUTPUT_PASS) {
					// TODO compare stream output
					fail("EXEC_OUTPUT_PASS not implemented");
				}
			} else {
				if (expected == TestResult.Result.JAP_ERR_OUTPUT) {
					// TODO compare errorstream output
					fail("JAP_ERR_OUTPUT not implemented");
				} else {
					// TODO implement using commented code below
					fail(errorMsg);
				}
			}

//				StringBuffer fullErrorMsg = new StringBuffer(errorMsg).append(':');
//				if (errors.length() > 0) {
//					fullErrorMsg.append('\n').append(errors);
//					if (failOnError) {
//						fail = true;
//					}
//				}
//				if (exitValue != 0) {
//					fullErrorMsg.append("\nProcess exited with value ").append(exitValue);
//					fail = true;
//				}
//				if (fail) {
//					fail(fullErrorMsg.toString());
//				}
		} catch (IOException e) {
			fail(errorMsg + ":\n" + e);
		} catch (InterruptedException e) {
			fail(errorMsg + ":\n" + e);
		}
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
		StringBuffer fileArgs = new StringBuffer();
		for (String s : sourceFiles) {
			fileArgs.append(' ').append(s);
		}

		if (fileArgs.length() > 0) {
			StringBuffer command = new StringBuffer(
					"javac -cp tools/beaver-rt.jar -g");
			command.append(fileArgs);
			executeCommand(command.toString(),
					"Compilation of generated source files failed", TestResult.Result.STEP_PASS);
		}
	}

	/**
	 * Collect absolute paths to all Java source files in the specified
	 * directory and append them to the specified StringBuffer.
	 * 
	 * @param path
	 *            The directory to search
	 * @param fileArgs
	 *            The StringBuffer for storing the result
	 */
	private static void collectFileArgs(File path, StringBuffer fileArgs) {
		for (File f : path.listFiles()) {
			String filePath = f.getAbsolutePath();
			if (f.isDirectory()) {
				collectFileArgs(f, fileArgs);
			} else if (filePath.endsWith("java")) {
				fileArgs.append(' ').append(filePath);
			}
		}
	}

	/**
	 * Invoke the generated parser with the generated scanner on the test input
	 * file if it exists.
	 * 
	 * @param testRoot
	 * @param testName
	 */
	private static <T extends beaver.Scanner, U extends beaver.Parser> void runParser(
			String testRoot, String testName) {
		File inputFile = new File(testRoot + '/' + testName, "input.test");
		if (!inputFile.exists()) {
			return;
		}
		T scanner;
		U parser;
		try {
			String testPackage = testName.replace('/', '.');
			Class<T> scannerClass = (Class<T>) Class.forName(testPackage
					+ ".scanner.TestScanner");

			Constructor<T> scannerCon = scannerClass
					.getConstructor(java.io.Reader.class);
			scanner = scannerCon.newInstance(new FileReader(inputFile));

			Class<U> parserClass = (Class<U>) Class.forName(testPackage
					+ ".parser.TestParser");
			Constructor<U> parserCon = parserClass.getConstructor();
			parser = parserCon.newInstance();
			parser.parse(scanner);
		} catch (java.lang.Exception e) {
			fail("Parser execution failed: " + e);
		}
		/*
		 * catch (ClassNotFoundException e) { e.printStackTrace(); } catch
		 * (NoSuchMethodException e) { e.printStackTrace(); } catch
		 * (SecurityException e) { e.printStackTrace(); } catch
		 * (InstantiationException e) { e.printStackTrace(); } catch
		 * (IllegalAccessException e) { e.printStackTrace(); } catch
		 * (IllegalArgumentException e) { e.printStackTrace(); } catch
		 * (InvocationTargetException e) { e.printStackTrace(); } catch
		 * (FileNotFoundException e) { e.printStackTrace(); } catch (IOException
		 * e) { e.printStackTrace(); } catch (beaver.Parser.Exception e) {
		 * e.printStackTrace(); }
		 */
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

		// File[] files = dir.listFiles();
		// String fileName = null;
		// int i = 0;
		// File f = files[i];
		// while (i < files.length && fileName == null) {
		// String name = f.getAbsolutePath();
		// if (name.endsWith(suffix)) {
		// fileName = name;
		// }
		// f = files[++i];
		// }
		// return fileName;
	}

	public static void main(String[] args) {
		TestRunner.runTest(args[0], args[1], args[2]);
		System.out.println("Test \"" + args[1] + "\" successful");
	}

	/**
	 * Find all valid test directories (directories containing one or more
	 * *.parser files) in the directory tree below the specified root directory
	 * 
	 * @param rootDir
	 *            the root of the tree to search
	 * @param currentDir
	 *            the current directory to search
	 * @return path names relative to rootDir, represented as Object arrays in a
	 *         collection
	 */
	public static Collection<Object[]> getTests(File currentDir, File rootDir) {
		Collection<Object[]> tests = new ArrayList<Object[]>();
		for (File f : currentDir.listFiles()) {
			if (f.isDirectory()) {
				tests.addAll(getTests(f, rootDir));
			} else if (f.getName().equals("result.test")) {
				String rootPath = rootDir.getPath();
				String testPath = currentDir.getPath();
				if (testPath.startsWith(rootPath + '/')) {
					testPath = testPath.substring(rootPath.length() + 1);
				}
				tests.add(new Object[] { testPath });
			}
		}
		return tests;
	}

}
