package compiler.phase.imclin;

import java.util.Vector;

import compiler.common.report.Report;
import compiler.phase.abstr.AST;
import compiler.phase.imcgen.IMC;
import compiler.phase.imcgen.IMC.*;
import compiler.phase.imcgen.ImcGen;
import compiler.phase.memory.MEM;
import compiler.phase.memory.Memory;

public class ChunkGenerator implements AST.FullVisitor<Object, Object> {

    private static class Linearizer implements IMC.Visitor<Expr, Vector<IMC.Stmt>> {
        // We can work directly with CONST and TEMP and LABEL and NAME.
        // Other things we have to MOVE to a TEMP.
        // Should return the Expr that is storing the value of the instruction.
        // TODO: somewhere have to take care of strings.

        private boolean isStore = false;

        @Override
        public Expr visit(BINOP binOp, Vector<IMC.Stmt> vec) {
			Expr arg1 = binOp.fstExpr.accept(this, vec);
			Expr arg2 = binOp.sndExpr.accept(this, vec);
            BINOP freshBinOp = new BINOP(binOp.oper, arg1, arg2);
            TEMP dst = new TEMP();
            MOVE mov = new MOVE(dst, freshBinOp);
            vec.add(mov);
            return dst;
        }

		@Override
        public Expr visit(CALL call, Vector<IMC.Stmt> vec) {
			// Nested calls need to be taken care of.
            // TODO: have an ESTMT -> CALL for the situation where call isn't nested.
            Expr addr = call.addr.accept(this, vec);
            // TODO: put the args properly.
            Vector<Expr> freshArgs = new Vector<Expr>();
            for (Expr arg : call.args) {
                Expr freshArg = arg.accept(this, vec);
                freshArgs.add(freshArg);
            }
            TEMP dst = new TEMP();
            
            // TODO: offs
            CALL freshCall = new CALL(addr, call.offs, freshArgs);
            MOVE mov = new MOVE(dst, freshCall);
            vec.add(mov);
            return dst;
		}

		@Override
        public Expr visit(CJUMP cjump, Vector<IMC.Stmt> vec) {
			Expr cond = cjump.cond.accept(this, vec);

            Expr pos = cjump.posAddr.accept(this, vec);
            Expr neg = cjump.negAddr.accept(this, vec);

            CJUMP freshCjump = new CJUMP(cond, pos, neg);
            vec.add(freshCjump);
            return null;
		}

		@Override
        public Expr visit(CONST constant, Vector<IMC.Stmt> vec) {
			return constant;
		}

		@Override
        public Expr visit(ESTMT eStmt, Vector<IMC.Stmt> vec) {
			eStmt.expr.accept(this, vec);
            return null;
		}

		@Override
        public Expr visit(JUMP jump, Vector<IMC.Stmt> vec) {
			Expr addr = jump.addr.accept(this, vec);
            JUMP freshJump = new JUMP(addr);
            vec.add(freshJump);
            return null;
		}

		@Override
        public Expr visit(LABEL label, Vector<IMC.Stmt> vec) {
			vec.add(label);
            return null;
		}

		@Override
        public Expr visit(MEM1 mem, Vector<IMC.Stmt> vec) {
            // MEM is sometimes a load, sometimes a store!
            // Hack for dealing with that
            boolean isStore = this.isStore;
            this.isStore = false;
			Expr addr = mem.addr.accept(this, vec);
            MEM1 freshMem = new MEM1(addr);

            if (isStore) {
                return freshMem;
            } else {
                TEMP dst = new TEMP();
                MOVE mov = new MOVE(dst, freshMem);
                vec.add(mov);
                return dst;
            }
		}

		@Override
        public Expr visit(MEM8 mem, Vector<IMC.Stmt> vec) {
            boolean isStore = this.isStore;
            this.isStore = false;
            Expr addr = mem.addr.accept(this, vec);
            MEM8 freshMem = new MEM8(addr);

            if (isStore) {
                return freshMem;
            } else {
                TEMP dst = new TEMP();
                MOVE mov = new MOVE(dst, freshMem);
                vec.add(mov);
                return dst;
            }
		}

