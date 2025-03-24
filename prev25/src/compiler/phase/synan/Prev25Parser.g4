parser grammar Prev25Parser;

@header {

	package compiler.phase.synan;
	
	import java.util.*;
	import compiler.common.report.*;
	import compiler.phase.lexan.*;
	import compiler.phase.abstr.*;

}

@members {

	private Location loc(Token tok) { return new Location((LexAn.LocLogToken)tok); }
	private Location loc(Token     tok1, Token     tok2) { return new Location((LexAn.LocLogToken)tok1, (LexAn.LocLogToken)tok2); }
	private Location loc(Token     tok1, Locatable loc2) { return new Location((LexAn.LocLogToken)tok1, loc2); }
	private Location loc(Locatable loc1, Token     tok2) { return new Location(loc1, (LexAn.LocLogToken)tok2); }
	private Location loc(Locatable loc1, Locatable loc2) { return new Location(loc1, loc2); }


	// Todo: make a PartDefnDummy

	private class FunDefnDummy {
		private Locatable location;
		private String name;
		private List<AST.ParDefn> pars;
		private AST.Type type;

		public FunDefnDummy(Locatable location, String name, List<AST.ParDefn> pars, AST.Type type) {
			this.location = location;
			this.name = name;
			this.pars = pars;
			this.type = type;
		}

		public AST.ExtFunDefn toExtFunDefn() {
			return new AST.ExtFunDefn(this.location, this.name, this.pars, this.type);
		}

		// Todo: replace 'this.location' with 'loc(this.location, stmts)'
		public AST.DefFunDefn toDefFunDefn(List<AST.Stmt> stmts) {
			return new AST.DefFunDefn(this.location, this.name, this.pars, this.type, stmts);
		}
	}
}

options{
    tokenVocab=Prev25Lexer;
}

type
	returns [AST.Type ast]
	: type_atom { $ast = $type_atom.ast; }
	| ID { $ast = new AST.NameType(loc($ID), $ID.getText()); }
	| CARET type { $ast = new AST.PtrType(loc($CARET, $type.ast), $type.ast); }
	| LBRACKET INTCONST RBRACKET type { $ast = new AST.ArrType(	loc($LBRACKET, $type.ast), 
																$type.ast, 
																$INTCONST.getText()
	);}
	| LPAREN types_opt RPAREN COLON type { $ast = new AST.FunType(	loc($LPAREN, $type.ast),
																	$types_opt.ast,
																	$type.ast																	
	);}
	| type_record { $ast = $type_record.ast; }
	;

type_atom 
	returns [AST.AtomType ast]
	: INT { $ast = new AST.AtomType(loc($INT), AST.AtomType.Type.INT); }
	| BOOL { $ast = new AST.AtomType(loc($BOOL), AST.AtomType.Type.BOOL); }
	| CHAR { $ast = new AST.AtomType(loc($CHAR), AST.AtomType.Type.CHAR); }
	| VOID { $ast = new AST.AtomType(loc($VOID), AST.AtomType.Type.VOID); }
	;

type_record
	returns [AST.RecType ast]
	: LTHAN typed_ids GTHAN { $ast = new AST.StrType(loc($LTHAN, $GTHAN), $typed_ids.ast); }
	| LBRACE typed_ids RBRACE { $ast = new AST.UniType(loc($LBRACE, $RBRACE), $typed_ids.ast); }
	;

types
	returns [List<AST.Type> ast]
	: type { $ast = new ArrayList<AST.Type>(); $ast.addLast($type.ast); }
	| oldtypes = types COMMA type { $ast = $oldtypes.ast; $ast.addLast($type.ast); }
	;

types_opt
	returns [List<AST.Type> ast]
	: types	{ $ast = $types.ast; }
	| 		{ $ast = new ArrayList<AST.Type>(); }
	;

typed_id
	returns [AST.CompDefn ast]	
	: ID COLON type { $ast = new AST.CompDefn(loc($ID, $type.ast), $ID.getText(), $type.ast); }
	;
typed_ids
	returns [List<AST.CompDefn> ast]
	: typed_id { $ast = new ArrayList<AST.CompDefn>(); $ast.addLast($typed_id.ast); }
	| oldids = typed_ids COMMA typed_id  { $ast = $oldids.ast; $ast.addLast($typed_id.ast); }
	;

// Same thing as typed_id but returns ParDefn instead of CompDefn.
param
	returns [AST.ParDefn ast]
	: ID COLON type { $ast = new AST.ParDefn(loc($ID, $type.ast), $ID.getText(), $type.ast); }
	;
