package compiler.phase.seman;

import java.util.*;

import compiler.common.report.*;
import compiler.phase.abstr.*;

/**
 * Type resolver.
 * 
 * Sets the isType attribute:
 * {@link compiler.phase.seman.SemAn#isType}
 * 
 * Formally, resolves the rules TYP:2 and TYP:11-19.
 * 
 * Forbids such types:
 * 
 * typ a = a
 * 
 * and
 * 
 * typ a = (b : b)
 * typ b = (a : a)
 * 
 * but allows
 * 
 * typ a = (b : ^b)
 * typ b = (a : ^a).
 * 
 * Should do two passes.
 * 
 * The first pass is for
 * - set nametype to a promise
 * 
 * The second pass is for
 * - resolve every type
 * - check if nametype promise delivered
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 * 
 * 
 * For a single node, should return the istype of the type, in the second pass for sure.
 */
public class TypeResolver implements AST.FullVisitor<TYP.Type, TypeResolver.Mode> {

	/** Constructs a new name resolver. */
	public TypeResolver() {
	}

    protected enum Mode {
		FIRST,
		SECOND
	}

	private Set<Integer> circularRecursionTracker = new HashSet<Integer>();
	private boolean inLegalRecursiveType = false;

    @Override
	public TYP.Type visit(AST.Nodes< ? extends AST.Node> nodes, Mode mode) {
		// This gets called first.
        // It also gets called for ParDefns etc... todo check
		if (mode == null) {
			visit(nodes, Mode.FIRST);
			visit(nodes, Mode.SECOND);
		} else {
			for (final AST.Node node : nodes)
				if ((node != null) || (!compiler.Compiler.devMode()))
					node.accept(this, mode);
		}
		return null;
	}

	
	@Override
	public TYP.Type visit(AST.TypDefn typDefn, Mode mode) {
		TYP.Type defnType;
		switch (mode) {
			case Mode.FIRST:
				if ((typDefn.type != null) || (!compiler.Compiler.devMode())) {
					/*
					defnType = typDefn.type.accept(this, mode);
					SemAn.isType.put(typDefn, defnType);
					return defnType;
					*/
					return typDefn.type.accept(this, mode);

				}
			case Mode.SECOND:
				if ((typDefn.type != null) || (!compiler.Compiler.devMode())) {
					defnType = typDefn.type.accept(this, mode);
					TYP.NameType defnNameType = new TYP.NameType(typDefn.name); 
					defnNameType.setActType(defnType);
					SemAn.isType.put(typDefn, defnNameType);
					return defnNameType;
					//return SemAn.isType.get(typDefn);
					/*
					return typDefn.type.accept(this, mode);
					defnType = typDefn.type.accept(this, mode);
					SemAn.isType.put(typDefn, defnType);
					return defnType;
					*/
				}
			default:
				throw new Report.InternalError();
		}
	}
	
	// TYP:11-14
	@Override
	public TYP.Type visit(AST.AtomType atomType, Mode mode) {
		switch (mode) {
			case Mode.FIRST:
				TYP.Type type;
				switch (atomType.type) {
					case AST.AtomType.Type.INT:
						type = TYP.IntType.type;
						break;
					case AST.AtomType.Type.CHAR:
						type = TYP.CharType.type;
						break;
					case AST.AtomType.Type.BOOL:
						type = TYP.BoolType.type;
						break;
					case AST.AtomType.Type.VOID:
						type = TYP.VoidType.type;
						break;
					default:
						throw new Report.InternalError();
				}
				SemAn.isType.put(atomType, type);
				return type;
			case Mode.SECOND:
				return SemAn.isType.get(atomType);
			default:
				throw new Report.InternalError();
		}
	}

	// TYP:15
	@Override
	public TYP.Type visit(AST.PtrType ptrType, Mode mode) {
		switch (mode) {
			case Mode.FIRST:
				TYP.Type baseType = ptrType.baseType.accept(this, mode);
				if (baseType == TYP.VoidType.type)
					throw new Report.Error(ptrType, "Cannot have explicit void pointer.");
				TYP.PtrType type = new TYP.PtrType(baseType);
				SemAn.isType.put(ptrType, type);
				return type;
			case Mode.SECOND:
				// TODO: This may be a problem.
				inLegalRecursiveType = true;
				TYP.Type retval = ptrType.baseType.accept(this, mode);
				inLegalRecursiveType = false;
				return SemAn.isType.get(ptrType);
				//return SemAn.isType.get(ptrType);
			default:
				throw new Report.InternalError();
		}
	}

	// TYP:16
	@Override
	public TYP.Type visit(AST.ArrType arrType, Mode mode) {
		TYP.Type elemType;
		switch (mode) {
			case Mode.FIRST:
				elemType = arrType.elemType.accept(this, mode);
				Long numElems;
				try {
					numElems = Long.valueOf(arrType.numElems);
				} catch (NumberFormatException e) {
					throw new Report.Error(arrType, "Value out of bounds for int type.");
				}
				if (0 >= numElems)
					throw new Report.Error(arrType, "Size of array must be a positive value.");
				TYP.ArrType type = new TYP.ArrType(elemType, numElems);
				SemAn.isType.put(arrType, type);
				return type;
			case Mode.SECOND:
				//inLegalRecursiveType = true;
				elemType = arrType.elemType.accept(this, mode);	
				//inLegalRecursiveType = false;
				/*
				 * MOVE THIS TO TYPECHECKER
				 if (TypeChecker.equ(elemType, TYP.VoidType.type))
				 throw new Report.Error(arrType, "Cannot have array elements of type void.");
				 */
				return SemAn.isType.get(arrType);

			default:
				throw new Report.InternalError();
		}
	}

