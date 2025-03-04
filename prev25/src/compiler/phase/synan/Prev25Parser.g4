parser grammar Prev25Parser;

@header {

	package compiler.phase.synan;
	
	import java.util.*;
	import compiler.common.report.*;
	import compiler.phase.lexan.*;

}

@members {

	private Location loc(Token tok) { return new Location((LexAn.LocLogToken)tok); }
	private Location loc(Token     tok1, Token     tok2) { return new Location((LexAn.LocLogToken)tok1, (LexAn.LocLogToken)tok2); }
	private Location loc(Token     tok1, Locatable loc2) { return new Location((LexAn.LocLogToken)tok1, loc2); }
	private Location loc(Locatable loc1, Token     tok2) { return new Location(loc1, (LexAn.LocLogToken)tok2); }
	private Location loc(Locatable loc1, Locatable loc2) { return new Location(loc1, loc2); }

}

options{
    tokenVocab=Prev25Lexer;
}

type
	: INT
	| BOOL 
	| CHAR 
	| VOID 
	| ID 
	| type_array
	| type_ptr
	| type_struct
	| type_union
	| type_fun
	;

type_array	: LBRACKET INTCONST RBRACKET type ;
type_ptr	: CARET type ;
type_struct	: LTHAN typed_id (COMMA typed_id)* GTHAN ;
type_union	: LBRACE typed_id (COMMA typed_id)* RBRACE ;
type_fun	: LPAREN (type)? (COMMA type)* RPAREN COLON type ;

defn
	: def_type 
	| def_var
	| decl_fun
	| def_fun
	;

def_type : TYP ID ASSIGN type ; //probably better za rocno catchanje exceptionov!
def_var	: VAR ID COLON type ;

typed_id	: ID COLON type ;
body	: statement (COMMA statement)*;
body_opt	: (statement)? (COMMA statement)*;

decl_fun	: FUN ID LPAREN (typed_id)? (COMMA typed_id)* RPAREN COLON type ;
def_fun 	: decl_fun ASSIGN body ;

statement
	: expr
	| stmt_assign
	| stmt_return
	| stmt_while
	| stmt_if
	| stmt_if_else
	| stmt_let
	;


stmt_assign	: expr ASSIGN expr ;
stmt_return	: RETURN expr ; 
stmt_while 	: WHILE expr DO body_opt END ;
stmt_if 	: IF expr THEN body_opt END;
stmt_if_else		: IF expr THEN body_opt ELSE body_opt END ;
stmt_let	: LET (defn)+ IN body END ;


expr
	: expr_or
	;

expr_or
	: expr_or PIPE expr_or
	| expr_and
	;

expr_and
	: expr_and AMPERSAND expr_and
	| expr_rel 
	;

expr_rel
	: expr_rel EQ expr_rel
	| expr_rel NEQ expr_rel
	| expr_rel LTHAN expr_rel
	| expr_rel GTHAN expr_rel
	| expr_rel LEQ expr_rel
	| expr_rel GEQ expr_rel
	| expr_add
	;

expr_add
	: expr_add PLUS expr_add
	| expr_add MINUS expr_add
	| expr_mult
	;

expr_mult
	: expr_mult ASTERISK expr_mult
	| expr_mult SLASH expr_mult
	| expr_mult PERCENT expr_mult
	| expr_prefix
	;

expr_prefix
	: PLUS expr_prefix
	| MINUS expr_prefix
	| BANG expr_prefix
	| CARET expr_prefix
	| expr_postfix
	;

expr_postfix
	: expr_postfix LPAREN (expr)? (COMMA expr)* RPAREN
	| expr_postfix LBRACKET expr RBRACKET
	| expr_postfix CARET
	| expr_postfix DOT ID
	| expr_const
	;

expr_const
	: INTCONST
	| CHARCONST
	| STRCONST
	| TRUE
	| FALSE
	| NULL
	| ID
	| SIZEOF type 
	| LPAREN expr RPAREN
	| LBRACE expr COLON type RBRACE
	;

source
	: defn+ EOF
	;
