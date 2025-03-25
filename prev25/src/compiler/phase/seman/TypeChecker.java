package compiler.phase.seman;

import java.util.*;
import java.util.jar.Attributes.Name;

import compiler.common.report.*;
import compiler.phase.abstr.*;
import compiler.phase.abstr.AST.NameType;

/**
 * Type checker.
 * 
 * sets the ofType attribute.
 * override for all AST.Expr and AST.Defn w/o AST.TypDefn nodes.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 * todo: remove s.o.p. in TYP.java and SemAn.java
 * TODO: resolve duplicate names in structs?
 * 
 */
public class TypeChecker implements AST.FullVisitor<TYP.Type, AST.Node> {

	/** Constructs a new name checker. */
	public TypeChecker() {
	}

	public static typeEquiv EQU = new typeEquiv();

	// Represents the structural equivalence of types relation.
	public static boolean equ(TYP.Type type1, TYP.Type type2) {
		return type1.accept(EQU, type2) || type2.accept(EQU, type1);
		// Hope that this doesn't expl0de
	}

	private static class typeEquiv extends TYP.FullVisitor<Boolean, TYP.Type> {
		
		public typeEquiv() {
		}
		
		// EQU:1
		@Override
		public Boolean visit(TYP.IntType intType, TYP.Type type) {
			return intType == type;
		}

		@Override
		public Boolean visit(TYP.CharType charType, TYP.Type type) {
			return charType == type;
		}

		@Override
		public Boolean visit(TYP.BoolType boolType, TYP.Type type) {
			return boolType == type;
		}

		@Override
		public Boolean visit(TYP.VoidType voidType, TYP.Type type) {
			return voidType == type;
		}

		// EQU:2
		@Override
		public Boolean visit(TYP.NameType nameType, TYP.Type type) {
			//return nameType.type().accept(this, type); 
			return equ(nameType.type(), type);
		}

		@Override
		public Boolean visit(TYP.ArrType arrType, TYP.Type type) {
			if (type instanceof TYP.ArrType) {
				TYP.ArrType arrType_ = (TYP.ArrType) type;
				return arrType.numElems == arrType_.numElems &&
					equ(arrType.elemType, arrType_.elemType);	
				//arrType.elemType.accept(this, arrType_.elemType);
			}
			return false;
		}

		@Override
		public Boolean visit(TYP.PtrType ptrType, TYP.Type type) {
			if (type instanceof TYP.PtrType) {
				TYP.PtrType ptrType_ = (TYP.PtrType) type;
				return equ(ptrType.baseType, ptrType_.baseType);
				//return ptrType.baseType.accept(this, ptrType_.baseType);
			}
			return false;
		}

		@Override
		public Boolean visit(TYP.StrType strType, TYP.Type type) {
			if (type instanceof TYP.StrType) {
				TYP.StrType strType_ = (TYP.StrType) type;
				Iterator<TYP.Type> iter1 = strType.compTypes.iterator();
				Iterator<TYP.Type> iter2 = strType_.compTypes.iterator();
				while (iter1.hasNext() && iter2.hasNext()) {
					if (!equ(iter1.next(), iter2.next())) return false;
					//if (!iter1.next().accept(this, iter2.next())) return false;
				}
				return !(iter1.hasNext() || iter2.hasNext());
			}
			return false;
		}

		@Override
		public Boolean visit(TYP.UniType uniType, TYP.Type type) {
			if (type instanceof TYP.UniType) {
				TYP.UniType uniType_ = (TYP.UniType) type;
				Iterator<TYP.Type> iter1 = uniType.compTypes.iterator();
				Iterator<TYP.Type> iter2 = uniType_.compTypes.iterator();
				while (iter1.hasNext() && iter2.hasNext()) {
					if (!equ(iter1.next(), iter2.next())) return false;
					//if (!iter1.next().accept(this, iter2.next())) return false;
				}
				return !(iter1.hasNext() || iter2.hasNext());
			}
			return false;
		}

