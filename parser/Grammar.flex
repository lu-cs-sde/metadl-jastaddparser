/*
 * The JastAdd Extensible Java Compiler (http://jastadd.org) is covered
 * by the modified BSD License. You should have received a copy of the
 * modified BSD license with this compiler.
 * 
 * Copyright (c) 2005-2008, Torbjorn Ekman
 * All rights reserved.
 */


package parser;

import beaver.Symbol;
import beaver.Scanner;
import parser.GrammarParser.Terminals;

%%

%class GrammarScanner
%public
%extends Scanner
%{
	private int token_line;
	private int token_column;

	private String matched_text;

	private Symbol newSymbol(short id)
	{
		return new Symbol(id, yyline + 1, yycolumn + 1, yylength(), yytext());
	}

	private Symbol newSymbol(short id, Object value)
	{
		return new Symbol(id, yyline + 1, yycolumn + 1, yylength(), value);
	}
%}
%unicode
%line
%column
%function nextToken
%yylexthrow Scanner.Exception
%type Symbol
%eofval{
	return newSymbol(Terminals.EOF, "end-of-file");
%eofval}

LineTerminator = \r | \n | \r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

Comment = {TraditionalComment}
        | {EndOfLineComment}

TraditionalComment = "/*" [^*] ~"*/" | "/*" "*"+ "/" | "/*" "*"+ [^/*] ~"*/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}?

Identifier     = [:letter:] ([:letter:] | [:digit:] | "_")*

AnyChar        = . | \n

MyCode = "{:" [^:] ~":}" | "{:" + ":" "}"

%%

<YYINITIAL> {
  {WhiteSpace}        { /* ignore */ }
  {Comment}           { /* ignore */ }

	"%header"       { return newSymbol(Terminals.HEADER   ); }
	"%embed"       { return newSymbol(Terminals.EMBED   ); }
  "%goal"     { return newSymbol(Terminals.GOAL); }

//	","             { return newSymbol(Terminals.COMMA    ); }
	":="             { return newSymbol(Terminals.REPLACE       ); }
	"="             { return newSymbol(Terminals.IS       ); }
	";"             { return newSymbol(Terminals.SEMI     ); }

//	"@"             { return newSymbol(Terminals.AT       ); }
	"."             { return newSymbol(Terminals.DOT      ); }
	"|"             { return newSymbol(Terminals.BAR      ); }

	"?"             { return newSymbol(Terminals.QUESTION ); }
	"+"             { return newSymbol(Terminals.PLUS     ); }
	"*"             { return newSymbol(Terminals.STAR     ); }

	{MyCode}		{ String s = yytext().trim();
	                  s = s.substring(2, s.length()-2).trim();
	                  return newSymbol(Terminals.CODE, s);
	                }

	{Identifier}    { return newSymbol(Terminals.IDENT, yytext()); }
    {AnyChar}           { throw new Scanner.Exception(yyline + 1, yycolumn + 1, "unrecognized character '" + yytext() + "'"); }
}

