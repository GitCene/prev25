package compiler.phase.imcgen;

import java.util.HashMap;
import java.util.Vector;

//import javax.swing.RepaintManager;

import compiler.common.report.Report;
import compiler.phase.abstr.*;
import compiler.phase.abstr.AST.Nodes;
import compiler.phase.memory.*;
import compiler.phase.seman.SemAn;
import compiler.phase.seman.TYP;

// TODO: Sort out external functions, and function call offsets.
/**
 * Intermediate code generator.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class ImcGenerator implements AST.FullVisitor<IMC.Instr, Object> {

    private Vector<MEM.Frame> frameStack = new Vector<MEM.Frame>();
    // More unneeded duct tape
    private Vector<AST.DefFunDefn> defnStack = new Vector<AST.DefFunDefn>();

    private Long memSize(TYP.Type type) {
        type = type.actualType();
        //if (type instanceof TYP.ArrType || type instanceof TYP.RecType)
        //    return MemEvaluator.PTRSIZE;
        //else 
        
        return Memory.TYPsizes.get(type);
    }

    // SEM:31 ?
    @Override
    public IMC.Instr visit(AST.Nodes<? extends AST.Node> nodes, Object o) {
        for (final AST.Node node : nodes)
            if ((node != null) || (!compiler.Compiler.devMode()))
                node.accept(this, o);
        return null;
    }

    @Override
    public IMC.Instr visit(AST.LetStmt letStmt, Object o) {
        Vector<IMC.Stmt> stmtsCodeVector = new Vector<IMC.Stmt>();
        for (final AST.Defn defn : letStmt.defns) {
            defn.accept(this, o);
        }
        for (final AST.Stmt stmt : letStmt.stmts) {
            stmtsCodeVector.add((IMC.Stmt) stmt.accept(this, o));
        }
        IMC.STMTS stmtsCode = new IMC.STMTS(stmtsCodeVector);
        ImcGen.stmt.put(letStmt, stmtsCode);
        return stmtsCode;
    }

    @Override
    public IMC.Instr visit(AST.DefFunDefn defFunDefn, Object o) {
        // Put a body label on this function
        MEM.Label funBodyLabel = new MEM.Label();
        ImcGen.entryLabel.put(defFunDefn, funBodyLabel);

        // Put an exit label on this function 
        MEM.Label funExitLabel = new MEM.Label();
        ImcGen.exitLabel.put(defFunDefn, funExitLabel);

        // Generate code for contained statements
        this.frameStack.addFirst(Memory.frames.get(defFunDefn));
        this.defnStack.addFirst(defFunDefn);
        defFunDefn.stmts.accept(this, o);
        this.frameStack.removeFirst();
        this.defnStack.removeFirst();
        

        return null;
    }

    // Addresses

    // Exprs

    private Long evalChar(String value) {
        // Skip the opening and closing quote
        String raw = value.substring(1, value.length() - 1);
        raw = raw.replace("\\\\", "\\");
        raw = raw.replace("\\'", "'");
        if (raw.length() > 1)
        // It's an 0xAB escape char.
            return Long.parseLong(raw.substring(3), 16);
        else
            return (long) raw.charAt(0);
    }

    // SEM:6-10, 1?
    @Override
    public IMC.Instr visit(AST.AtomExpr atomExpr, Object o) {
        Long value;
        switch (atomExpr.type) {
            // SEM:10
            case AST.AtomExpr.Type.INT:
                try {
                    value = Long.valueOf(atomExpr.value);
                } catch (Exception e) {
                    throw new Report.Error("TODO: handle Int size exceptions (Java Long)");
                } 
                break;
        
            // SEM:7,8
            case AST.AtomExpr.Type.BOOL:
                // TODO: check where along the way this becomes 0/1
                if (atomExpr.value.equals("1"))
                    value = 1L;
                else if (atomExpr.value.equals("0"))
                    value = 0L;
                else
                    throw new Report.Error("Internal error: BOOL is smth other than true or false.");
                break;

            // SEM:9
            case AST.AtomExpr.Type.CHAR:
                value = evalChar(atomExpr.value);
                break;

            // SEM:6 because the only const ptr is null
            case AST.AtomExpr.Type.PTR:
                value = 0L;
                break;
            case AST.AtomExpr.Type.STR:
            // TODO:
                value = 69L;
                break;
            default:
                throw new Report.InternalError();
        }
        // Write cons
        IMC.CONST cons = new IMC.CONST(value);
        ImcGen.expr.put(atomExpr, cons);
        return cons;
    }

    // SEM:11,13
    @Override
    public IMC.Instr visit(AST.PfxExpr pfxExpr, Object o) {
        IMC.Expr subExprCode = (IMC.Expr) pfxExpr.subExpr.accept(this, o);
        IMC.Expr expr;
        switch (pfxExpr.oper) {
            case ADD:
                expr = subExprCode;
                break;
            case NOT:
                expr = new IMC.UNOP(IMC.UNOP.Oper.NOT, subExprCode);
                break;
            case PTR:
            // TODO
                try {
                    expr = ((IMC.MEM8) subExprCode).addr;                
                } catch (ClassCastException e) {
                    expr = ((IMC.MEM1) subExprCode).addr;
                } 
                break;
            case SUB:
                expr = new IMC.UNOP(IMC.UNOP.Oper.NEG, subExprCode);
                break;
            default:
                throw new Report.InternalError();
        }
        ImcGen.expr.put(pfxExpr, expr);
        return expr;
    }

    // SEM:12
    public IMC.Instr visit(AST.BinExpr binExpr, Object o) {
        IMC.Expr arg1 = (IMC.Expr) binExpr.fstExpr.accept(this, o);
        IMC.Expr arg2 = (IMC.Expr) binExpr.sndExpr.accept(this, o);
        IMC.Expr expr;
        switch (binExpr.oper) {
            case ADD:
                expr = new IMC.BINOP(IMC.BINOP.Oper.ADD, arg1, arg2);
                break;
            case SUB:
                expr = new IMC.BINOP(IMC.BINOP.Oper.SUB, arg1, arg2);
                break;
            case MUL:
                expr = new IMC.BINOP(IMC.BINOP.Oper.MUL, arg1, arg2);
                break;
            case DIV:
                expr = new IMC.BINOP(IMC.BINOP.Oper.DIV, arg1, arg2);
                break;
            case MOD:
                expr = new IMC.BINOP(IMC.BINOP.Oper.MOD, arg1, arg2);
                break;
            case AND:
                expr = new IMC.BINOP(IMC.BINOP.Oper.AND, arg1, arg2);
                break;
            case OR:
                expr = new IMC.BINOP(IMC.BINOP.Oper.OR, arg1, arg2);
                break;
            case EQU:
                expr = new IMC.BINOP(IMC.BINOP.Oper.EQU, arg1, arg2);
                break;
            case NEQ:
                expr = new IMC.BINOP(IMC.BINOP.Oper.NEQ, arg1, arg2);
                break;
            case LTH:
                expr = new IMC.BINOP(IMC.BINOP.Oper.LTH, arg1, arg2);
                break;
            case GTH:
                expr = new IMC.BINOP(IMC.BINOP.Oper.GTH, arg1, arg2);
                break;
            case LEQ:
                expr = new IMC.BINOP(IMC.BINOP.Oper.LEQ, arg1, arg2);
                break;
            case GEQ:
                expr = new IMC.BINOP(IMC.BINOP.Oper.GEQ, arg1, arg2);
                break;
            default:
                throw new Report.InternalError();
        }
        ImcGen.expr.put(binExpr, expr);
        return expr;
    }

    // SEM:14
    @Override
    public IMC.Instr visit(AST.SfxExpr sfxExpr, Object o) {
        IMC.Expr ptr = (IMC.Expr) sfxExpr.subExpr.accept(this, o);
        IMC.MEM8 mem = new IMC.MEM8(ptr);
        ImcGen.expr.put(sfxExpr, mem);
        return mem;
    }

    // SEM:15
    @Override
    public IMC.Instr visit(AST.NameExpr nameExpr, Object o) {
        // ExtVar is MEM;NAME
        // LocalVar is MEM; stack access
        // Par is ... 
        //System.out.print("Trying to get defAt of " + nameExpr + " " + nameExpr.name);
        AST.Defn nameDefn = SemAn.defAt.get(nameExpr);
        //System.out.println(" : success");

        if (nameDefn instanceof AST.FunDefn) {
            // TODO: how to get extFunDefn sorted?
            if (nameDefn instanceof AST.ExtFunDefn)
                return new IMC.NAME(new MEM.Label());
            // In this case, just return a NAME of the function.
            // Get the fun's frame and its label.
            MEM.Label label = Memory.frames.get(nameDefn).label;
            return new IMC.NAME(label);
        }

        //System.out.print("Trying to get memory access of " + nameDefn + " " + nameDefn.name);
        MEM.Access access = Memory.accesses.get(nameDefn);
        //System.out.println(" : success");
        IMC.Expr accessCode;
        if (access instanceof MEM.AbsAccess) {
            accessCode = new IMC.NAME(((MEM.AbsAccess)access).label);
        } else if (access instanceof MEM.RelAccess) {
            // Have to find the FP of proper function.
            // TODO: in Memory, set depth of record components to -1?
            MEM.RelAccess relAccess = (MEM.RelAccess) access;
            
            Long nameDepth = relAccess.depth;
            MEM.Frame frame = this.frameStack.getFirst();
            Long frameIndex = frame.depth + 1 - nameDepth;
            
            //System.out.printf("We have frameDepth %d and nameDepth %d so we need %d MEMs\n", frame.depth, nameDepth, frameIndex);
            //frame = this.frameStack.get(frameIndex.intValue());
            //assert nameDepth == frame.depth + 1;
            IMC.Expr FP = new IMC.TEMP(frame.FP);
            for (int i = 0; i < frameIndex; i++) {
                FP = new IMC.MEM8(FP);
            }
            //System.out.println("We got now: " + FP);
            // TODO: test if this method works as envisioned.

            //IMC.CONST FP = new IMC.CONST(0);
            IMC.CONST offset = new IMC.CONST(relAccess.offset);
            IMC.BINOP accessArithm = new IMC.BINOP(IMC.BINOP.Oper.ADD, FP, offset);
            //accessCode = new IMC.MEM8(accessArithm);
            accessCode = accessArithm;
        } else throw new Report.InternalError();

        // TODO: sometimes is a MEM8 too much. When NAME no, otherwise yes.
        // MEM8 or MEM1 depends on the type.
        IMC.Expr mem;
        TYP.Type type = SemAn.ofType.get(nameExpr);
        Long size = memSize(type);
        if (size == 1)
            mem = new IMC.MEM1(accessCode);
        else //if (size == 8) FALLBACK
            mem = new IMC.MEM8(accessCode);
        //else
        //    throw new Report.Error("Internal error: unknown size of a type for MEM instr.");
        ImcGen.expr.put(nameExpr, mem);
        return mem;
    }

    // SEM:17
    @Override
    public IMC.Instr visit(AST.ArrExpr arrExpr, Object o) {
        
        IMC.MEM8 addrMem = (IMC.MEM8) arrExpr.arrExpr.accept(this, o);
        IMC.Expr addr = addrMem.addr;

        TYP.Type type = SemAn.ofType.get(arrExpr);
        Long size = memSize(type);
        
        IMC.Expr index = (IMC.Expr) arrExpr.idx.accept(this, o);
        IMC.BINOP mul = new IMC.BINOP(IMC.BINOP.Oper.MUL, index, new IMC.CONST(size));
        IMC.BINOP add = new IMC.BINOP(IMC.BINOP.Oper.ADD, addr, mul);
    
        IMC.Expr mem;
        if (size == 1)
            mem = new IMC.MEM1(add);
        else //if (size == 8) FALLBACK
            mem = new IMC.MEM8(add);
        //else
        //    throw new Report.Error("Internal error: unknown size of a type for MEM instr.");
        
        ImcGen.expr.put(arrExpr, mem);
        return mem;
    }

    // SEM:18
    @Override
    public IMC.Instr visit(AST.CompExpr compExpr, Object o) {
        IMC.MEM8 addrMem = (IMC.MEM8) compExpr.recExpr.accept(this, o);
        IMC.Expr addr = addrMem.addr;

        AST.Defn compDefn = SemAn.defAt.get(compExpr); //for this reason, CompDefn gets a defAt.
        MEM.RelAccess access = (MEM.RelAccess) Memory.accesses.get(compDefn);

        IMC.BINOP add = new IMC.BINOP(IMC.BINOP.Oper.ADD, addr, new IMC.CONST(access.offset));
        
        TYP.Type type = SemAn.ofType.get(compExpr);
        // TODO: what happens here, when this comp is a large struct?
        // TODO: Sort out what happens, when structs are passed around...
        Long size = memSize(type);
        IMC.Expr mem;
        if (size == 1)
            mem = new IMC.MEM1(add);
        else //if (size == 8) FALLBACK
            mem = new IMC.MEM8(add);
        //else
            //throw new Report.Error("Internal error: unknown size of a type for MEM instr.");

        ImcGen.expr.put(compExpr, mem);
        return mem;
    }

    // SEM:19
    @Override
    public IMC.Instr visit (AST.CallExpr callExpr, Object o) {
        // TODO: what do we do with nonglobal functions?
        IMC.Expr name = (IMC.Expr) callExpr.funExpr.accept(this, o);
        Vector<Long> offs  = new Vector<Long>();
        Vector<IMC.Expr> args = new Vector<IMC.Expr>();
        //TODO: add the static link to arg!
        MEM.Frame currFrame = this.frameStack.getFirst();
        args.add(new IMC.TEMP(currFrame.FP));
        // offs.add + 8
        //TODO: we've already seen these types. There may already be a way to have them saved?
        // Otherwise, make a hashtable of a list of types of the params for each function...
        for (AST.Expr arg : callExpr.argExprs) {
            IMC.Expr argCode = (IMC.Expr) arg.accept(this, o);
            args.add(argCode);
            // Args aren't necessarily defAt. this is unnecessar?
            // TODO: sort out.
            //System.out.println("Trying to get defAt of arg " + arg);
            //MEM.Access argAddr = Memory.accesses.get(SemAn.defAt.get(arg));
            //offs.add(argCode.)
            // This is something onyl needded for later.s
            offs.add(0L);
        }
        IMC.CALL call = new IMC.CALL(name, offs, args);
        ImcGen.expr.put(callExpr, call);
        return call;
    }

    // SEM:20,21,22
    @Override
    public IMC.Instr visit(AST.CastExpr castExpr, Object o) {
        // TODO: Do with type equivalence? For aliases?
        TYP.Type type = SemAn.isType.get(castExpr.type);
        IMC.Expr subexpr = (IMC.Expr) castExpr.expr.accept(this, o);
        IMC.Expr expr;
        if (type instanceof TYP.BoolType) {
            expr = new IMC.BINOP(IMC.BINOP.Oper.MOD, subexpr, new IMC.CONST(2));
        } else if (type instanceof TYP.CharType) {
            expr = new IMC.BINOP(IMC.BINOP.Oper.MOD, subexpr, new IMC.CONST(256));
        } else {
            expr = subexpr;
        }
        ImcGen.expr.put(castExpr, expr);
        return expr;
    }





    // Stmts

    // SEM:23
    @Override
    public IMC.Instr visit(AST.ExprStmt exprStmt, Object o) {
        IMC.ESTMT estmt = new IMC.ESTMT((IMC.Expr)exprStmt.expr.accept(this, o));
        ImcGen.stmt.put(exprStmt, estmt);
        return estmt;
    }

    // SEM:24
    @Override
    public IMC.Instr visit(AST.AssignStmt assignStmt, Object o) {
        // Move src expr into dst expr.
        IMC.Expr dst = (IMC.Expr) assignStmt.dstExpr.accept(this, o);
        IMC.Expr src = (IMC.Expr) assignStmt.srcExpr.accept(this, o);
        
        IMC.MOVE mov = new IMC.MOVE(dst, src);
        ImcGen.stmt.put(assignStmt, mov);
        return mov;
    }

    // SEM:25,26
    @Override
    public IMC.Instr visit(AST.IfThenStmt ifThenStmt, Object o) {
        // Evaluate cond and do CJUMP to appropriate label.
        // TODO: refactor so that false code is in normal block and the then-code is in the jumpout block.
        IMC.Expr cond = (IMC.Expr) ifThenStmt.condExpr.accept(this, o);
        
        IMC.LABEL ifTrueLabel = new IMC.LABEL(new MEM.Label());
        IMC.LABEL ifFalseLabel = new IMC.LABEL(new MEM.Label());
        
        IMC.NAME ifTrueName = new IMC.NAME(ifTrueLabel.label);
        IMC.NAME ifFalseName = new IMC.NAME(ifFalseLabel.label);

        IMC.CJUMP cjump = new IMC.CJUMP(cond, ifTrueName, ifFalseName);
        
        Vector<IMC.Stmt> thenStmtsCodeVector = new Vector<IMC.Stmt>();
        for (AST.Stmt stmt : ifThenStmt.thenStmt) {
            thenStmtsCodeVector.add((IMC.Stmt) stmt.accept(this, o));
        }
        IMC.STMTS thenStmtsCode = new IMC.STMTS(thenStmtsCodeVector);

        IMC.JUMP jumpAfterThen = new IMC.JUMP(ifFalseName);

        Vector<IMC.Stmt> ifThenCodeVector = new Vector<IMC.Stmt>();
        ifThenCodeVector.add(cjump);
        ifThenCodeVector.add(ifTrueLabel);
        ifThenCodeVector.add(thenStmtsCode);
        ifThenCodeVector.add(jumpAfterThen);
        ifThenCodeVector.add(ifFalseLabel);
        IMC.STMTS ifThenCode = new IMC.STMTS(ifThenCodeVector);
        ImcGen.stmt.put(ifThenStmt, ifThenCode);
        return ifThenCode;
    }

    // SEM:27,28
    public IMC.Instr visit(AST.IfThenElseStmt ifThenElseStmt, Object o) {
        IMC.Expr cond = (IMC.Expr) ifThenElseStmt.condExpr.accept(this, o);
   
        IMC.LABEL ifTrueLabel = new IMC.LABEL(new MEM.Label());
        IMC.LABEL ifFalseLabel = new IMC.LABEL(new MEM.Label());
        IMC.LABEL continueLabel = new IMC.LABEL(new MEM.Label());

        IMC.NAME ifTrueName = new IMC.NAME(ifTrueLabel.label);
        IMC.NAME ifFalseName = new IMC.NAME(ifFalseLabel.label);
        IMC.NAME continueName = new IMC.NAME(continueLabel.label);
        
        IMC.CJUMP cjump = new IMC.CJUMP(cond, ifTrueName, ifFalseName);

        Vector<IMC.Stmt> thenStmtsCodeVector = new Vector<IMC.Stmt>();
        for (AST.Stmt stmt : ifThenElseStmt.thenStmt) {
            thenStmtsCodeVector.add((IMC.Stmt) stmt.accept(this, o));
        }
        IMC.STMTS thenStmtsCode = new IMC.STMTS(thenStmtsCodeVector);

        Vector<IMC.Stmt> elseStmtsCodeVector = new Vector<IMC.Stmt>();
        for (AST.Stmt stmt : ifThenElseStmt.elseStmt) {
            elseStmtsCodeVector.add((IMC.Stmt) stmt.accept(this, o));
        }
        IMC.STMTS elseStmtsCode = new IMC.STMTS(elseStmtsCodeVector);

        IMC.JUMP jumpToContinue = new IMC.JUMP(continueName);

        Vector<IMC.Stmt> ifThenElseCodeVector = new Vector<IMC.Stmt>();
        ifThenElseCodeVector.add(cjump);
        ifThenElseCodeVector.add(ifTrueLabel);
        ifThenElseCodeVector.add(thenStmtsCode);
        ifThenElseCodeVector.add(jumpToContinue);
        ifThenElseCodeVector.add(ifFalseLabel);
        ifThenElseCodeVector.add(elseStmtsCode);
        ifThenElseCodeVector.add(jumpToContinue);
        ifThenElseCodeVector.add(continueLabel);
        
        IMC.STMTS ifThenElseCode = new IMC.STMTS(ifThenElseCodeVector);
        ImcGen.stmt.put(ifThenElseStmt, ifThenElseCode);
        return ifThenElseCode;
    }

    // SEM:29,30
    @Override
    public IMC.Instr visit(AST.WhileStmt whileStmt, Object o) {
        IMC.Expr cond = (IMC.Expr) whileStmt.condExpr.accept(this, o);
        
        IMC.LABEL condLabel = new IMC.LABEL(new MEM.Label());
        IMC.LABEL beginLabel = new IMC.LABEL(new MEM.Label());
        IMC.LABEL continueLabel = new IMC.LABEL(new MEM.Label());
        
        IMC.NAME condName = new IMC.NAME(condLabel.label);
        IMC.NAME beginName = new IMC.NAME(beginLabel.label);
        IMC.NAME continueName = new IMC.NAME(continueLabel.label);
        
        IMC.CJUMP cjump = new IMC.CJUMP(cond, beginName, continueName);
        
        Vector<IMC.Stmt> stmtsCodeVector = new Vector<IMC.Stmt>();
        for (AST.Stmt stmt : whileStmt.stmts) {
            stmtsCodeVector.add((IMC.Stmt) stmt.accept(this, o));
        }
        IMC.STMTS stmtsCode = new IMC.STMTS(stmtsCodeVector);

        IMC.JUMP jumpToCheck = new IMC.JUMP(condName);

        Vector<IMC.Stmt> whileCodeVector = new Vector<IMC.Stmt>();
        whileCodeVector.add(condLabel);
        whileCodeVector.add(cjump);
        whileCodeVector.add(beginLabel);
        whileCodeVector.add(stmtsCode);
        whileCodeVector.add(jumpToCheck);
        whileCodeVector.add(continueLabel);
        
        IMC.STMTS whileCode = new IMC.STMTS(whileCodeVector);
        ImcGen.stmt.put(whileStmt, whileCode);
        return whileCode;
    }

    // SEM:32
    @Override
    public IMC.Instr visit(AST.ReturnStmt returnStmt, Object o) {
        // Move the return value into a register, and jump back.
        MEM.Frame currFrame = this.frameStack.getFirst();
        
        IMC.Expr retval = (IMC.Expr) returnStmt.retExpr.accept(this, o);
        IMC.MOVE mov = new IMC.MOVE(new IMC.TEMP(currFrame.RV), retval);

        // Jump to the epilogue.
        AST.DefFunDefn currDefFunDefn = this.defnStack.getFirst();
        IMC.JUMP jump = new IMC.JUMP( new IMC.NAME(ImcGen.exitLabel.get(currDefFunDefn)));

        Vector<IMC.Stmt> stmts = new Vector<IMC.Stmt>();
        stmts.add(mov);
        stmts.add(jump);
        
        IMC.STMTS ret = new IMC.STMTS(stmts);
        ImcGen.stmt.put(returnStmt, ret);
        return ret;
    }

}
