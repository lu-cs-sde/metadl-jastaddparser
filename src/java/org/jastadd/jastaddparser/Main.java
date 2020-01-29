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
package org.jastadd.jastaddparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.jastadd.jastaddparser.parser.GrammarParser;
import org.jastadd.jastaddparser.parser.GrammarScanner;
import org.jgrapht.Graph;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jastadd.jastaddparser.ast.ASTNode;
import org.jastadd.jastaddparser.ast.Grammar;
import org.jastadd.jastaddparser.ast.Rule;

public class Main {
  public static void main(String[] args) {
    try {
      boolean noBeaverSymbol = false;
      boolean useTokenlist = false;
	  boolean patternGrammar = false;
	  boolean pep = false;
      if (args[0].equals("--version")) {
        System.out.println("JastAddParser version " + versionString());
        System.exit(0);
      } else if (args[0].equals("--no-beaver-symbol")) {
        noBeaverSymbol = true;
      } else if (args[0].equals("--tokenlist")) {
		noBeaverSymbol = true;
		useTokenlist = true;
      } else if (args[0].equals("--pattern_grammar")) {
		patternGrammar = true;
	  } else if (args[0].equals("--pep")) {
		pep = true;
	  }
      if (args.length > 2 && !noBeaverSymbol && !patternGrammar && !pep) {
        System.err.println("Unrecognized option \"" + args[0] + '\"');
        System.exit(1);
      }
      int sourceIndex = args.length == 3 ? 1 : 0;
      int destIndex = args.length == 3 ? 2 : 1;
      String source = args[sourceIndex];
      String dest = args[destIndex];
      File sourceFile = new File(source);
      File destFile = new File(dest);
      if (false && sourceFile.exists() && destFile.exists()
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
		if (pep) {
		  root.removeOpt();
		  root.oneRule();
		  MetaRuleSolver s = new MetaRuleSolver(root);

		  for (Rule r : s.solve()) {
			  System.out.println("MV: " + r.getIdDecl().getID());
		  }

		  root.addPatternGrammarClauses();
		  // root.removeRedundantMetaVars();
		  root.genPEP(out);
		  out.flush();
		}
		if (patternGrammar) {
		  root.addPatternGrammarClauses();
		}
		if (!pep) {
		  root.genCode(out, noBeaverSymbol,useTokenlist);
		}
        out.close();
        System.err.println("Parser specification " + dest + " generated from " + source);
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
      InputStream in = Main.class.getResourceAsStream("/Version.properties");
      props.load(in);
      version = props.getProperty("version");
    } catch (Exception e) {
      System.err.println("Could not retrieve version information");
      System.exit(1);
    }
    return version;
  }
}
