package compiler.phase.imclin;

import java.io.IOException;
import java.util.*;

import compiler.common.report.*;
import compiler.phase.memory.*;
import compiler.phase.imcgen.*;

/**
 * Interpreter - for testing purposes only.
 */
public class Interpreter {

    private boolean debug = false;

    /** if CJUMP cond is <strong>false</strong>, fall through */
    private final boolean ENFORCE_FALL_THROUGH = true;

    /**
     * if a function, with the same name as a native function, is implemented inside
     * the program, use it instead of native function
     */
    private final boolean ALLOW_OVERRIDE_NATIVE = true;

    private final boolean USE_ADDITIONAL_INTERPRETER_ONLY_FUNCTIONS = false;

    private Random runtimeRandom;

    private Random random;

    private HashMap<Long, Byte> memory;

    private HashMap<MEM.Temp, Long> temps;

    private HashMap<MEM.Label, Long> dataMemLabels;

    private Vector<MEM.Label> labelsToAddr;

    private HashMap<MEM.Label, Integer> jumpMemLabels;

    private HashMap<MEM.Label, LIN.CodeChunk> callMemLabels;

    private HashMap<String, Function<Long>> nativeFunctions;

    private MEM.Temp SP;

    private MEM.Temp FP;

    private MEM.Temp RV;

    private MEM.Temp HP;

    public Interpreter(Vector<LIN.DataChunk> dataChunks, Vector<LIN.CodeChunk> codeChunks) {
        random = new Random();
        runtimeRandom = new Random();

        this.memory = new HashMap<Long, Byte>();
        this.temps = new HashMap<MEM.Temp, Long>();
        this.labelsToAddr = new Vector<>();
        this.nativeFunctions = new HashMap<>();

        SP = new MEM.Temp();
        tempST(SP, 0x7FFFFFFFFFFFFFF8l);
        HP = new MEM.Temp();
        tempST(HP, 0x2000000000000000l);

        this.dataMemLabels = new HashMap<MEM.Label, Long>();
        loadDataChunks(dataChunks);
        if (debug)
            System.out.printf("###\n");

        this.jumpMemLabels = new HashMap<MEM.Label, Integer>();
        this.callMemLabels = new HashMap<MEM.Label, LIN.CodeChunk>();

        if (ALLOW_OVERRIDE_NATIVE) {
            loadCodeChunks(codeChunks);
        }

        addNative("malloc", Interpreter::native_malloc);
        addNative("new", Interpreter::native_malloc);
        addNative("free", Interpreter::native_free);
        addNative("del", Interpreter::native_free);
        addNative("die", Interpreter::native_die);
        addNative("exit", Interpreter::native_exit);
        addNative("putint", Interpreter::native_putint);
        addNative("putint_hex", Interpreter::native_putint_hex);
        addNative("putint_bin", Interpreter::native_putint_bin);
        addNative("putuint", Interpreter::native_putuint);
        addNative("putchar", Interpreter::native_putchar);
        addNative("puts", Interpreter::native_puts);
        addNative("getint", Interpreter::native_getint);
        addNative("getchar", Interpreter::native_getchar);
        addNative("gets", Interpreter::native_gets);

        if (USE_ADDITIONAL_INTERPRETER_ONLY_FUNCTIONS) {
            addNative("printf", Interpreter::native_printf);
            addNative("random", Interpreter::native_random);
            addNative("at", Interpreter::native_at);
            addNative("seed", Interpreter::native_seed);
        }

        if (!ALLOW_OVERRIDE_NATIVE) {
            loadCodeChunks(codeChunks);
        }
    }

    private void addNative(String name, Function<Long> fn) {
        var label = new MEM.Label(name);
        this.labelsToAddr.addLast(label);
        this.nativeFunctions.put(label.name, fn);
    }

    private void loadDataChunks(Vector<LIN.DataChunk> dataChunks) {
        for (LIN.DataChunk dataChunk : dataChunks) {
            if (debug) {
                System.out.printf("### (SET) %s @ %d\n", dataChunk.label.name, tempLD(HP, false));
            }
            this.dataMemLabels.put(dataChunk.label, tempLD(HP, false));
            if (dataChunk.init != null) {
                for (int c = 0; c < dataChunk.size - 1; c++)
                    memST(tempLD(HP, false) + c, (long) dataChunk.init.charAt(c), 1, false);
                memST(tempLD(HP, false) + (dataChunk.size - 1), 0L, 1, false);
            }
            tempST(HP, tempLD(HP, false) + dataChunk.size, debug);
        }
    }