	// TYP:17
	@Override
	public TYP.Type visit(AST.StrType strType, Mode mode) {
		TYP.StrType type;
		switch (mode) {
			case Mode.FIRST:
				ArrayList<TYP.Type> compTypes = new ArrayList<TYP.Type>();
				for (final AST.CompDefn compDefn : strType.comps)
					compTypes.addLast(compDefn.type.accept(this, mode));
				type = new TYP.StrType(compTypes);
				SemAn.isType.put(strType, type);
				return type;
			case Mode.SECOND:
			/* 
			* 
			for (final AST.CompDefn compDefn : strType.comps) {
				TYP.Type compType = compDefn.type.accept(this, mode);
				//if (compType == TYP.VoidType.type)
				//	throw new Report.Error(compDefn, "Cannot have void type as record component.");
				//SemAn.ofType.put(compDefn, compType);
				//SemAn.isConst.put(compDefn, false);
				//SemAn.isAddr.put(compDefn, true);
			}
			*/
				strType.comps.accept(this, mode);
				return SemAn.isType.get(strType);
			default:
				throw new Report.InternalError();
		}
	}

	//TYP:18
	@Override
	public TYP.Type visit(AST.UniType uniType, Mode mode) {
		switch (mode) {
			case Mode.FIRST:
				ArrayList<TYP.Type> compTypes = new ArrayList<TYP.Type>();
				for (final AST.CompDefn compDefn : uniType.comps)
					compTypes.addLast(compDefn.type.accept(this, mode));
				TYP.UniType type = new TYP.UniType(compTypes);
				SemAn.isType.put(uniType, type);
				return type;
			case Mode.SECOND:
				/*
				for (final AST.CompDefn compDefn : uniType.comps) {
					TYP.Type compType = compDefn.type.accept(this, mode);
					//if (compType == TYP.VoidType.type)
					//	throw new Report.Error(compDefn, "Cannot have void type as record component.");
					//SemAn.ofType.put(compDefn, compType);
					//SemAn.isConst.put(compDefn, false);
					//SemAn.isAddr.put(compDefn, true);
				}
				*/
				//inLegalRecursiveType = true;
				uniType.comps.accept(this, mode);
				//inLegalRecursiveType = false;
				return SemAn.isType.get(uniType);
			default:
				throw new Report.InternalError();
		}
	}

	
	// TYP:19
	// It's enough to check the relevant conditions in TypeChecker, right?
	@Override
	public TYP.Type visit(AST.FunType funType, Mode mode) {
		switch (mode) {
			case Mode.FIRST:
			TYP.Type resType = funType.resType.accept(this, mode);
			ArrayList<TYP.Type> parTypes = new ArrayList<TYP.Type>();
			for (final AST.Type parType : funType.parTypes)
			parTypes.addLast(parType.accept(this, mode));
			TYP.FunType type = new TYP.FunType(parTypes, resType);
			SemAn.isType.put(funType, type);
			
			return type;
			case Mode.SECOND:
				//inLegalRecursiveType = true; // maybe??
				funType.resType.accept(this, mode);
				for (final AST.Type parType : funType.parTypes)
					parType.accept(this, mode);
				//inLegalRecursiveType = false; // maybe??
				return SemAn.isType.get(funType);
			default:
				throw new Report.InternalError();
		}
	}

	@Override
	public TYP.Type visit(AST.NameType nameType, Mode mode) {
		// TODO: fix bin_search_tree.
		TYP.NameType type;
		TYP.Type refType;
		switch (mode) {
			case Mode.FIRST:
				type = new TYP.NameType(nameType.name);
				SemAn.isType.put(nameType, type);
				//System.out.printf("put into isType under name %s the type %s\n", nameType.name, type);
				return type;
			case Mode.SECOND:
				type = (TYP.NameType) SemAn.isType.get(nameType);
				AST.Defn def = SemAn.defAt.get(nameType);
				if (def == null)
					throw new Report.Error(nameType, "Odd behaviour involving : " + nameType.name);
				//System.out.printf("Got def of type %s : %s %s %s\n", nameType.name, def, def.name, def.type);
				if (circularRecursionTracker.add(nameType.id) == false) {
					// We have come back to the same name we have already seen.
					// Maybe there was valid recursion along the way:
					if (inLegalRecursiveType) {
						circularRecursionTracker.remove(nameType.id);
						if (!SemAn.validForIsType.test(def))
							throw new Report.Error(def, "Cannot resolve type name: " + nameType.name);
						refType = SemAn.isType.get(def); 
						// Breaks structs if here. But causes stack overflow if not here.
						if (refType == null) 
							throw new Report.Error(def, "Cannot resolve type name (isType is null): " + nameType.name);	
						type.setActType(refType);
						return type;
					}
					// Else, tough luck.
					throw new Report.Error(nameType, "Circular recursion involving named type : " + nameType.name);
				}
				refType = def.accept(this, mode);

				circularRecursionTracker.remove(nameType.id);
				inLegalRecursiveType = false;
				
				//TBD to see if we need this
				//if (refType instanceof TYP.NameType) {
				//	refType = ((TYP.NameType)refType).type();
				//}
				
				type.setActType(refType);
				return type;
				//return SemAn.isType.get(nameType);
			default:
				throw new Report.InternalError();
		}
	}
}
