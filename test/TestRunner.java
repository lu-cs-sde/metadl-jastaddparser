import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Constructor;

//import beaver.Parser.Exception;

public class TestRunner {

	public static <T extends beaver.Scanner, U extends beaver.Parser> void runTest(String path, String testName) {
		T scanner;
		U parser;
		try {
			Class<T> scannerClass = (Class<T>) Class.forName(testName + ".scanner.TestScanner");
			Constructor<T> scannerCon = scannerClass.getConstructor(Class.forName("java.io.Reader"));
			scanner = scannerCon.newInstance(new FileReader(path + '/' + testName + "/testinput"));
			Class<U> parserClass = (Class<U>) Class.forName(testName + ".parser.TestParser");
			Constructor<U> parserCon = parserClass.getConstructor();
			parser = parserCon.newInstance();
			parser.parse(scanner);
			
//			Näe, det här går inte.
//			Class<V> nodeClass = (Class<V>) Class.forName(name + ".ast.ASTNode");
//			rootNode = (V) parser.parse(scanner);
//			rootNode.print(System.out, 0);
		} catch (ClassNotFoundException e) {
			System.err.println("Error: " + testName + " does not seem to be a valid test directory");
			System.exit(1);
		} catch (FileNotFoundException e) {
			System.err.println("Error: " + testName + " does not seem to be a valid test directory");
			System.exit(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(2);
		}
	}
	
	public static void main(String[] args) {
		TestRunner.runTest(args[0], args[1]);
		System.out.println("Test successful");
	}

}
