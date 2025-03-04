lexer grammar Prev25Lexer;

@header {
	package compiler.phase.lexan;

	import compiler.common.report.*;
}

@members {
    @Override
	public LexAn.LocLogToken nextToken() {
		return (LexAn.LocLogToken) super.nextToken();
	}

	@Override
	public void notifyListeners(LexerNoViableAltException e) {
		//throw new Report.Error("Unrecognizable symbol: " + e.toString() + " at " + getLine() + "." + (getCharPositionInLine()+1));
		//int ix = e.getStartIndex();

		CharStream offendingInput = e.getInputStream();
		int start = _tokenStartCharIndex;
		int stop = _input.index();

		String unmatched = offendingInput.getText(new Interval(start, stop));
		Location loc = new Location(getLine(), getCharPositionInLine()+1);
		System.out.println(e.getCtx());
		/*
		if (getType() == STRCONST) {
			throw new Report.Error(loc, "Invalid character in STRCONST: " + unmatched);
		} else {
			throw new Report.Error(loc, "Unrecognizable symbol: " + unmatched);
		}
		*/
		throw new Report.Error(loc, "Unrecognizable symbol: " + unmatched);
		//int diff = stop - start;
		//int locstartline = getLine();
		//throw new Report.Error(new Location(getLine(), getCharPositionInLine()+1), "Unrecognizable token: " + e.getInputStream().getText(new Interval(start, stop))) ;
	}
}
//


// 1. constants
INTCONST		: [0-9]+ ;

fragment CharFragChar	: [ -&(-[\]-~] ;
fragment EscBackslash	: '\\\\' ;
fragment EscSingleQuote	: '\\\'' ;
fragment Hex			: '0x' [0-9A-F][0-9A-F] ;
fragment CharDelim		: '\'' ;

fragment CharFrag 		: CharFragChar | EscBackslash | EscSingleQuote | Hex ;

CHARCONST		: CharDelim CharFrag CharDelim ;
//ERR_CHARCONST 	: CharDelim ~[CharFrag] CharDelim ;

fragment StrFragChar 	: [ !#-[\]-~] ;
fragment EscDoubleQuote	: '\\"' ;
fragment EscHex			: '\\' Hex ;
fragment StrDelim		: '"' ;

fragment StrFrag 		: StrFragChar | EscDoubleQuote | EscBackslash | EscHex ;

fragment ErrBadEscape	: '\\.' ;
fragment ErrBadHex		: '0x' ~[0-9A-F] | '0x' [0-9A-F] ~[0-9A-F]; 
fragment ErrBadEscHex	: '\\' ErrBadHex ;


STRCONST		: StrDelim StrFrag+ StrDelim ;
ERR_STRCONST_BADESCAPE	: StrDelim (StrFrag | ErrBadEscape | EscSingleQuote)+ StrDelim;
ERR_STRCONST_BADHEX		: StrDelim (StrFrag | ErrBadEscHex)+ StrDelim;

// 2. symbols

//relational
EQ				: '==' ;
NEQ				: '!=' ;
LTHAN			: '<' ;
GTHAN			: '>' ;
LEQ				: '<=' ;
GEQ				: '>=' ;

//binops
AMPERSAND		: '&' ;
PIPE			: '|' ;
ASTERISK		: '*' ;
SLASH			: '/' ;
PERCENT			: '%' ;
PLUS			: '+' ;
MINUS			: '-' ;
CARET			: '^' ;

//punct
BANG			: '!' ;
DOT   			: '.' ;
ASSIGN			: '=' ;
COLON			: ':' ;
COMMA			: ',' ;

//brackets
LBRACE			: '{' ;
RBRACE			: '}' ;
LPAREN			: '(' ;
RPAREN			: ')' ;
LBRACKET		: '[' ;
RBRACKET		: ']' ;

// 3. keywords
BOOL 			: 'bool' ;
CHAR			: 'char' ; 
DO 				: 'do' ;
ELSE			: 'else' ;
END				: 'end' ;
FALSE			: 'false' ;
FUN				: 'fun' ;
IF				: 'if' ;
IN				: 'in' ;
INT				: 'int' ;
LET				: 'let' ;
NULL			: 'null' ;
RETURN			: 'return' ;
SIZEOF			: 'sizeof' ;
THEN			: 'then' ;
TRUE			: 'true' ;
TYP				: 'typ' ;
VAR				: 'var' ;
VOID			: 'void' ;
WHILE			: 'while' ;

//4. identifiers
ID				: [a-zA-Z_] [a-zA-Z_0-9]* ;

//5. comments
COMMENT 		: '#' ~[\r\n]* '\r'? '\n' -> skip ;

WHITESPACE    	: [ \t\n\r]+ -> skip ;