    private void loadCodeChunks(Vector<LIN.CodeChunk> codeChunks) {
        for (LIN.CodeChunk codeChunk : codeChunks) {
            this.callMemLabels.put(codeChunk.frame.label, codeChunk);
            this.labelsToAddr.addLast(codeChunk.frame.label);
            Vector<IMC.Stmt> stmts = codeChunk.stmts();
            for (int stmtOffset = 0; stmtOffset < stmts.size(); stmtOffset++) {
                if (stmts.get(stmtOffset) instanceof IMC.LABEL)
                    jumpMemLabels.put(((IMC.LABEL) stmts.get(stmtOffset)).label, stmtOffset);
            }
        }
    }

    private void memST(Long address, Long value, final int size) {
        memST(address, value, size, debug);
    }

    private void memST(Long address, Long value, final int size, boolean debug) {
        if (debug)
            System.out.printf("### (ST) [%d] <- %d\n", address, value);
        for (int b = 0; b < size; b++) {
            long longval = value & 0xFF;
            byte byteval = (byte) longval;
            memory.put(address + b, byteval);
            value = value >> 8;
        }
    }

    private Long memLD(Long address, final int size) {
        return memLD(address, size, debug);
    }

    private Long memLD(Long address, final int size, boolean debug) {
        Long value = 0L;
        for (int b = size - 1; b >= 0; b--) {
            Byte byteval = memory.get(address + b);
            if (byteval == null) {
                byteval = (byte) (random.nextLong() / 0x100);
                // throw new Report.Error("INTERPRETER: Uninitialized memory location " +
                // (address + b) + ".");
            }
            long longval = (long) byteval;
            value = (value << 8) + (longval < 0 ? longval + 0x100 : longval);
        }
        if (debug)
            System.out.printf("### (LD) %d <- [%d]\n", value, address);
        return value;
    }

    private void tempST(MEM.Temp temp, Long value) {
        tempST(temp, value, debug);
    }

    private void tempST(MEM.Temp temp, Long value, boolean debug) {
        temps.put(temp, value);
        if (debug) {
            if (temp == SP) {
                System.out.printf("### (ST) SP <- %d\n", value);
                return;
            }
            if (temp == FP) {
                System.out.printf("### (ST) FP <- %d\n", value);
                return;
            }
            if (temp == RV) {
                System.out.printf("### (ST) RV <- %d\n", value);
                return;
            }
            if (temp == HP) {
                System.out.printf("### (ST) HP <- %d\n", value);
                return;
            }
            System.out.printf("### (ST) T%d <- %d\n", temp.temp, value);
            return;
        }
    }

    private Long tempLD(MEM.Temp temp) {
        return tempLD(temp, debug);
    }

    private Long tempLD(MEM.Temp temp, boolean debug) {
        Long value = temps.get(temp);
        if (value == null) {
            value = random.nextLong();
            // throw new Report.Error("Uninitialized temporary variable T" + temp.temp +
            // ".");
        }
        if (debug) {
            if (temp == SP) {
                System.out.printf("### (LD) %d <- SP\n", value);
                return value;
            }
            if (temp == FP) {
                System.out.printf("### (LD) %d <- FP\n", value);
                return value;
            }
            if (temp == RV) {
                System.out.printf("### (LD) %d <- RV\n", value);
                return value;
            }
            if (temp == HP) {
                System.out.printf("### (LD) %d <- HP\n", value);
                return value;
            }
            System.out.printf("### (LD) %d <- T%d\n", value, temp.temp);
            return value;
        }
        return value;
    }

    private class ExprInterpreter implements IMC.Visitor<Long, Object> {

        private final Interpreter inter;

        ExprInterpreter(Interpreter inter) {
            this.inter = inter;
        }