		@Override
		public Boolean visit(TYP.FunType funType, TYP.Type type) {
			if (type instanceof TYP.FunType) {
				TYP.FunType funType_ = (TYP.FunType) type;
				if (!funType.resType.accept(this, funType_.resType)) return false;
				Iterator<TYP.Type> iter1 = funType.parTypes.iterator();
				Iterator<TYP.Type> iter2 = funType_.parTypes.iterator();
				while (iter1.hasNext() && iter2.hasNext()) {
					if (!equ(iter1.next(), iter2.next())) return false;
					//if (!iter1.next().accept(this, iter2.next())) return false;
				}
				return !(iter1.hasNext() || iter2.hasNext());
			}
			return false;
		}
	}
	
	
	public static typeCoerce COE = new typeCoerce();
	
	// Represents the type coercion relation.
	// coe(T1, T2) : T1 ~> T2 := whether T1 can change into T2.
	public static boolean coe(TYP.Type type1, TYP.Type type2) {
		return type1.accept(COE, type2);
	}

	private static class typeCoerce extends TYP.FullVisitor<Boolean, TYP.Type> {

		// COE:1-4
		@Override
		public Boolean visit(TYP.IntType intType, TYP.Type type) {
			return equ(intType, type);
		}
		@Override
		public Boolean visit(TYP.CharType charType, TYP.Type type) {
			return equ(charType, type);
		}
		@Override
		public Boolean visit(TYP.BoolType boolType, TYP.Type type) {
			return equ(boolType, type);
		}
		@Override
		public Boolean visit(TYP.VoidType voidType, TYP.Type type) {
			return equ(voidType, type);
		}

		// COE:5,6
		@Override
		public Boolean visit(TYP.PtrType ptrType, TYP.Type type) {
			if (type instanceof TYP.PtrType) {
				TYP.PtrType ptrType_ = (TYP.PtrType) type;
				return coe(ptrType.baseType, ptrType_.baseType) || equ(ptrType.baseType, TYP.VoidType.type);
			}
			return false;
		}

		// COE:7
		@Override
		public Boolean visit(TYP.ArrType arrType, TYP.Type type) {
			if (type instanceof TYP.ArrType) {
				TYP.ArrType arrType_ = (TYP.ArrType) type;
				return arrType.numElems == arrType_.numElems && 
					coe(arrType.elemType, arrType_.elemType);
			}
			return false;
		}

		// COE:8
		@Override
		public Boolean visit(TYP.StrType strType, TYP.Type type) {
			if (type instanceof TYP.StrType) {
				TYP.StrType strType_ = (TYP.StrType) type;
				Iterator<TYP.Type> iter1 = strType.compTypes.iterator();
				Iterator<TYP.Type> iter2 = strType_.compTypes.iterator();
				while (iter1.hasNext() && iter2.hasNext()) {
					if (!coe(iter1.next(), iter2.next())) return false;
				}
				return !(iter1.hasNext() || iter2.hasNext());
			}
			return false;
		}

		// COE:9
		@Override
		public Boolean visit(TYP.UniType uniType, TYP.Type type) {
			if (type instanceof TYP.UniType) {
				TYP.UniType uniType_ = (TYP.UniType) type;
				Iterator<TYP.Type> iter1 = uniType.compTypes.iterator();
				Iterator<TYP.Type> iter2 = uniType_.compTypes.iterator();
				while (iter1.hasNext() && iter2.hasNext()) {
					if (!coe(iter1.next(), iter2.next())) return false;
				}
				return !(iter1.hasNext() || iter2.hasNext());
			}
			return false;
		}

		// COE:10
		@Override
		public Boolean visit(TYP.FunType funType, TYP.Type type) {
			if (type instanceof TYP.FunType) {
				TYP.FunType funType_ = (TYP.FunType) type;
				if (!coe(funType.resType, funType_.resType)) return false;
				Iterator<TYP.Type> iter1 = funType.parTypes.iterator();
				Iterator<TYP.Type> iter2 = funType_.parTypes.iterator();
				while (iter1.hasNext() && iter2.hasNext()) {
					if (!coe(iter1.next(), iter2.next())) return false;
				}
				return !(iter1.hasNext() || iter2.hasNext());
			}
			return false;
		}
		
		// COE:11
		@Override
		public Boolean visit(TYP.NameType nameType, TYP.Type type) {
			if (type instanceof TYP.NameType) {
				TYP.NameType nameType_ = (TYP.NameType) type;
				return nameType.name.equals(nameType_.name) && equ(nameType.type(), nameType_.type());
			}
			return false;
		}

	}

