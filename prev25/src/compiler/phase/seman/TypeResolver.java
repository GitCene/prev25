package compiler.phase.seman;

import java.util.*;

import compiler.common.report.*;
import compiler.phase.abstr.*;

/**
 * Type resolver.
 *  moji zapiski gone :(((
 * 
 * sets the isType attribute for type definitions.
 * Should resolve ALL isType values properly.
 * 
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
// implements .. <returntype, argument>
public class TypeResolver implements AST.FullVisitor<TYP.Type, TypeResolver.Mode> {

	/** Constructs a new name resolver. */
	public TypeResolver() {
	}

	protected enum Mode {
		FIRST,
		SECOND
	}

	private Set<Integer> circularRecursionTracker = new HashSet<Integer>();

	@Override
	public TYP.Type visit(AST.Nodes< ? extends AST.Node> nodes, Mode mode) {
		// This gets called first.
		if (mode == null) {
			System.out.println("Mode first");
			visit(nodes, Mode.FIRST);
			System.out.println("Mode second");
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
					return typDefn.type.accept(this, mode);
				}
			case Mode.SECOND:
				if ((typDefn.type != null) || (!compiler.Compiler.devMode())) {
					defnType = typDefn.type.accept(this, mode);
					SemAn.isType.put(typDefn, defnType);
					return defnType;
				}
			default:
				throw new Report.InternalError();
		}
	}

	@Override
	public TYP.Type visit(AST.AtomType atomType, Mode mode) {
		switch (mode) {
			case Mode.FIRST:
				TYP.Type type;
				switch (atomType.type) {
					case AST.AtomType.Type.BOOL:
						type = TYP.BoolType.type;
						break;
					case AST.AtomType.Type.CHAR:
						type = TYP.CharType.type;
						break;
					case AST.AtomType.Type.INT:
						type = TYP.IntType.type;
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

	@Override
	public TYP.Type visit(AST.ArrType arrType, Mode mode) {
		switch (mode) {
			case Mode.FIRST:
				TYP.Type elemType = arrType.elemType.accept(this, mode);
				Long numElems = Long.valueOf(arrType.numElems);
				TYP.ArrType type = new TYP.ArrType(elemType, numElems);
				SemAn.isType.put(arrType, type);
				return type;
			case Mode.SECOND:
				return SemAn.isType.get(arrType);
			default:
				throw new Report.InternalError();
		}
	}

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
				return SemAn.isType.get(funType);
			default:
				throw new Report.InternalError();
		}
	}

	@Override
	public TYP.Type visit(AST.PtrType ptrType, Mode mode) {
		switch (mode) {
			case Mode.FIRST:
				TYP.Type baseType = ptrType.baseType.accept(this, mode);
				TYP.PtrType type = new TYP.PtrType(baseType);
				SemAn.isType.put(ptrType, type);
				return type;
			case Mode.SECOND:
				return SemAn.isType.get(ptrType);
			default:
				throw new Report.InternalError();
		}
	}

	@Override
	public TYP.Type visit(AST.NameType nameType, Mode mode) {
		TYP.NameType type;
		switch (mode) {
			case Mode.FIRST:
				type = new TYP.NameType(nameType.name);
				SemAn.isType.put(nameType, type);
				return type;
			case Mode.SECOND:
				type = (TYP.NameType) SemAn.isType.get(nameType);
				AST.Defn def = SemAn.defAt.get(nameType);
				if (circularRecursionTracker.add(nameType.id) == false)
					throw new Report.Error(nameType, "Circular recursion involving named type : " + nameType.name);
				TYP.Type refType = def.accept(this, mode);
				circularRecursionTracker.remove(nameType.id);
				/*
				
				if (refType instanceof TYP.NameType) {
					refType = ((TYP.NameType)refType).type();
				}
				*/
				type.setActType(refType);
				return type;
			default:
				throw new Report.InternalError();
		}
	}

	// TODO: add handling these nodes in the NameResolver.
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
				for (final AST.CompDefn compDefn : strType.comps)
					compDefn.type.accept(this, mode);
				return SemAn.isType.get(strType);
			default:
				throw new Report.InternalError();
		}
	}
	
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
				for (final AST.CompDefn compDefn : uniType.comps)
					compDefn.type.accept(this, mode);
				return SemAn.isType.get(uniType);
			default:
				throw new Report.InternalError();
		}
	}
}