        @Override
        public Long visit(IMC.BINOP imcBinop, Object arg) {
            long fstExpr = imcBinop.fstExpr.accept(this, null).longValue();
            long sndExpr = imcBinop.sndExpr.accept(this, null).longValue();
            switch (imcBinop.oper) {
            case OR:
                return (fstExpr != 0) || (sndExpr != 0) ? 1L : 0L;
            case AND:
                return (fstExpr != 0) && (sndExpr != 0) ? 1L : 0L;
            case EQU:
                return (fstExpr == sndExpr) ? 1L : 0L;
            case NEQ:
                return (fstExpr != sndExpr) ? 1L : 0L;
            case LEQ:
                return (fstExpr <= sndExpr) ? 1L : 0L;
            case GEQ:
                return (fstExpr >= sndExpr) ? 1L : 0L;
            case LTH:
                return (fstExpr < sndExpr) ? 1L : 0L;
            case GTH:
                return (fstExpr > sndExpr) ? 1L : 0L;
            case ADD:
                return fstExpr + sndExpr;
            case SUB:
                return fstExpr - sndExpr;
            case MUL:
                return fstExpr * sndExpr;
            case DIV:
                return fstExpr / sndExpr;
            case MOD:
                return fstExpr % sndExpr;
            }
            throw new Report.InternalError();
        }

        @Override
        public Long visit(IMC.CALL imcCall, Object arg) {
            throw new Report.InternalError();
        }

        @Override
        public Long visit(IMC.CONST imcConst, Object arg) {
            return imcConst.value;
        }

        @Override
        public Long visit(IMC.MEM1 imcMem, Object arg) {
            var addr = imcMem.addr.accept(this, null);
            if (addr == 0) {
                throw new Report.Error("Null pointer dereference");
            }
            return memLD(addr, 1);
        }

        @Override
        public Long visit(IMC.MEM8 imcMem, Object arg) {
            var addr = imcMem.addr.accept(this, null);
            if (addr == 0) {
                throw new Report.Error("Null pointer dereference");
            }
            return memLD(addr, 8);
        }

        @Override
        public Long visit(IMC.NAME imcName, Object arg) {
            var addr = getByLabel(dataMemLabels, imcName.label);
            if (addr != null) {
                return addr;
            }
            addr = (long) getByLabel(inter.labelsToAddr, imcName.label);
            return (addr >= 0) ? addr : null;
        }

        @Override
        public Long visit(IMC.SEXPR imcSExpr, Object arg) {
            throw new Report.InternalError();
        }

        @Override
        public Long visit(IMC.TEMP imcMemTemp, Object arg) {
            return tempLD(imcMemTemp.temp);
        }

        @Override
        public Long visit(IMC.UNOP imcUnop, Object arg) {
            Long subExpr = imcUnop.subExpr.accept(this, null);
            switch (imcUnop.oper) {
            case NOT:
                return (subExpr == 0) ? 1L : 0L;
            case NEG:
                return -subExpr;
            }
            throw new Report.InternalError();
        }

    }

    private class StmtInterpreter implements IMC.Visitor<MEM.Label, StmtInterpreter.Info> {

        private final Interpreter interpreter;

        StmtInterpreter(Interpreter interpreter) {
            this.interpreter = interpreter;
        }

        public static class Info {
            public MEM.Label lastLabel = null;
        }

        @Override
        public MEM.Label visit(IMC.CJUMP imcCJump, Info arg) {
            if (debug)
                System.out.println(imcCJump);
            Long cond = imcCJump.cond.accept(new ExprInterpreter(this.interpreter), null);

            if (ENFORCE_FALL_THROUGH) {
                return (cond != 0) ? ((IMC.NAME) imcCJump.posAddr).label : null;
            } else {
                return (cond != 0) ? ((IMC.NAME) imcCJump.posAddr).label : ((IMC.NAME) imcCJump.negAddr).label;
            }
        }

        @Override
        public MEM.Label visit(IMC.ESTMT imcEStmt, Info arg) {
            if (debug)
                System.out.println(imcEStmt);
            if (imcEStmt.expr instanceof IMC.CALL) {
                call((IMC.CALL) imcEStmt.expr);
                return null;
            }
            imcEStmt.expr.accept(new ExprInterpreter(this.interpreter), null);
            return null;
        }