	private boolean legalTypeEquiv(TYP.Type type) {
		// Check if type is equivalent to a set of legal types for params?
		// TODO: testing with instanceof is not okay
		return equ(type, TYP.IntType.type) ||
			equ(type, TYP.CharType.type) ||
			equ(type, TYP.BoolType.type) ||
			equPtr(type) ||
			equFun(type) ||
			type instanceof TYP.NameType && legalTypeEquiv( ((TYP.NameType)type).type() );
			// I guess that's ok because it gets recursively checked inside anyway?
	}

	private boolean equPtr(TYP.Type type) {
		return type instanceof TYP.PtrType ||
			(type instanceof TYP.NameType && equPtr(((TYP.NameType)type).type())  );
	}

	private boolean equFun(TYP.Type type) {
		return type instanceof TYP.FunType ||
			(type instanceof TYP.NameType && equFun(((TYP.NameType)type).type())  );
	}

	private boolean equArr(TYP.Type type) {
		System.out.println("Checking array equ : " + type.toString());
		System.out.println(type instanceof TYP.ArrType);
		System.out.println(type instanceof TYP.NameType);
		
		return type instanceof TYP.ArrType ||
			(type instanceof TYP.NameType && equArr(((TYP.NameType)type).type())  );
	}

	private boolean equStr(TYP.Type type) {
		return type instanceof TYP.StrType ||
			(type instanceof TYP.NameType && equStr(((TYP.NameType)type).type())  );
	}

	private boolean equUni(TYP.Type type) {
		return type instanceof TYP.UniType ||
			(type instanceof TYP.NameType && equStr(((TYP.NameType)type).type())  );
	}

	private boolean legalTypeEquivVoid(TYP.Type type) {
		// Check if type is legal return value?
		return equ(type, TYP.VoidType.type) || legalTypeEquiv(type);
	}

	// TODO: Everywhere overriding VoidType with java's null works?
	// ---------- Program ---------- 
	// TYP:1
	@Override
	public TYP.Type visit(AST.Nodes<? extends AST.Node> nodes, AST.Node D) {
		//D is null only on toplevel.
		boolean mainFound = false;
		for (final AST.Node node : nodes) {
			System.out.print("Looking at node: ");
			System.out.println(node.location());
			if ((node != null) || (!compiler.Compiler.devMode())) {
				TYP.Type nodeType = node.accept(this, node);

				// IF on top level:
				if (D == null) {
					if (nodeType != TYP.VoidType.type)
						throw new Report.Error(node, "Malformed definition.");
					if (!mainFound && node instanceof AST.DefFunDefn) {
						AST.DefFunDefn funNode = (AST.DefFunDefn) node;
						if (funNode.type instanceof AST.AtomType) {
							AST.AtomType funNodeAtom = (AST.AtomType) funNode.type;
							
							mainFound = funNode.name.equals("main") && 
							funNode.pars.size() == 0 &&
							funNodeAtom.type == AST.AtomType.Type.INT;
						}
					}
				}
			}
		}
		if (!mainFound && D == null) throw new Report.Error("Function 'main' is malformed or missing.");
		return TYP.VoidType.type;
	}

	// ---------- Definitions ----------
	// TYP:2
	@Override
	public TYP.Type visit(AST.TypDefn typDefn, AST.Node D) {
		// Partly taken care of in TypeResolver.
		typDefn.type.accept(this, D);
		return TYP.VoidType.type;
	}

	// TYP:3
	@Override
	public TYP.Type visit(AST.VarDefn varDefn, AST.Node D) {
		TYP.Type varType = SemAn.isType.get(varDefn.type);
		// Should I be putting varDefn.name, or varDefn ?!?!? 
		SemAn.ofType.put(varDefn, varType);
		return TYP.VoidType.type;
	}

	// TYP:4
	@Override
	public TYP.Type visit(AST.ParDefn parDefn, AST.Node D) {
		TYP.Type parType = SemAn.isType.get(parDefn.type);
		SemAn.ofType.put(parDefn, parType);
		return parType;
	}
	
	@Override
	public TYP.Type visit(AST.DefFunDefn funDefn, AST.Node D) {
		return funVisitSubroutine(funDefn, D, true);
	}

