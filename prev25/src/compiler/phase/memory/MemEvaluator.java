package compiler.phase.memory;


import compiler.phase.abstr.*;
import compiler.phase.seman.*;

// TODO: Think about labels.
// I am labelling strings with _Si, should they be anonymous labels?
// Should my labels or abs/rel access be retooled?

/**
 * Computing memory layout: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 * 
 * We have static variables, and automatic variables.
 * Static variables are declared outside of any function, and have a fixed address.
 * Automatic variables are declared inside functions, and are put on the stack.
 * 
 * Stack frame for a function call:
 *
 * |  Local variables.. |
 * |********************|  ------------ SP
 * | Outgoing call args |       *
 * |--------------------|       *
 * |Prev. register state|       *
 * |--------------------|       *
 * |   Temp. reg vars   |       *
 * |--------------------|       *   one stack frame
 * |   Return address   |       *
 * |--------------------|       *
 * |    Previous FP     |       *
 * |--------------------|       *
 * |  Local variables   |       *
 * |********************|  ------------ FP
 * 
 * At the end of outgoing call args, there is SL : static link to the start (FP)
 * of the stack frame of the function wrapping the DEFINITION of the called function.
 * 
 * Convention: 
 * SP points to the last occupied location on top.
 * FP points to the first free location under the stack.
 * ...
 * [FP - 8] : local var 1;
 * [FP + 0] : SL
 * [FP + 8] : arg 1;
 * ... etc
 */
public class MemEvaluator implements AST.FullVisitor<Long, Long> { // <retval, arg>

    private static final long INTSIZE = 8L;
    private static final long CHARSIZE = 1L;
    private static final long BOOLSIZE = 1L;
    private static final long PTRSIZE = 8L; //TO BE DETERMINED
    private static final long WORDSIZE = 8L;
    
    private long depth = 0;
    private long paramBlockSize = 0L;

    private static long strCount = 0;
    
    public MemEvaluator() {
    }

    private static class TypeSizer extends TYP.FullVisitor<Long, Object> {
        @Override
		public Long visit(TYP.Types<? extends TYP.Type> types, Object arg) {
			Long size = 0L;
            for (TYP.Type type : types) {
                Long typeSize = type.accept(this, arg);
                // Respect alignment
                size += padded(typeSize); //+ pad(typeSize);
            }
			return size;
		}

		@Override
		public Long visit(TYP.IntType intType, Object arg) {
			return INTSIZE;
		}

		@Override
		public Long visit(TYP.CharType charType, Object arg) {
			return CHARSIZE;
		}

		@Override
		public Long visit(TYP.BoolType boolType, Object arg) {
			return BOOLSIZE;
		}

		@Override
		public Long visit(TYP.VoidType voidType, Object arg) {
			return PTRSIZE;
		}

		@Override
		public Long visit(TYP.ArrType arrType, Object arg) {
			Long elemSize = arrType.elemType.accept(this, arg);
            // Should we pad?
            Long size = arrType.numElems * elemSize;
            return size; // + pad(size); Probably should not pad due to arrays of arrays?
		}

		@Override
		public Long visit(TYP.PtrType ptrType, Object arg) {
			return PTRSIZE;
		}

		@Override
		public Long visit(TYP.StrType strType, Object arg) {
			return strType.compTypes.accept(this, arg);
		}

		@Override
		public Long visit(TYP.UniType uniType, Object arg) {
			//return uniType.compTypes.accept(this, arg);
            Long size = 0L;
            for (TYP.Type type : uniType.compTypes) {
                Long typeSize = type.accept(this, arg);
                size = Long.max(size, typeSize);
            }
            return size;

		}

		@Override
		public Long visit(TYP.NameType nameType, Object arg) {
            //System.out.println("You have visited a NameType with TypeSizer. This should not happen. Is .actualType() working?");
			//Let's put some duct tape on this
            return nameType.type().accept(this, null);
		}

		@Override
		public Long visit(TYP.FunType funType, Object arg) {
			// Idk?
            return PTRSIZE;
		}

    }