        @Override
        public MEM.Label visit(IMC.JUMP imcJump, Info arg) {
            if (debug)
                System.out.println(imcJump);
            return ((IMC.NAME) imcJump.addr).label;
        }

        @Override
        public MEM.Label visit(IMC.LABEL imcMemLabel, Info arg) {
            if (debug)
                System.out.println(imcMemLabel);

            arg.lastLabel = imcMemLabel.label;
            return null;
        }

        @Override
        public MEM.Label visit(IMC.MOVE imcMove, Info arg) {
            if (debug) {
                System.out.println(imcMove);
            }

            switch (imcMove.dst) {
            case IMC.MEM1 m -> {
                Long dst = m.addr.accept(new ExprInterpreter(this.interpreter), null);
                Long src;
                if (imcMove.src instanceof IMC.CALL) {
                    call((IMC.CALL) imcMove.src);
                    src = memLD(tempLD(SP), 1);
                } else {
                    src = imcMove.src.accept(new ExprInterpreter(this.interpreter), null);
                }
                memST(dst, src, 1);
                return null;
            }
            case IMC.MEM8 m -> {
                Long dst = m.addr.accept(new ExprInterpreter(this.interpreter), null);
                Long src;
                if (imcMove.src instanceof IMC.CALL) {
                    call((IMC.CALL) imcMove.src);
                    src = memLD(tempLD(SP), 8);
                } else {
                    src = imcMove.src.accept(new ExprInterpreter(this.interpreter), null);
                }
                memST(dst, src, 8);
                return null;
            }
            case IMC.TEMP t -> {
                Long src;
                if (imcMove.src instanceof IMC.CALL) {
                    call((IMC.CALL) imcMove.src);
                    src = memLD(tempLD(SP), 8);
                } else {
                    src = imcMove.src.accept(new ExprInterpreter(this.interpreter), null);
                }
                tempST(t.temp, src);
                return null;
            }
            default -> {
                throw new Report.InternalError();
            }
            }
        }

        @Override
        public MEM.Label visit(IMC.STMTS imcStmts, Info arg) {
            if (debug) {
                System.out.println(imcStmts);
            }
            throw new Report.InternalError();
        }

        private void call(IMC.CALL imcCall) {
            Long offset = 0L;
            for (IMC.Expr callArg : imcCall.args) {
                Long callValue = callArg.accept(new ExprInterpreter(this.interpreter), null);
                memST(tempLD(SP) + offset, callValue, 8);
                offset += 8;
            }

            MEM.Label addrLabel = null;

            switch (imcCall.addr) {
            case IMC.MEM1 m -> {
                var addr = m.accept(new ExprInterpreter(interpreter), null);
                addrLabel = interpreter.labelsToAddr.get(addr.intValue());
            }
            case IMC.MEM8 m -> {
                var addr = m.accept(new ExprInterpreter(interpreter), null);
                if (addr != null) {
                    addrLabel = interpreter.labelsToAddr.get(addr.intValue());
                }
            }
            case IMC.NAME n -> {
                addrLabel = n.label;
            }
            case IMC.TEMP t -> {
                var addr = t.accept(new ExprInterpreter(interpreter), null);
                if (addr != null) {
                    try {
                        addrLabel = interpreter.labelsToAddr.get(addr.intValue());
                    } catch (ArrayIndexOutOfBoundsException e) {
                    }
                }
            }
            default -> {
                throw new Report.Error("Interpreter does not handle address calling for: " + stringify(imcCall.addr));
            }
            }

            if (addrLabel == null) {
                throw new Report.Error("No label exists on address: " + stringify(imcCall.addr));
            }

            if (ALLOW_OVERRIDE_NATIVE && (getByLabel(callMemLabels, addrLabel) != null)) {
                funCall(addrLabel);
                return;
            }

            var fn = nativeFunctions.get(addrLabel.name);
            if (fn != null) {
                fn.call(this.interpreter, imcCall.args.size() - 1);
                return;
            }

            funCall(addrLabel);
        }

    }

