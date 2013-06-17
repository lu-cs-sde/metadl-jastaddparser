import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;

//import beaver.Parser.Exception;

public class TestRunner {

	/**
	 * Build needed source files, compile and run test
	 * 
	 * @param testRoot common path to test input files
	 * @param testName directory name of this test
	 * @param genPath path for generated code
	 */
	public static void runTest(String testRoot, String testName, String genPath) {
		File testGenDir = new File(genPath + '/' + testName);
		testGenDir.mkdirs();
		
		invokeJastAdd(testRoot, testName, genPath);
		boolean hasParser = invokeJFlex(testRoot, testName, genPath);
		invokeJastAddParser(testRoot, testName, genPath);
		if (hasParser) {
			invokeBeaver(testRoot, testName, genPath);
			compileSourceFiles(testRoot, testName, genPath);
			runParser(testRoot, testName);
		}
	}
	
	/**
	 * Invoke JastAdd with the relevant files from the test directory
	 * 
	 * @param testRoot
	 * @param testName
	 * @param genPath
	 * @return true if JastAdd was invoked, false if no relevant files were found
	 */
	private static boolean invokeJastAdd(String testRoot, String testName, String genPath) {
		File testDir = new File(testRoot + '/' + testName);
		File[] files = testDir.listFiles();
		StringBuffer fileArgs = new StringBuffer();
		for (File f : files) {
			String name = f.getAbsolutePath();
			if (name.endsWith("ast") | name.endsWith("jadd") | name.endsWith("jrag")) {
				fileArgs.append(' ').append(name);
			}
		}
		if (fileArgs.length() == 0) {
			return false;
		}
		
		StringBuffer command = new StringBuffer("java -jar tools/jastadd2.jar");
		command.append(" --package=").append(testName).append(".ast");
		command.append(" --o=").append(genPath);
		command.append(" --beaver").append(fileArgs);
		
		executeCommand(command.toString(), "JastAdd invocation failed", false);
		return true;
	}
	
	/**
	 * Invoke JFlex if a .flex file is present in the test directory
	 * 
	 * @param testRoot
	 * @param testName
	 * @param genPath
	 * @return true if JFlex was invoked, false otherwise
	 */
	private static boolean invokeJFlex(String testRoot, String testName, String genPath) {
		StringBuffer fileName = new StringBuffer(testRoot);
		fileName.append('/').append(testName).append('/').append(testName).append(".flex");
		File file = new File(fileName.toString());
		if (!file.exists()) {
			return false;
		}
		
		StringBuffer command = new StringBuffer("java -jar tools/JFlex.jar");
		command.append(" -d ").append(genPath).append('/').append(testName).append("/scanner");
		command.append(" -nobak ").append(fileName);
		executeCommand(command.toString(), "Scanner generation failed", false);
		return true;
	}
	
	/**
	 * Invoke JastAddParser with the .parser file in the test directory.
	 * The test will fail is no such file is present.
	 * 
	 * @param testRoot
	 * @param testName
	 * @param genPath
	 */
	private static void invokeJastAddParser(String testRoot, String testName, String genPath) {
		//TODO concatenate several .parser files before invocation
		StringBuffer fileNameBuf = new StringBuffer(testRoot);
		fileNameBuf.append('/').append(testName).append('/').append(testName).append(".parser");
		File file = new File(fileNameBuf.toString());
		if (!file.exists()) {
			fail("Could not find JastAddParser input specification");
		}
		
		StringBuffer command = new StringBuffer("java -jar tools/JastAddParser.jar");
		command.append(' ').append(fileNameBuf).append(' ');
		command.append(genPath).append('/').append(testName).append('/').append("TestParser.beaver");
		executeCommand(command.toString(), "JastAddParser invocation failed", false);
	}
	
	/**
	 * Invoke Beaver if a .beaver file has been generated earlier
	 * 
	 * @param testRoot
	 * @param testName
	 * @param genPath
	 */
	private static void invokeBeaver(String testRoot, String testName, String genPath) {
		StringBuffer fileNameBuf = new StringBuffer(genPath);
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
		
		// Beaver always exits with 0 on errors, so check the error output
		executeCommand(command.toString(), "Parser generation failed", true);
	}

	/**
	 * Fork a process using the specified command. The test will fail if the
	 * process does not exit normally.
	 * 
	 * @param command
	 *            The command to execute
	 * @param errorMsg
	 *            Error message for JUnit
	 * @param failOnErrOut
	 *            If set to false, the test will only fail if the process
	 *            terminates with a non-zero exit code. If set to true, the test
	 *            will also fail on a non-empty error stream.
	 */
	private static void executeCommand(String command, String errorMsg, boolean failOnErrOut) {
		System.out.println(command);
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
			boolean fail = false;
			StringBuffer fullErrorMsg = new StringBuffer(errorMsg).append(':');
			if (errors.length() > 0) {
				fullErrorMsg.append('\n').append(errors);
				if (failOnErrOut) {
					fail = true;
				}
			}
			if (exitValue != 0) {
				fullErrorMsg.append("\nProcess exited with value ").append(exitValue);
				fail = true;
			}
			if (fail) {
				fail(fullErrorMsg.toString());
			}
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
	 * @param genPath
	 */
	private static void compileSourceFiles(String testRoot, String testName,
			String genPath) {
		StringBuffer pathBuf = new StringBuffer(genPath);
		pathBuf.append('/').append(testName);
		File path = new File(pathBuf.toString());
		
		StringBuffer fileArgs = new StringBuffer();
		collectFileArgs(path, fileArgs);

		if (fileArgs.length() > 0) {
			StringBuffer command = new StringBuffer("javac -cp tools/beaver-rt.jar -g");
			command.append(fileArgs);
			executeCommand(command.toString(), "Compilation of generated source files failed", false);
		}
	}
	
	/**
	 * Collect absolute paths to all Java source files in the specified directory
	 * and append them to the specified StringBuffer.
	 * 
	 * @param path The directory to search
	 * @param fileArgs The StringBuffer for storing the result
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
	 * Invoke the generated parser with the generated scanner on the test input file if it exists.
	 * 
	 * @param testRoot
	 * @param testName
	 */
	private static <T extends beaver.Scanner, U extends beaver.Parser> void runParser(String testRoot, String testName) {
		File inputFile = new File(testRoot + '/' + testName + "/testinput");
		if (!inputFile.exists()) {
			return;
		}
		T scanner;
		U parser;
		try {
			Class<T> scannerClass = (Class<T>) Class.forName(testName + ".scanner.TestScanner");
			
			Constructor<T> scannerCon = scannerClass.getConstructor(java.io.Reader.class);
			scanner = scannerCon.newInstance(new FileReader(inputFile));
			
			Class<U> parserClass = (Class<U>) Class.forName(testName + ".parser.TestParser");
			Constructor<U> parserCon = parserClass.getConstructor();
			parser = parserCon.newInstance();
			parser.parse(scanner);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (beaver.Parser.Exception e) {
			e.printStackTrace();
		}
	}

//	private static String getFileNameFromSuffix(String dirPath, String suffix) {
//		File dir = new File(dirPath);
//		File[] files = dir.listFiles();
//		String fileName = null;
//		int i = 0;
//		File f = files[i];
//		while (i < files.length && fileName == null) {
//			String name = f.getAbsolutePath();
//			if (name.endsWith(suffix)) {
//				fileName = name;
//			}
//			f = files[++i];
//		}
//		return fileName;
//	}

	public static void main(String[] args) {
		TestRunner.runTest(args[0], args[1], args[2]);
		System.out.println("Test successful");
	}

}