    private TypeSizer typeSizer = new TypeSizer();

    private static Long pad(Long size) {
        return 0L;
        //return (WORDSIZE - size % WORDSIZE) % WORDSIZE;
    }

    private static Long padded(Long size) {
        // Pad the size to nearest multiple of WORDSIZE.
        return size + ((WORDSIZE - size % WORDSIZE) % WORDSIZE);
    }

    @Override
	public Long visit(AST.Nodes< ? extends AST.Node> nodes, Long offset) {
		if (offset == null) {
			return visit(nodes, 0L);
		} else {
            Long size = 0L;
            // This causes structs to double in size?
			for (final AST.Node node : nodes)
				if ((node != null) || (!compiler.Compiler.devMode())) {
                    Long retval = node.accept(this, offset);
                    if (retval != null) {
                        // Respect alignment
                        //retval += pad(retval);
                        size += padded(retval);
                        offset += padded(retval);
                    }
                }
            return size;
        }   
	}

    // TODO : perhaps extend for a StrType too?
    @Override
    public Long visit(AST.UniType uniType, Long offset) {
        Long size = 0L;
        for (final AST.Node node : uniType.comps) {
            if ((node != null) || (!compiler.Compiler.devMode())) {
                Long retval = node.accept(this, offset);
                if (retval != null) {
                    //retval += pad(retval);
                    size = Long.max(size, retval);
                }
            }
        }
        return size;
    }

    // Should set Memory.frames attribute for a fun defn.
    @Override
    public Long visit(AST.ExtFunDefn extFunDefn, Long offset) {
        this.depth++;
        // Should here be a new label?
        extFunDefn.pars.accept(this, 0L);
        this.depth--;
        return 0L;
    }
    @Override
    public Long visit(AST.DefFunDefn defFunDefn, Long offset) {
     	Long size = 0L, locsSize = 0L, argsSize = 0L;
        final MEM.Label label; 
        if (this.depth == 0)
            label = new MEM.Label(defFunDefn.name);
		else
            label = new MEM.Label();
        this.depth++;
        
        // Don't forget to visit parameters!
        defFunDefn.pars.accept(this, 0L);
        // Compute the size of local variables.
        // Also compute the size of parameter block.
        // This is the MAX size for args needed by the functions called here.
        Long tempParamBlockSize = paramBlockSize;
        paramBlockSize = 0L;
        if ((defFunDefn.stmts != null) || (!compiler.Compiler.devMode())) {
            // Set offset to 0, now relative to this new function's frame.
                locsSize = defFunDefn.stmts.accept(this, 0L);
        }
        // the override for CallExpr SHOULD have set paramBlockSize to smth here.
        argsSize = paramBlockSize;
        paramBlockSize = tempParamBlockSize;

        // Increase argsSize by SL
        argsSize += PTRSIZE;

        // size = local vars + prev. FP + return address + (temp vars, registers) + (outargs + SL)
        size = locsSize      + PTRSIZE  + PTRSIZE                                 + argsSize;
        
        // depth ++ for vars inside the function, while the function is still of depth depth
        Memory.frames.put(defFunDefn, new MEM.Frame(label, this.depth-1, locsSize, argsSize, size));
        this.depth--;

        return 0L;
    }

    // Should return the size of enclosed var defns
    // + potential enclosed Let stmts.
    @Override
    public Long visit(AST.LetStmt letStmt, Long offset) {
        Long defnsSize = letStmt.defns.accept(this, offset);
        Long stmtsSize = letStmt.stmts.accept(this, offset + defnsSize);
        return defnsSize + stmtsSize;
    }