    public void funCall(MEM.Label entryMemLabel) {

        HashMap<MEM.Temp, Long> storedMemTemps;
        MEM.Temp storedFP = null;
        MEM.Temp storedRV = null;

        LIN.CodeChunk chunk = getByLabel(callMemLabels, entryMemLabel);
        MEM.Frame frame = chunk.frame;
        Vector<IMC.Stmt> stmts = chunk.stmts();
        int stmtOffset;

        /* PROLOGUE */
        {
            if (debug)
                System.out.printf("###\n### CALL: %s\n", entryMemLabel.name);

            // Store registers and FP.
            storedMemTemps = temps;
            temps = new HashMap<MEM.Temp, Long>(temps);
            // Store RA.
            // Create a stack frame.
            FP = frame.FP;
            RV = frame.RV;
            tempST(frame.FP, tempLD(SP));
            tempST(SP, tempLD(SP) - frame.size);
            // Jump to the body.
            stmtOffset = getByLabel(jumpMemLabels, chunk.entryLabel);
        }

        /* BODY */
        {
            int pc = 0;
            MEM.Label label = null;
            MEM.Label lastLabel = null;

            while ((label != chunk.exitLabel) && (lastLabel != chunk.exitLabel)) {
                if (debug) {
                    pc++;
                    System.out.printf("### %s (%d):\n", chunk.frame.label.name, pc);
                    if (pc == 1000000)
                        break;
                }

                if (label != null) {
                    Integer offset = getByLabel(jumpMemLabels, label);
                    if (offset == null) {
                        throw new Report.InternalError();
                    }
                    stmtOffset = offset;
                }

                var info = new StmtInterpreter.Info();
                label = stmts.get(stmtOffset).accept(new StmtInterpreter(this), info);
                lastLabel = info.lastLabel;

                stmtOffset += 1;
            }
        }

        /* EPILOGUE */
        {
            // Store the result.
            memST(tempLD(frame.FP), tempLD(frame.RV), 8);
            // Destroy a stack frame.
            tempST(SP, tempLD(SP) + frame.size);
            // Restore registers and FP.
            FP = storedFP;
            RV = storedRV;
            Long hp = tempLD(HP);
            temps = storedMemTemps;
            tempST(HP, hp);
            // Restore RA.
            // Return.

            if (debug)
                System.out.printf("### RETURN: %s\n###\n", entryMemLabel.name);
        }

    }

    public long run(String entryMemLabel) {
        for (MEM.Label label : callMemLabels.keySet()) {
            if (label.name.equals(entryMemLabel)) {
                funCall(label);
                return memLD(tempLD(SP), 8);
            }
        }
        throw new Report.InternalError();
    }

    // ======================== NATIVE FUNCTIONS ========================

    @FunctionalInterface
    static interface Function<R> {
        public R call(Interpreter self, int argNum);
    }

