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
		int ix = e.getStartIndex();
		// TODO: properly handle errors of unmatched multiline tokens.
		throw new Report.Error(new Location(getLine(), getCharPositionInLine()+1), "Unrecognizable symbol: " + e.getInputStream().getText(new Interval(ix, ix))) ;
	}
}
//


// 1. constants
INTCONST		: [0-9]+ ;
fragment CHARFRAG		: [ -&(-[\]-~] | '\\\'' | '\\\\' | '0x' [0-9A-F][0-9A-F] ;
CHARCONST		: '\''CHARFRAG'\'' ;
fragment STRFRAG		: [ !#-[\]-~] | '\\"' | '\\\\' | '\\0x' [0-9A-F][0-9A-F] ;
STRCONST		: '"'STRFRAG+'"' ;

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