params
	returns [List<AST.ParDefn> ast]
	: param	{ $ast = new ArrayList<AST.ParDefn>(); $ast.addLast($param.ast); }
	| oldparams = params COMMA param  { $ast = $oldparams.ast; $ast.addLast($param.ast); }
	;
params_opt
	returns [List<AST.ParDefn> ast]
	: params	{ $ast = $params.ast; }
	|			{ $ast = new ArrayList<AST.ParDefn>(); }
	;

defs 
	returns [List<AST.FullDefn> ast]
	: def { $ast = new ArrayList<AST.FullDefn>(); $ast.addLast($def.ast); }
	| olddefs = defs def { $ast = $olddefs.ast; $ast.addLast($def.ast); }
	;

decl_fun
	returns [FunDefnDummy dummy] 	
	: FUN ID LPAREN params_opt RPAREN COLON type { $dummy = new FunDefnDummy(	loc($FUN, $type.ast),
																				$ID.getText(),
																				$params_opt.ast,
																				$type.ast		
	);}
	;

def
	returns [AST.FullDefn ast]
	: TYP ID ASSIGN type { $ast = new AST.TypDefn(	loc($TYP, $type.ast), 
													$ID.getText(), 
													$type.ast
	);}
	| VAR ID COLON type { $ast = new AST.VarDefn( 	loc($VAR, $type.ast),
													$ID.getText(),
													$type.ast 
	);}
	| decl_fun { $ast = $decl_fun.dummy.toExtFunDefn(); }
	| decl_fun ASSIGN body { $ast = $decl_fun.dummy.toDefFunDefn($body.ast); }
	;

body
	returns [List<AST.Stmt> ast]
	: statement { $ast = new ArrayList<AST.Stmt>(); $ast.addLast($statement.ast); }
	| oldbody = body COMMA statement { $ast = $oldbody.ast; $ast.addLast($statement.ast); }
	;
body_opt	
	returns [List<AST.Stmt> ast]
	: body	{ $ast = $body.ast; }
	|		{ $ast = new ArrayList<AST.Stmt>(); }
	;

statement
	returns [AST.Stmt ast]
	: expr { $ast = new AST.ExprStmt($expr.ast, $expr.ast); }
	| dst=expr ASSIGN src=expr { $ast = new AST.AssignStmt(loc($dst.ast, $src.ast), $dst.ast, $src.ast); }
	| RETURN expr { $ast = new AST.ReturnStmt(loc($RETURN, $expr.ast), $expr.ast); }
	| WHILE expr DO body_opt END { $ast = new AST.WhileStmt(loc($WHILE, $END), $expr.ast, $body_opt.ast); }
	| IF expr THEN body_opt END { $ast = new AST.IfThenStmt(loc($IF, $END), $expr.ast, $body_opt.ast); }
	| IF expr THEN body1=body_opt ELSE body2=body_opt END { $ast = new AST.IfThenElseStmt(loc($IF, $END), $expr.ast, $body1.ast, $body2.ast); }
	| LET defs IN body END { $ast = new AST.LetStmt(loc($LET, $END), $defs.ast, $body.ast); }
	;


expr
	returns [AST.Expr ast]
	: expr_or { $ast = $expr_or.ast; }
	;

expr_or
	returns [AST.Expr ast]
	: op1=expr_or PIPE op2=expr_or { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.OR, $op1.ast, $op2.ast); }
	| expr_and { $ast = $expr_and.ast; }
	;

expr_and
	returns [AST.Expr ast]
	: op1=expr_and AMPERSAND op2=expr_and { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.AND, $op1.ast, $op2.ast); }
	| expr_rel { $ast = $expr_rel.ast; }
	;

expr_rel
	returns [AST.Expr ast]
	: op1=expr_add EQ op2=expr_add { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.EQU, $op1.ast, $op2.ast); }
	| op1=expr_add NEQ op2=expr_add { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.NEQ, $op1.ast, $op2.ast); }
	| op1=expr_add LTHAN op2=expr_add { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.LTH, $op1.ast, $op2.ast); }
	| op1=expr_add GTHAN op2=expr_add { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.GTH, $op1.ast, $op2.ast); }
	| op1=expr_add LEQ op2=expr_add { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.LEQ, $op1.ast, $op2.ast); }
	| op1=expr_add GEQ op2=expr_add { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.GEQ, $op1.ast, $op2.ast); }
	| expr_add { $ast = $expr_add.ast; }
	;