    /**
     * Allocate {@code size} bytes of memory on the heap
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun malloc(size: int): ^any}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_malloc(int argNum) {
        if (argNum <= 0) { throw new Report.Error("(native_malloc) Not enough arguments"); }
        Long size = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        Long addr = tempLD(HP);
        tempST(HP, addr + size);
        memST(tempLD(SP), addr, 8, false);
        return 0;
    }

    /**
     * Deallocate memory (here it does nothing)
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun free(ptr: ^any): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_free(int argNum) {
        return 0;
    }

    /**
     * Exit the program with {@code 0}
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun die(): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_die(int argNum) {
        System.exit(0);
        return 0;
    }

    /**
     * Exit the program with {@code 0} or {@code code}
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun exit(): void}
     * <p>
     * {@code fun exit(code: int): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_exit(int argNum) {
        if (argNum <= 0) { throw new Report.Error("(native_exit) Not enough arguments"); }

        Long exitCode = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        System.exit(exitCode.intValue());
        return 0;
    }

    /**
     * Print integer to stdout
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun putint(n: int): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_putint(int argNum) {
        if (argNum <= 0) { throw new Report.Error("(native_putint) Not enough arguments"); }
        Long c = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        System.out.printf("%d", c);
        return 0;
    }

    /**
     * Print integer as binary to stdout
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun putint_bin(n: int): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_putint_bin(int argNum) {
        if (argNum <= 0) { throw new Report.Error("(native_putint) Not enough arguments"); }
        Long c = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        var s = Long.toBinaryString(c);
        System.out.print("0b" + "0".repeat(64 - s.length()) + s);
        return 0;
    }

    /**
     * Print integer as hexadecimal to stdout
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun putint_hex(n: int): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_putint_hex(int argNum) {
        if (argNum <= 0) { throw new Report.Error("(native_putint) Not enough arguments"); }
        Long c = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        if ((c > ((1L << 31) - 1)) || (c < -((1L << 31) - 1))) {
            System.out.printf("0x%016x", c);
        } else {
            System.out.printf("0x%08x", c);
        }
        return 0;
    }

    /**
     * Print unsigned integer to stdout
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun putuint(n: int): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_putuint(int argNum) {
        if (argNum <= 0) { throw new Report.Error("(native_putint) Not enough arguments"); }
        Long c = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        System.out.print(Long.toUnsignedString(c));
        return 0;
    }

    /**
     * Get integer from stdin
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun getint(): int}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_getint(int argNum) {
        long l = 0;
        try {
            long sgn = 1;
            int c = System.in.read();
            while ((c == ' ') || (c == '\n') || (c == '\r') || (c == '\t')) {
                c = System.in.read();
            }
            if (c == '-') {
                c = System.in.read();
                sgn = -1;
            } else if (c == '+') { c = System.in.read(); }
            while ((c >= '0') && (c <= '9')) {
                l *= 10;
                l += c - '0';
                c = System.in.read();
            }
            l *= sgn;
        } catch (IOException e) {
            throw new Report.Error(e.getMessage());
        }
        memST(tempLD(SP), l, 8, false);
        return 0;
    }

    /**
     * Get line from stdin (including {@code '\n'})
     * <p>
     * Reads maximum of {@code size - 1} bytes
     * <p>
     * Pust null byte at the end
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun gets(buf: ^char, size: int): int}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_gets(int argNum) {
        if (argNum < 2) { throw new Report.Error("(native_gets) Not enough arguments"); }
        Long bufPtr = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        Long maxLen = memLD(tempLD(SP, false) + 2 * 8, 8, false) - 1L;
        try {
            int i = 0;
            int c = System.in.read();
            while ((i < maxLen) && (c != -1) && ((char) c != '\n')) {
                memST(bufPtr + i, (long) c, 1);

                if (i + 1 >= maxLen) { i++; break; }
                c = System.in.read();
                i++;
            }

            if ((i < maxLen) && ((char) c == '\n')) {
                memST(bufPtr + i, (long) c, 1);
                i++;
            }
            memST(bufPtr + i, 0L, 1);
            memST(tempLD(SP), (long) i, 8, false);
        } catch (IOException e) {
            throw new Report.Error(e.getMessage());
        }
        return 0;
    }

    /**
     * Print character to stdout
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun putchar(c: char): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_putchar(int argNum) {
        if (argNum <= 0) { throw new Report.Error("(native_putchar) Not enough arguments"); }
        Long c = memLD(tempLD(SP, false) + 1 * 8, 1, false);
        System.out.printf("%c", (char) ((long) c) & 0xFF);
        return 0;
    }

    /**
     * Get character from stdin
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun getchar(): char}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_getchar(int argNum) {
        char c = '\n';
        try {
            c = (char) System.in.read();
        } catch (Exception __) {
        }
        memST(tempLD(SP), (long) c, 1, false);
        return 0;
    }

    /**
     * Print string to stdout
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun puts(s: ^char): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_puts(int argNum) {
        if (argNum <= 0) { throw new Report.Error("(native_puts) Not enough arguments"); }
        Long strPtr = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        System.out.print(ptrToJavaString(strPtr));
        return 0;
    }

    /**
     * Get random integer
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * <p>
     * Get random integer
     * <p>
     * {@code fun random(): int}
     * <p>
     * Get random integer between {@code 0} and {@code upper_bound} (exclusive)
     * <p>
     * {@code fun random(upper_bound: int): int}
     * <p>
     * Get random integer between {@code lower_bound} (inclusive) and
     * {@code upper_bound} (exclusive)
     * <p>
     * {@code fun random(lower_bound: int, upper_bound: int): int}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_random(int argNum) {
        if (argNum <= 0) {
            Long l = runtimeRandom.nextLong();
            memST(tempLD(SP), (long) l, 8, false);
        } else if (argNum == 1) {
            Long upperBound = memLD(tempLD(SP, false) + 1 * 8, 8, false);
            Long l = runtimeRandom.nextLong(upperBound);
            memST(tempLD(SP), (long) l, 8, false);
        } else {
            Long lowerBound = memLD(tempLD(SP, false) + 1 * 8, 8, false);
            Long upperBound = memLD(tempLD(SP, false) + 2 * 8, 8, false);
            Long l = runtimeRandom.nextLong(lowerBound, upperBound);
            memST(tempLD(SP), (long) l, 8, false);
        }
        return 0;
    }

    /**
     * Print formated string {@code s}
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun printf(s: ^char, ...): void}
     * <p>
     * <strong>Formats:</strong>
     * <ul>
     * <li>{@code %%} - {@code %}</li>
     * <li>{@code %d} - integer</li>
     * <li>{@code %x} - integer as hexadecimal</li>
     * <li>{@code %X} - integer as hexadecimal upper case</li>
     * <li>{@code %b} - boolean as integer</li>
     * <li>{@code %B} - boolean as true/false</li>
     * <li>{@code %c} - character</li>
     * <li>{@code %s} - string</li>
     * <li>{@code %p} - pointer</li>
     * <li>{@code %P} - pointer as upper case</li>
     * </ul>
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_printf(int argNum) {
        if (argNum <= 0) { throw new Report.Error("(native_printf) Not enough arguments"); }
        Long mainStrPtr = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        String mainStr = ptrToJavaString(mainStrPtr);

        System.out.print(parseString(mainStr, 2, argNum));
        return 0;
    }

    /**
     * Get element of array at {@code idx}
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun at(ptr: ^any, idx: int, size_of_any: int): ^any}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_at(int argNum) {
        if (argNum < 3) { throw new Report.Error("(native_at) Not enough arguments"); }
        Long ptr = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        Long idx = memLD(tempLD(SP, false) + 2 * 8, 8, false);
        Long size = memLD(tempLD(SP, false) + 3 * 8, 8, false);

        memST(tempLD(SP), ptr + idx * size, 8, false);
        return 0;
    }

    /**
     * Set seed of random generator
     * <p>
     * <strong>Signatures:</strong>
     * <p>
     * {@code fun seed(seed: int): void}
     * 
     * @param argNum number of arguments passed to this function
     */
    private long native_seed(int argNum) {
        if (argNum < 1) { throw new Report.Error("(native_at) Not enough arguments"); }
        Long seed = memLD(tempLD(SP, false) + 1 * 8, 8, false);
        runtimeRandom.setSeed(seed);
        return 0;
    }