    // Could contain let statements.
    @Override
    public Long visit(AST.IfThenElseStmt ifThenElseStmt, Long offset) {
        Long thenSize = ifThenElseStmt.thenStmt.accept(this, offset);
        Long elseSize = ifThenElseStmt.elseStmt.accept(this, offset);
        if (thenSize == null) thenSize = 0L;
        if (elseSize == null) elseSize = 0L;
            return Math.max(thenSize, elseSize);
    }
    @Override
    public Long visit(AST.IfThenStmt ifThenStmt, Long offset) {
            return ifThenStmt.thenStmt.accept(this, offset);
    }
    @Override
    public Long visit(AST.WhileStmt whileStmt, Long offset) {
        return whileStmt.stmts.accept(this, offset);
    }

    // Here, we start to set Memory.accesses.
    @Override
    public Long visit(AST.VarDefn varDefn, Long offset) {
        // Go deeper?
        varDefn.type.accept(this, 0L);

        // Get the checked type of this var
        TYP.Type type = SemAn.ofType.get(varDefn).actualType();
        Long size = type.accept(typeSizer, null);
        //size += pad(size);
        // Set the access
        MEM.Label label;
        MEM.Access access;
        if (this.depth == 0) {
            label = new MEM.Label(varDefn.name);
            access = new MEM.AbsAccess(size, label);
        } else {
            label = new MEM.Label();
            access = new MEM.RelAccess(size, -(offset+padded(size)), this.depth);
        }
        Memory.accesses.put(varDefn, access);
        return size;
    }

    @Override
    public Long visit(AST.ParDefn parDefn, Long offset) {
        // Go deeper?
        parDefn.type.accept(this, 0L);

        TYP.Type type = SemAn.ofType.get(parDefn).actualType();
        Long size = type.accept(typeSizer, null);
        //I think this is all right?
        MEM.RelAccess access = new MEM.RelAccess(size, offset+padded(size), depth);
        Memory.accesses.put(parDefn, access);
        return size;
    }

    @Override
    public Long visit(AST.CompDefn compDefn, Long offset) {
        // Go deeper?
        compDefn.type.accept(this, 0L);

        TYP.Type type = SemAn.ofType.get(compDefn);
        Long size = type.accept(typeSizer, null);
        //size += pad(size);
        //I guess this isn't quite working
        MEM.RelAccess access = new MEM.RelAccess(size, offset, -1);
        Memory.accesses.put(compDefn, access);
        return size;
    }

    // Set the max param size for calling frame.
    @Override
    public Long visit(AST.CallExpr callExpr, Long offset) {
        // Go deeper?
        callExpr.funExpr.accept(this, 0L);
        callExpr.argExprs.accept(this, 0L);
        Long size = 0L;
        for (AST.Expr argExpr : callExpr.argExprs) {
            //Long retsize = argExpr.t.accept(this, offset);
            //if (retsize != null) {
            //    size += padded(retsize);
            //    offset += padded(retsize);
            //}
            Long argSize = SemAn.ofType.get(argExpr).accept(typeSizer, null);
            size += padded(argSize);
        }
        /*
         // Not ok, we don't want the return value's type.
         TYP.Type type = SemAn.ofType.get(callExpr).actualType();
         System.out.println(type);
         Long size = type.accept(typeSizer, null);
         //size += pad(size);
         */
        if (size > this.paramBlockSize)
            paramBlockSize = size;
        return 0L;
    }

    // Strings.
    @Override
    public Long visit(AST.AtomExpr expr, Long offset) {
        if (expr.type == AST.AtomExpr.Type.STR) {
            // Should strings be named labels?
            MEM.Label label = new MEM.Label("S" + strCount);
            strCount++;
            // Skip the opening and closing quote
            String raw = expr.value.substring(1, expr.value.length() - 1);
            raw = raw.replace("\\\"", "\"");
            String sizecopy = raw.replaceAll("\\\\0x[0-9A-F][0-9A-F]", "|");
            raw = raw.replace("\\\\", "\\");
            sizecopy = sizecopy.replace("\\\\", "\\");
            // Calculate size accounting for \0xXX and for terminating null byte
            Long size = (long) sizecopy.length() + 1;

            MEM.AbsAccess access = new MEM.AbsAccess(size, label, raw);
            Memory.strings.put(expr, access);
        }
        return 0L;
    }
}
