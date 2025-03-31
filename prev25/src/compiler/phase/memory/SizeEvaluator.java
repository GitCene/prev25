package compiler.phase.memory;

import compiler.phase.seman.*;

/**
 * Computing sizes of data types.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class SizeEvaluator extends TYP.FullVisitor<Long, Object> {

	@Override
	public Long visit(TYP.VoidType voidType, Object arg) {
		return 0l;
	}

	@Override
	public Long visit(TYP.BoolType boolType, Object arg) {
		return 1l;
	}

	@Override
	public Long visit(TYP.CharType charType, Object arg) {
		return 1l;
	}

	@Override
	public Long visit(TYP.IntType intType, Object arg) {
		return 8l;
	}

	@Override
	public Long visit(TYP.ArrType arrType, Object arg) {
		return arrType.numElems * arrType.elemType.accept(this, null);
	}

	@Override
	public Long visit(TYP.PtrType ptrType, Object arg) {
		return 8l;
	}

	@Override
	public Long visit(TYP.StrType strType, Object arg) {
		long size = 0l;
		for (final TYP.Type compType : strType.compTypes) {
			final long compSize = compType.accept(this, arg);
			size += (compSize / 8l) * 8l + (compSize % 8l == 0 ? 0l : 8l);
		}
		return size;
	}

	@Override
	public Long visit(TYP.UniType uniType, Object arg) {
		long size = 0l;
		for (final TYP.Type compType : uniType.compTypes) {
			final long compSize = compType.accept(this, arg);
			size = Long.max(size, (compSize / 8l) * 8l + (compSize % 8l == 0 ? 0l : 8l));
		}
		return size;
	}

	@Override
	public Long visit(TYP.NameType nameType, Object arg) {
		return nameType.type().accept(this, arg);
	}

	@Override
	public Long visit(TYP.FunType funType, Object arg) {
		return 8l;
	}

}