    // ============================ HELPERS ============================

    private String ptrToJavaString(long strPtr) {
        char chr = (char) (memLD(strPtr, 1) & 0xFF);
        StringBuilder str = new StringBuilder();
        int off = 0;
        while (chr != 0) {
            str.append(chr);
            off++;
            chr = (char) (memLD(strPtr + off, 1) & 0xFF);
        }

        return str.toString();
    }

    private String parseString(String str, long argOff, int argNum) {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        while (i < (str.length() - 1)) {
            int idx = str.indexOf('%', i);
            if (idx < 0 || (idx >= (str.length() - 1))) { break; }

            sb.append(str.substring(i, idx));
            i = idx + 2;

            switch (str.charAt(idx + 1)) {
            case '%':
                sb.append('%');
                continue;
            case 'd':
                sb.append(getArg(argOff, argNum));
                break;
            case 'x': {
                var arg = getArg(argOff, argNum);
                if ((arg > ((1 << 31) - 1)) || (arg < -((1 << 31) - 1))) {
                    sb.append(String.format("0x%016x", arg));
                } else {
                    sb.append(String.format("0x%08x", arg));
                }
            }
                break;
            // case 'X':
            // sb.append(Long.toHexString(getArg(argOff, argNum)).toUpperCase());
            // break;
            case 'b': {
                var s = Long.toBinaryString(getArg(argOff, argNum));
                sb.append("0b");
                sb.append("0".repeat(64 - s.length()));
                sb.append(s);
            }
                break;
            case 'z':
                sb.append((getArg(argOff, argNum) != 0) ? "1" : "0");
                break;
            case 'Z':
                sb.append((getArg(argOff, argNum) != 0) ? "true" : "false");
                break;
            case 'c':
                sb.append((char) (getArg(argOff, argNum) & 0xFF));
                break;
            case 's':
                sb.append(ptrToJavaString(getArg(argOff, argNum)));
                break;
            case 'p': {
                var arg = getArg(argOff, argNum);
                if ((arg > ((1 << 31) - 1)) || (arg < -((1 << 31) - 1))) {
                    sb.append(String.format("@0x%016x", arg));
                } else {
                    sb.append(String.format("@0x%08x", arg));
                }
            }
                // sb.append(String.format("@0x%016x", getArg(argOff, argNum)));
                break;
            // case 'P':
            // sb.append("@" + String.format("%016X", getArg(argOff, argNum)));
            // break;
            default:
                continue;
            }

            argOff++;
        }

        sb.append(str.substring(i));

        return sb.toString();
    }

