package alias03.scanner;

import beaver.Symbol;
import beaver.Scanner;
import alias03.parser.TestParser.Terminals;

%%

%public 
%final 
%class TestScanner
%extends Scanner

%type Symbol 
%function nextToken 
%yylexthrow Scanner.Exception

%eofval{
	return 	new Symbol(Terminals.EOF, yyline + 1, yycolumn + 1, yylength(), "EOF");
%eofval}

%unicode
%line %column

WhiteSpace = [ ] | \t | \f | \r | \n | \r\n

AnyWord = [A-Za-z]+

%%

<YYINITIAL> {
	{WhiteSpace}	{}
	{AnyWord}		{ return new Symbol(Terminals.TOKEN, yyline + 1, yycolumn + 1, yylength(), yytext()); }
	.				{ throw new Scanner.Exception(yyline + 1, yycolumn + 1, yytext()); }
}
