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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jastadd.jastaddparser.ast.ASTNode;
import org.jastadd.jastaddparser.ast.Grammar;
import org.jastadd.jastaddparser.parser.GrammarParser;
import org.jastadd.jastaddparser.parser.GrammarScanner;


public class Main {
  public static void main(String[] args) {
    try {
      boolean noBeaverSymbol = false;
      boolean useTokenlist = false;
      boolean patternGrammar = false;
      boolean sep = false;
      boolean nullSemanticAction = false;

      String source = null;
      String dest = null;
      Set<String> explicitMetaVarSymbols = null;

      for (int i = 0; i < args.length; ++i) {
        if (args[i].startsWith("--")) {
          // flag argument
          switch (args[i]) {
          case "--version":
            System.out.println("JastAddParser version " + versionString());
            System.exit(0);
            break;
          case "--no-beaver-symbol":
            noBeaverSymbol = true;
            break;
          case "--tokenlist":
            noBeaverSymbol = true;
            useTokenlist = true;
            break;
          case "--sep-null-action":
            sep = true;
            nullSemanticAction = true;
            break;
          case "--sep":
            sep = true;
            break;
          default:
            if (args[i].startsWith("--pattern_grammar")) {
              patternGrammar = true;
              sep = true;
              int eqIdx = args[i].indexOf("=");
              if (eqIdx >= 0) {
                // explicit non-terminals for metavariables
                String[] nonTerms = args[i].substring(eqIdx + 1).split(":");
                explicitMetaVarSymbols = new HashSet<String>(List.of(nonTerms));
              }
            } else {
              System.err.println("Unrecognized option \"" + args[i] + '\"');
              System.exit(1);
            }
            break;
          }
        } else {
          if (source == null) {
            source = args[i];
          } else if (dest == null) {
            dest = args[i];
          } else {
            System.err.println("Unrecognized option \"" + args[i] + '\"');
            System.exit(1);
          }
        }
      }

      if (source == null || dest == null) {
        System.err.println("Unknown source or destination files");
        System.exit(1);
      }

      File sourceFile = new File(source);
      File destFile = new File(dest);
      if (false && sourceFile.exists() && destFile.exists()
          && sourceFile.lastModified() < destFile.lastModified()) {
        System.out.println("Parser specification " + dest + " need not be regenerated");
      } else {
        ASTNode.sourceName = source;
        GrammarScanner scanner = new GrammarScanner(new FileReader(source));
        GrammarParser parser = new GrammarParser();
        Object o = parser.parse(scanner);
        Grammar root = (Grammar) o;
        ArrayList<String> errors = new ArrayList<String>();
        ArrayList<String> warnings = new ArrayList<String>();
        root.errorCheck(errors, warnings);
        if (!errors.isEmpty()) {
          System.err.println("There were errors in " + source + ":");
          for (Iterator iter = errors.iterator(); iter.hasNext(); )
            System.err.println(iter.next());
          System.exit(1);
        }
        for (Iterator iter = warnings.iterator(); iter.hasNext(); )
          System.err.println(iter.next());
        FileOutputStream os = new FileOutputStream(dest);
        PrintStream out = new PrintStream(os);
        if (sep) {
          root.removeOpt();
          root.oneRule();
          if (patternGrammar) {
              root.addPatternGrammarClauses(explicitMetaVarSymbols);
          }
          root.genSEP(out, patternGrammar, nullSemanticAction);
          out.flush();
        } else {
            if (patternGrammar) {
                root.addPatternGrammarClauses(explicitMetaVarSymbols);
            }
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