    private long getArg(long off, int argNum) {
        if (off > argNum) {
            throw new Report.Error("Not enough arguments");
        }
        return memLD(tempLD(SP, false) + off * 8, 8, false);
    }

    public static String stringify(IMC.Stmt s) {
        if (s == null) { return "null"; }

        switch (s) {
        case IMC.CJUMP j -> {
            return "CJUMP(" + stringify(j.cond) + ", " + stringify(j.posAddr) + ", " + stringify(j.negAddr) + ")";
        }
        case IMC.ESTMT e -> {
            return "ESTMT(" + stringify(e.expr) + ")";
        }
        case IMC.JUMP j -> {
            return "JUMP(" + stringify(j.addr) + ")";
        }
        case IMC.LABEL l -> {
            return "LABEL(" + l.label.name + ")";
        }
        case IMC.MOVE m -> {
            return "MOVE(" + stringify(m.dst) + ", " + stringify(m.src) + ")";
        }
        case IMC.STMTS ss -> {
            String ret = "STMTS(";
            for (var stmt : ss.stmts) {
                ret += stringify(stmt);
                if (stmt != ss.stmts.getLast()) {
                    ret += "; ";
                }
            }
            ret += ")";
            return ret;
        }
        default -> {
            return "null";
        }
        }
    }

    public static String stringify(Vector<IMC.Stmt> stmts) {
        String ret = "STMTS(";
        for (var stmt : stmts) {
            ret += stringify(stmt);
            if (stmt != stmts.getLast()) {
                ret += "; ";
            }
        }
        ret += ")";
        return ret;
    }

    public static String stringify(IMC.Expr e) {
        if (e == null) { return "null"; }

        switch (e) {
        case IMC.BINOP b -> {
            return "BINOP(" + b.oper.toString() + ", " + stringify(b.fstExpr) + ", " + stringify(b.sndExpr) + ")";
        }
        case IMC.CALL c -> {
            String ret = "CALL(" + stringify(c.addr);
            for (int i = 0; i < c.args.size(); i++) {
                ret += ", ";
                ret += c.offs.get(i).toString() + ":" + stringify(c.args.get(i));
            }
            ret += ")";
            return ret;
        }
        case IMC.CONST c -> {
            return c.toString();
        }
        case IMC.MEM1 m -> {
            return "MEM1(" + stringify(m.addr) + ")";
        }
        case IMC.MEM8 m -> {
            return "MEM8(" + stringify(m.addr) + ")";
        }
        case IMC.NAME n -> {
            return n.toString();
        }
        case IMC.TEMP t -> {
            return t.toString();
        }
        case IMC.UNOP u -> {
            return "UNOP(" + u.oper.toString() + ", " + stringify(u.subExpr) + ")";
        }
        default -> {
            return "null";
        }
        }
    }

    private static <T> T getByLabel(Map<MEM.Label, T> map, MEM.Label key) {
        for (var e : map.entrySet()) {
            if (e.getKey().name.equals(key.name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static int getByLabel(List<MEM.Label> list, MEM.Label key) {
        for (int i = 0; i < list.size(); i++) {
            var e = list.get(i);
            if (e.name.equals(key.name)) {
                return i;
            }
        }
        return -1;
    }
}