	@Override
	public TYP.Type visit(AST.ExtFunDefn funDefn, AST.Node D) {
		return funVisitSubroutine(funDefn, D, false);
	}

	private TYP.Type funVisitSubroutine(AST.FunDefn funDefn, AST.Node D, boolean hasBody) {
		ArrayList<TYP.Type> parTypes = new ArrayList<TYP.Type>();
		for (final AST.ParDefn par : funDefn.pars) {
			TYP.Type parType = par.accept(this, D);
			if (!legalTypeEquiv(parType))
				throw new Report.Error(par, "Illegal parameter type : " + parType.toString());
			parTypes.addLast(parType);
		}
		TYP.Type resType = SemAn.isType.get(funDefn.type);
		if (!legalTypeEquivVoid(resType))
			throw new Report.Error(funDefn.type, "Illegal return type : " + resType.toString());
		
		if (hasBody) {
			AST.DefFunDefn defFunDefn = (AST.DefFunDefn) funDefn;
			for (final AST.Stmt stmt : defFunDefn.stmts) {
				TYP.Type stmtType = stmt.accept(this, defFunDefn);
				if (stmtType != TYP.VoidType.type) // && stmtType != null)
					throw new Report.Error(stmt, "Malformed statement. How?");
			}
		}
		TYP.FunType funType = new TYP.FunType(parTypes, resType);
		SemAn.ofType.put(funDefn, funType);
		return TYP.VoidType.type;
	}

	// ---------- Statements ----------
	// TYP:5
	@Override
	public TYP.Type visit(AST.AssignStmt assStmt, AST.Node D) {
		TYP.Type dstType = assStmt.dstExpr.accept(this, D);
		System.out.println("three acts: ");
		System.out.println(assStmt);
		System.out.println(assStmt.dstExpr);
		System.out.println(dstType);
		TYP.Type srcType = assStmt.srcExpr.accept(this, D);
		if (!legalTypeEquiv(dstType))
			throw new Report.Error(assStmt.dstExpr, "Illegal type : " + dstType.toString());
		if (!legalTypeEquiv(srcType))
			throw new Report.Error(assStmt.srcExpr, "Illegal type : " + srcType.toString());
		if (!coe(srcType, dstType)) 
			throw new Report.Error(assStmt, "Cannot coerce " + srcType.toString() + " to " + dstType.toString());
		return TYP.VoidType.type;
	}

	// TYP:6
	public TYP.Type visit(AST.ReturnStmt returnStmt, AST.Node D) {
		// You should check that return belongs to a function with proper form...
		AST.DefFunDefn parentFun = (AST.DefFunDefn) D;
		TYP.Type resType = SemAn.isType.get(parentFun.type);
		TYP.Type retExprType = returnStmt.retExpr.accept(this, D);
		// The resType has already been checked above.
		if (!legalTypeEquiv(retExprType)) 
			throw new Report.Error(returnStmt, "Illegal return type : " + retExprType.toString());
		if (!coe(retExprType, resType))
			throw new Report.Error(returnStmt, "Invalid return type : " + retExprType.toString());
		return TYP.VoidType.type;
	}

	// TYP:7
	public TYP.Type visit(AST.WhileStmt whileStmt, AST.Node D) {
		TYP.Type condType = whileStmt.condExpr.accept(this, D);
		if (!equ(condType, TYP.BoolType.type))
			throw new Report.Error(whileStmt.condExpr, "Invalid type of condition : " + condType.toString());
		for (final AST.Stmt stmt : whileStmt.stmts)
			if (stmt.accept(this, D) != TYP.VoidType.type)
				throw new Report.Error(stmt, "Invalid statement.");
		return TYP.VoidType.type;
	}

	// TYP:8
	public TYP.Type visit(AST.IfThenStmt ifStmt, AST.Node D) {
		TYP.Type condType = ifStmt.condExpr.accept(this, D);
		if (!equ(condType, TYP.BoolType.type))
			throw new Report.Error(ifStmt.condExpr, "Invalid type of condition : " + condType.toString());
		for (final AST.Stmt stmt : ifStmt.thenStmt)
			if (stmt.accept(this, D) != TYP.VoidType.type)
				throw new Report.Error(stmt, "Invalid statement.");
		return TYP.VoidType.type;
	}

