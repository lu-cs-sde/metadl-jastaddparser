


import parser.*;
import java.io.*;
import java.util.*;

import beaver.Parser.Exception;
import AST.*;

public class Main {

	public static void main(String[] args) {
		try {
			if(args.length != 2) {
				System.err.println("Arguments: SourceFileName DestFileName");
				System.exit(1);
			}
			String source = args[0];
			String dest = args[1];
			File sourceFile = new File(source);
			File destFile = new File(dest);
			if(sourceFile.exists() && destFile.exists()
			   && sourceFile.lastModified() < destFile.lastModified()) {
				System.out.println("Parser specification " + dest + " need not be regenerated");
			}
			else {
        ASTNode.sourceName = args[0];
				GrammarScanner scanner = new GrammarScanner(new FileReader(args[0]));
				GrammarParser parser = new GrammarParser();
				Object o = parser.parse(scanner);
				Grammar root = (Grammar)o;
		ArrayList<String> errors = new ArrayList<String>();
		ArrayList<String> warnings = new ArrayList<String>();
        root.errorCheck(errors, warnings);
        if(!errors.isEmpty()) {
          System.err.println("There were errors in " + args[0] + ":");
          for(Iterator iter = errors.iterator(); iter.hasNext(); )
            System.err.println(iter.next());
          System.exit(1);
        }
        for(Iterator iter = warnings.iterator(); iter.hasNext(); )
            System.err.println(iter.next());
				FileOutputStream os = new FileOutputStream(args[1]);
				PrintStream out = new PrintStream(os);
				root.pp(out);
				out.close();
				System.out.println("Parser specification " + dest + " generated from " + source);
			}
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
}