expr_add
	returns [AST.Expr ast]
	: op1=expr_add PLUS op2=expr_add { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.ADD, $op1.ast, $op2.ast); }
	| op1=expr_add MINUS op2=expr_add { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.SUB, $op1.ast, $op2.ast); }
	| expr_mult { $ast = $expr_mult.ast; }
	;

expr_mult
	returns [AST.Expr ast]
	: op1=expr_mult ASTERISK op2=expr_mult { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.MUL, $op1.ast, $op2.ast); }
	| op1=expr_mult SLASH op2=expr_mult { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.DIV, $op1.ast, $op2.ast); }
	| op1=expr_mult PERCENT op2=expr_mult { $ast = new AST.BinExpr(loc($op1.ast, $op2.ast), AST.BinExpr.Oper.MOD, $op1.ast, $op2.ast); }
	| expr_prefix { $ast = $expr_prefix.ast; }
	;

expr_prefix
	returns [AST.Expr ast]
	: PLUS op=expr_prefix { $ast = new AST.PfxExpr(loc($PLUS, $op.ast), AST.PfxExpr.Oper.ADD, $op.ast); }
	| MINUS op=expr_prefix { $ast = new AST.PfxExpr(loc($MINUS, $op.ast), AST.PfxExpr.Oper.SUB, $op.ast); }
	| BANG op=expr_prefix { $ast = new AST.PfxExpr(loc($BANG, $op.ast), AST.PfxExpr.Oper.NOT, $op.ast); }
	| CARET op=expr_prefix { $ast = new AST.PfxExpr(loc($CARET, $op.ast), AST.PfxExpr.Oper.PTR, $op.ast); }
	| expr_postfix { $ast = $expr_postfix.ast; }
	;

args
	returns [List<AST.Expr> ast]
	: expr { $ast = new ArrayList<AST.Expr>(); $ast.addLast($expr.ast); }
	| oldargs=args COMMA expr { $ast = $oldargs.ast; $ast.addLast($expr.ast); }
	;

args_opt
	returns [List<AST.Expr> ast]
	: args 	{ $ast = $args.ast; }
	|		{ $ast = new ArrayList<AST.Expr>(); }
	;

expr_postfix
	returns [AST.Expr ast]
	: op=expr_postfix LPAREN args_opt RPAREN { $ast = new AST.CallExpr(loc($op.ast, $RPAREN), $op.ast, $args_opt.ast); }
	| op=expr_postfix LBRACKET expr RBRACKET { $ast = new AST.ArrExpr(loc($op.ast, $RBRACKET), $op.ast, $expr.ast); }
	| op=expr_postfix CARET { $ast = new AST.SfxExpr(loc($op.ast, $CARET), AST.SfxExpr.Oper.PTR, $op.ast); }
	| op=expr_postfix DOT ID { $ast = new AST.CompExpr(loc($op.ast, $ID), $op.ast, $ID.getText()); }
	| expr_atom { $ast = $expr_atom.ast; }
	;

expr_atom
	returns [AST.Expr ast]
	// Question: when is a constant of ptr type? (AST.AtomExpr.Type.PTR) I suppose null?
	: INTCONST { $ast = new AST.AtomExpr(loc($INTCONST), AST.AtomExpr.Type.INT, $INTCONST.getText()); }
	| CHARCONST { $ast = new AST.AtomExpr(loc($CHARCONST), AST.AtomExpr.Type.CHAR, $CHARCONST.getText()); }
	| STRCONST { $ast = new AST.AtomExpr(loc($STRCONST), AST.AtomExpr.Type.STR, $STRCONST.getText()); }
	| TRUE { $ast = new AST.AtomExpr(loc($TRUE), AST.AtomExpr.Type.BOOL, "1"); }
	| FALSE { $ast = new AST.AtomExpr(loc($FALSE), AST.AtomExpr.Type.BOOL, "0"); }
	| NULL { $ast = new AST.AtomExpr(loc($NULL), AST.AtomExpr.Type.PTR, "0"); }
	| ID { $ast = new AST.NameExpr(loc($ID), $ID.getText()); }
	| SIZEOF type { $ast = new AST.SizeExpr(loc($SIZEOF, $type.ast), $type.ast); }
	// TODO: Think about location here where we lose the parentheses.
	| LPAREN expr RPAREN { $ast = $expr.ast; $ast.relocate(loc($LPAREN, $RPAREN)); }
	| LBRACE expr COLON type RBRACE { $ast = new AST.CastExpr(loc($LBRACE, $RBRACE), $type.ast, $expr.ast); }
	;

source
	returns [AST.Nodes<AST.FullDefn> ast]
	: defs EOF
		{ $ast = new AST.Nodes<AST.FullDefn>($defs.ast); }
	;