	// TYP:9
	public TYP.Type visit(AST.IfThenElseStmt ifStmt, AST.Node D) {
		TYP.Type condType = ifStmt.condExpr.accept(this, D);
		if (!equ(condType, TYP.BoolType.type))
			throw new Report.Error(ifStmt.condExpr, "Invalid type of condition : " + condType.toString());
		for (final AST.Stmt stmt : ifStmt.thenStmt)
			if (stmt.accept(this, D) != TYP.VoidType.type)
				throw new Report.Error(stmt, "Invalid statement.");
		for (final AST.Stmt stmt : ifStmt.elseStmt)
			if (stmt.accept(this, D) != TYP.VoidType.type)
				throw new Report.Error(stmt, "Invalid statement.");
		return TYP.VoidType.type;
	}

	// TYP:10
	public TYP.Type visit(AST.LetStmt letStmt, AST.Node D) {
		for (final AST.Defn defn : letStmt.defns)
			if (defn.accept(this, D) != TYP.VoidType.type)
				throw new Report.Error(defn, "Invalid definition.");
		for (final AST.Stmt stmt : letStmt.stmts)
			if (stmt.accept(this, D) != TYP.VoidType.type)
				throw new Report.Error(stmt, "Invalid statement.");
		return TYP.VoidType.type;
	}

	// ---------- Types ----------
	// - Handled in TypeResolver (TYP:11-19).

	// In case we won't handle for records.
	@Override
	public TYP.Type visit(AST.CompDefn compDefn, AST.Node D) {
		TYP.Type compType = SemAn.isType.get(compDefn.type);
		if (compType == TYP.VoidType.type)
			throw new Report.Error(compDefn, "Cannot have void type as record component.");
		SemAn.ofType.put(compDefn, compType);
		return compType;
	}

	// ---------- Expressions ----------
	// TYP:20-25
	@Override
	public TYP.Type visit(AST.AtomExpr atomExpr, AST.Node D) {
		TYP.Type type;
		switch (atomExpr.type) {
			case AST.AtomExpr.Type.INT:
				type = TYP.IntType.type;
				Long intValue;
				try {
					intValue = Long.valueOf(atomExpr.value);
				} catch (NumberFormatException e) {
					throw new Report.Error(atomExpr, "Value out of bounds for int type.");
				}
				break;
			case AST.AtomExpr.Type.CHAR:
				type = TYP.CharType.type;
				//TODO: check that it is valid.
				break;
			case AST.AtomExpr.Type.BOOL:
				type = TYP.BoolType.type;
				// TODO : check val ...
				String value = atomExpr.value;
				break;
			case AST.AtomExpr.Type.STR:
				type = new TYP.PtrType(TYP.CharType.type);
				break;
			case AST.AtomExpr.Type.PTR:
				// Should be the only "constant of a pointer type" in atomexpr right?
				type = TYP.VoidType.type;
				break;
			default:
				throw new Report.InternalError();
		}
		
		SemAn.ofType.put(atomExpr, type);
		return type;
	}

	private void complain(Locatable location, TYP.Type offender) {
		throw new Report.Error(location, "Invalid operand type : " + offender.toString());
	}
	// TYP:26,27,
	@Override
	public TYP.Type visit(AST.PfxExpr pfxExpr, AST.Node D) {
		TYP.Type subType = pfxExpr.subExpr.accept(this, D);
		switch(pfxExpr.oper) {
			case AST.PfxExpr.Oper.ADD:
			case AST.PfxExpr.Oper.SUB:
				if (!equ(subType, TYP.IntType.type)) complain(pfxExpr, subType);
				SemAn.ofType.put(pfxExpr, subType);
				return subType;
			case AST.PfxExpr.Oper.NOT:
				if (!equ(subType, TYP.BoolType.type)) complain(pfxExpr, subType);
				SemAn.ofType.put(pfxExpr, subType);
				return subType;
			// TYP:35
			case AST.PfxExpr.Oper.PTR:
				if (equ(subType, TYP.VoidType.type))
					throw new Report.Error(pfxExpr.subExpr, "Cannot reference void type.");
				TYP.PtrType ptrType = new TYP.PtrType(subType);
				SemAn.ofType.put(pfxExpr, ptrType);
				return ptrType;
		}
		throw new Report.InternalError();
	}

