package small.scanner;

import beaver.Symbol;
import beaver.Scanner;
import small.parser.TestParser.Terminals;

%%

%public 
%final 
%class TestScanner
%extends Scanner

%type Symbol 
%function nextToken 
%yylexthrow Scanner.Exception

%eofval{
	return sym(Terminals.EOF, "EOF");
%eofval}

%unicode
%line %column

%{
  StringBuffer strbuf = new StringBuffer(128);
  int sub_line;
  int sub_column;
  int strlit_start_line, strlit_start_column;

  private Symbol sym(short id) {
    return new Symbol(id, yyline + 1, yycolumn + 1, len(), str());
  }

  private Symbol sym(short id, String value) {
    return new Symbol(id, yyline + 1, yycolumn + 1, len(), value);
  }

  private String str() { return yytext(); }
  private int len() { return yylength(); }

  private void error(String msg) throws Scanner.Exception {
    throw new Scanner.Exception(yyline + 1, yycolumn + 1, msg);
  }
%}

WhiteSpace = [ ] | \t | \f | \r | \n | \r\n

DecimalNumber = [0-9]* {DecimalPart}
       | [0-9]+

DecimalPart = \. [0-9]*

Identifier = [A-Za-z] [A-Za-z0-9]*

%%

<YYINITIAL> {
	     {WhiteSpace}	{}
	     "+"	     { return sym(Terminals.PLUS); }
	     "-"	     { return sym(Terminals.MINUS); }
	     "*"	     { return sym(Terminals.MULT); }
	     "/"	     { return sym(Terminals.DIV); }
	     "-"	     { return sym(Terminals.MINUS); }
	     "("	     { return sym(Terminals.LPAREN); }
	     ")"	     { return sym(Terminals.RPAREN); }
	     {DecimalNumber} { return sym(Terminals.NUMBER); }
	     {Identifier}    { return sym(Terminals.IDENTIFIER); }
	     . | \n		{ error("Error: " + str()); }
}
