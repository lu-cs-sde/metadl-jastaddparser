package beaver_switch.opt_list.scanner;

import beaver.Symbol;
import beaver.Scanner;
import beaver_switch.opt_list.parser.TestParser.Terminals;

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
  private Symbol sym(short id) {
    return new Symbol(id, yyline + 1, yycolumn + 1, yylength(), yytext());
  }

  private Symbol sym(short id, String value) {
    return new Symbol(id, yyline + 1, yycolumn + 1, yylength(), value);
  }

  private void error(String msg) throws Scanner.Exception {
    throw new Scanner.Exception(yyline + 1, yycolumn + 1, msg);
  }
%}

WhiteSpace = [ ] | \t | \f | \r | \n | \r\n

%%

<YYINITIAL> {
	     {WhiteSpace}	{}
	     "T1"	     { return sym(Terminals.T1); }
	     "T2"	     { return sym(Terminals.T2); }
	     .           { error("Error: " + yytext()); }
}