	@Override
	public TYP.Type visit(AST.BinExpr binExpr, AST.Node D) {
		TYP.Type fstType = binExpr.fstExpr.accept(this, D);
		TYP.Type sndType = binExpr.sndExpr.accept(this, D);
		TYP.Type resType = null;
		switch (binExpr.oper) {
			// TYP:28,29
			case AST.BinExpr.Oper.AND:
			case AST.BinExpr.Oper.OR:
				if (!equ(fstType, TYP.BoolType.type)) complain(binExpr.fstExpr, fstType);
				if (!equ(sndType, TYP.BoolType.type)) complain(binExpr.sndExpr, sndType);
				if (coe(sndType, fstType))
					resType = fstType;
				else if (coe(fstType, sndType))
					resType = sndType;
				else 
					throw new Report.Error(binExpr, "Somehow couldn't coerce two boolean-y types.");
				break;
			// TYP:30,31
			case AST.BinExpr.Oper.MUL:
			case AST.BinExpr.Oper.DIV:
			case AST.BinExpr.Oper.MOD:
			case AST.BinExpr.Oper.ADD:
			case AST.BinExpr.Oper.SUB:
				if (!equ(fstType, TYP.IntType.type)) complain(binExpr.fstExpr, fstType);
				if (!equ(sndType, TYP.IntType.type)) complain(binExpr.sndExpr, sndType);
				if (coe(sndType, fstType))
					resType = fstType;
				else if (coe(fstType, sndType))
					resType = sndType;
				else 
					throw new Report.Error(binExpr, "Somehow couldn't coerce two int-y types.");
				break;
			// TYP:32
			case AST.BinExpr.Oper.EQU:	
			case AST.BinExpr.Oper.NEQ:	
			case AST.BinExpr.Oper.LTH:	
			case AST.BinExpr.Oper.GTH:	
			case AST.BinExpr.Oper.LEQ:	
			case AST.BinExpr.Oper.GEQ:
				if (!legalTypeEquiv(fstType)) complain(binExpr.fstExpr, fstType);
				if (!legalTypeEquiv(sndType)) complain(binExpr.sndExpr, sndType);
				if (coe(sndType, fstType) || coe(fstType, sndType))
					resType = TYP.BoolType.type;
				else 
					throw new Report.Error(binExpr, "Incomparable types : " + fstType.toString() + ", " + sndType.toString());
				break;
		}
		SemAn.ofType.put(binExpr, resType);
		return resType;
	}

	// TYP:33
	@Override
	public TYP.Type visit(AST.ArrExpr arrExpr, AST.Node D) {
		TYP.Type arrType = arrExpr.arrExpr.accept(this, D);
		// Weakly checking? i guess?
		if (!(equArr(arrType)))
			throw new Report.Error(arrExpr.arrExpr, "Invalid array type : " + arrType.toString());
		TYP.Type idxType = arrExpr.idx.accept(this, D);
		if (!equ(idxType, TYP.IntType.type))
			throw new Report.Error(arrExpr.idx, "Invalid type of indexing expression : " + idxType.toString());
		
		if (arrType instanceof TYP.NameType) {
			arrType = ((TYP.NameType)arrType).type();
		}
		TYP.ArrType arrExprType = (TYP.ArrType)arrType;
		SemAn.ofType.put(arrExpr, arrExprType.elemType);
		return arrExprType.elemType;
	}

	// TYP:34
	@Override
	public TYP.Type visit(AST.SfxExpr sfxExpr, AST.Node D) {
		TYP.Type subType = sfxExpr.subExpr.accept(this, D);
		switch(sfxExpr.oper) {
			case AST.SfxExpr.Oper.PTR:
				if (!(equPtr(subType))) complain(sfxExpr.subExpr, subType);
				if (subType instanceof TYP.NameType)
					subType = ((TYP.NameType)subType).type();
				TYP.PtrType subPointerType = (TYP.PtrType) subType;
				if (equ(subPointerType.baseType, TYP.VoidType.type))
					throw new Report.Error(sfxExpr.subExpr, "Cannot dereference void type.");
				
				SemAn.ofType.put(sfxExpr, subPointerType.baseType);
				return subPointerType.baseType;
		}
		throw new Report.InternalError();
	}

