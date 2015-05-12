package org.jastadd.jastaddparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.jastadd.jastaddparser.parser.GrammarParser;
import org.jastadd.jastaddparser.parser.GrammarScanner;
import org.jastadd.jastaddparser.AST.ASTNode;
import org.jastadd.jastaddparser.AST.Grammar;

public class Main {

	public static void main(String[] args) {
		try {
			boolean noBeaverSymbol = false;
			if (args[0].equals("--version")) {
				System.out.println("JastAddParser version " + versionString());
				System.exit(0);
			} else if (args[0].equals("--no-beaver-symbol")) {
				noBeaverSymbol = true;
			}
			if (args.length > 2 && !noBeaverSymbol) {
				System.err.println("Unrecognized option \"" + args[0] + '\"');
				System.exit(1);
			}
			int sourceIndex = args.length == 3 ? 1 : 0;
			int destIndex = args.length == 3 ? 2 : 1;
			String source = args[sourceIndex];
			String dest = args[destIndex];
			File sourceFile = new File(source);
			File destFile = new File(dest);
			if (sourceFile.exists() && destFile.exists()
					&& sourceFile.lastModified() < destFile.lastModified()) {
				System.out.println("Parser specification " + dest + " need not be regenerated");
			} else {
				ASTNode.sourceName = args[sourceIndex];
				GrammarScanner scanner = new GrammarScanner(new FileReader(args[sourceIndex]));
				GrammarParser parser = new GrammarParser();
				Object o = parser.parse(scanner);
				Grammar root = (Grammar) o;
				ArrayList<String> errors = new ArrayList<String>();
				ArrayList<String> warnings = new ArrayList<String>();
				root.errorCheck(errors, warnings);
				if (!errors.isEmpty()) {
					System.err.println("There were errors in " + args[sourceIndex] + ":");
					for (Iterator iter = errors.iterator(); iter.hasNext(); )
						System.err.println(iter.next());
					System.exit(1);
				}
				for (Iterator iter = warnings.iterator(); iter.hasNext(); )
					System.err.println(iter.next());
				FileOutputStream os = new FileOutputStream(args[destIndex]);
				PrintStream out = new PrintStream(os);
				root.pp(out, noBeaverSymbol);
				out.close();
				System.out.println("Parser specification " + dest + " generated from " + source);
			}
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Usage: java -jar JastAddParser.jar <options> " + 
					"<source file name> <destination file name>");
			System.exit(1);
		} catch (beaver.Parser.Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	private static String versionString() {
		Properties props = new Properties();
		String version = null;
		try {
			InputStream in = Main.class.getResourceAsStream("/properties/version.properties");
			props.load(in);
			version = props.getProperty("version");
		} catch (Exception e) {
			System.err.println("Could not retrieve version information");
			System.exit(1);
		}
		return version;
	}
}
