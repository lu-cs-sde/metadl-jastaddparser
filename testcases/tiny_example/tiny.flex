package tiny_example.scanner;

import beaver.Symbol;
import beaver.Scanner;
import tiny_example.parser.TestParser.Terminals;

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

TerminalName = [A-Za-z] [A-Za-z0-9]*

%%

<YYINITIAL> {
	    {WhiteSpace}	{}
	    {TerminalName}	{ return sym(Terminals.TERMINAL, yytext()); }
	    .				{ error("Error: " + yytext()); }
}