	// TYP:36,37
	@Override
	public TYP.Type visit(AST.CompExpr compExpr, AST.Node D) {
		//TODO: handle multilevel structs access.
		AST.NameExpr nameOfStructVar = (AST.NameExpr) (compExpr.recExpr);
		TYP.Type recType = compExpr.recExpr.accept(this, D);
		if (!(equStr(recType) || equUni(recType)))
			throw new Report.Error(compExpr, "Not a record type : " +  recType.toString());
		String fieldname = compExpr.name;
		// Go to the var defn of this struct variable...
		AST.VarDefn defnStructVar = (AST.VarDefn) SemAn.defAt.get(nameOfStructVar);
		// Look which type it is...
		AST.TypDefn defnStructType = (AST.TypDefn) SemAn.defAt.get(defnStructVar.type);
		// See that it has this field.
		AST.StrType strType = (AST.StrType) defnStructType.type;
		//AST.RecType recordType = (AST.RecType) recordDefn.type;
		for (final AST.CompDefn compDefn : strType.comps) {
			if (compDefn.name.equals(fieldname)) {
				TYP.Type exprType = SemAn.ofType.get(compDefn);
				SemAn.ofType.put(compExpr, exprType);
				return exprType;
			}
		}
		throw new Report.Error(compExpr, "Nonexistent field '" + fieldname + "' in record : " + recType.toString());
	}

	// TYP:38
	public TYP.Type visit(AST.CallExpr callExpr, AST.Node D) {
		TYP.Type funType = callExpr.funExpr.accept(this, D);
		if (!equFun(funType)) 
			throw new Report.Error(callExpr, "Not a callable type : " + funType.toString());
		if (funType instanceof TYP.NameType) {
			funType = ((TYP.NameType)funType).type();
		}
		TYP.FunType funFunType = (TYP.FunType) funType;
		Iterator<TYP.Type> iterPar = funFunType.parTypes.iterator();
		Iterator<AST.Expr> iterArg = callExpr.argExprs.iterator();
		while (iterPar.hasNext() && iterArg.hasNext()) {
			TYP.Type argType = iterArg.next().accept(this, D);
			TYP.Type parType = iterPar.next();
			if (!coe(argType, parType))
				throw new Report.Error(callExpr, "Argument types do not agree: " + argType.toString() + ", " + parType.toString());
		}
		if (iterPar.hasNext() || iterArg.hasNext())
			throw new Report.Error(callExpr, "Mismatched number of arguments in function call for type : " + funFunType.toString());
		TYP.Type resType = funFunType.resType;
		SemAn.ofType.put(callExpr, resType);
		return resType;
	}

	// TYP:39
	public TYP.Type visit(AST.SizeExpr sizeExpr, AST.Node D) {
		TYP.Type exprType = SemAn.isType.get(sizeExpr.type);
		if (equ(exprType, TYP.VoidType.type))
			throw new Report.Error(sizeExpr, "Cannot get size of void.");
		SemAn.ofType.put(sizeExpr, TYP.IntType.type);
		return TYP.IntType.type;
	}

	// TYP:40
	public TYP.Type visit(AST.CastExpr castExpr, AST.Node D) {
		TYP.Type destType = SemAn.isType.get(castExpr.type);
		if (equ(destType, TYP.VoidType.type))
			throw new Report.Error(castExpr, "Cannot cast to voidy type : " + destType.toString());
		TYP.Type exprType = castExpr.expr.accept(this, D);
		if (equ(exprType, TYP.VoidType.type))
			throw new Report.Error(castExpr, "Cannot cast expression of voidy type : " + exprType.toString());
		SemAn.ofType.put(castExpr, destType);
		return destType;
	}

	// TYP:41 - we do not have parentheses anymore.

	// How to deal with name expression?
	public TYP.Type visit(AST.NameExpr nameExpr, AST.Node D) {
		AST.Defn defnAt = SemAn.defAt.get(nameExpr);
		TYP.Type defnType = SemAn.ofType.get(defnAt);
		SemAn.ofType.put(nameExpr, defnType);
		return defnType;
	}

}