		@Override
        public Expr visit(MOVE move, Vector<IMC.Stmt> vec) {
            // TODO: pazi, kdaj temp v nov temp.. ce jih funkcija modificira?
            if (move.dst instanceof MEM1 || move.dst instanceof MEM8)
                this.isStore = true;
            Expr dst = move.dst.accept(this, vec);
            this.isStore = false;
            Expr src = move.src.accept(this, vec);
            Stmt mov = new MOVE(dst, src);
            vec.add(mov);
			return dst;
		}

		@Override
        public Expr visit(NAME name, Vector<IMC.Stmt> vec) {
			return name;
		}

		@Override
        public Expr visit(SEXPR sExpr, Vector<IMC.Stmt> vec) {
            // Don't think I'm using this ever.
            // TODO: what?
			throw new Report.InternalError();
		}

		@Override
        public Expr visit(STMTS stmts, Vector<IMC.Stmt> vec) {
			for (Stmt stmt : stmts.stmts) {
                stmt.accept(this, vec);
            }
            return null;
		}

		@Override
        public Expr visit(TEMP temp, Vector<IMC.Stmt> vec) {
			return temp;
		}

		@Override
        public Expr visit(UNOP unOp, Vector<IMC.Stmt> vec) {
			Expr arg = unOp.subExpr.accept(this, vec);
            UNOP freshUnop = new UNOP(unOp.oper, arg);
            TEMP dst = new TEMP();
            MOVE mov = new MOVE(dst, freshUnop);
            vec.add(mov);
            return dst;
		}
    }
    
    private Linearizer linearizer = new Linearizer();

    @Override
    public Object visit(AST.Nodes<? extends AST.Node> nodes, Object o) {
        for (final AST.Node node : nodes)
            if ((node != null) || (!compiler.Compiler.devMode()))
                node.accept(this, o);
        return null;
    }


    @Override
    public Object visit(AST.DefFunDefn defFunDefn, Object o) {
        MEM.Frame frame = Memory.frames.get(defFunDefn);
        MEM.Label entryLabel = ImcGen.entryLabel.get(defFunDefn);
        MEM.Label exitLabel = ImcGen.exitLabel.get(defFunDefn);

        // Entry label
        Vector<IMC.Stmt> stmts = new Vector<IMC.Stmt>();
        stmts.add(new IMC.LABEL(entryLabel));

        for (AST.ParDefn param : defFunDefn.pars) {
            //param.accept(this, 0);
        }

        for (AST.Stmt stmt : defFunDefn.stmts) {
            //stmts.add(ImcGen.stmt.get(stmt));
            IMC.Stmt stmtCode = ImcGen.stmt.get(stmt);
            stmtCode.accept(linearizer, stmts);

            // Probably for proper recursion...
            stmt.accept(this, o);
        }

        // Exit label: sometimes not required .. TODO

        // A bodge - fuj!
        IMC.Stmt possibleJump = stmts.getLast();
        boolean add = true;
        if (possibleJump instanceof IMC.JUMP ) {
            IMC.JUMP jmp = (IMC.JUMP) possibleJump;
            if (jmp.addr instanceof IMC.NAME)
                if (exitLabel == ((IMC.NAME) jmp.addr).label)
                    add = false;
        }
        if (add) stmts.add(new IMC.JUMP(new IMC.NAME(exitLabel)));
        
        LIN.CodeChunk funChunk = new LIN.CodeChunk(frame, stmts, entryLabel, exitLabel);
        ImcLin.addCodeChunk(funChunk);
        return null;
    }

    @Override
    public Object visit(AST.AtomExpr atomExpr, Object o) {
        if (atomExpr.type == AST.AtomExpr.Type.STR) {
            MEM.AbsAccess strConstAccess = Memory.strings.get(atomExpr);
            LIN.DataChunk strConstChunk = new LIN.DataChunk(strConstAccess);
            ImcLin.addDataChunk(strConstChunk);
        }
        return null;
    }

    
}
