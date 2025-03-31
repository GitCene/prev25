package compiler.phase.memory;

import java.util.function.Predicate;

import compiler.common.logger.Logger;
import compiler.phase.*;
import compiler.phase.abstr.*;
import compiler.phase.seman.*;

/**
 * Memory layout phase: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class Memory extends Phase {

	// Attribute Frames:

	/** Tester for nodes describing function definitions. */
	private static final Predicate<AST.Node> validForFrames = //
			(AST.Node node) -> (node instanceof AST.DefFunDefn);

	/** Maps function declarations to frames. */
	public static final SemAn.Attribute<MEM.Frame> frames = new SemAn.Attribute<MEM.Frame>(validForFrames);

	// Attribute Accesses:

	/** Tester for nodes describing variables. */
	private static final Predicate<AST.Node> validForAccesses = //
			(AST.Node node) -> ((node instanceof AST.VarDefn) || (node instanceof AST.ParDefn)
					|| (node instanceof AST.CompDefn));

	/** Maps variable declarations to accesses. */
	public static final SemAn.Attribute<MEM.Access> accesses = new SemAn.Attribute<MEM.Access>(validForAccesses);

	// Attribute Strings:

	/** Tester for nodes describing strings. */
	private static final Predicate<AST.Node> validForStrings = //
			(AST.Node node) -> ((node instanceof AST.AtomExpr)
					&& (((AST.AtomExpr) node).type == AST.AtomExpr.Type.STR));

	/** Maps variable declarations to accesses. */
	public static final SemAn.Attribute<MEM.AbsAccess> strings = new SemAn.Attribute<MEM.AbsAccess>(validForStrings);

	/**
	 * Phase construction.
	 */
	public Memory() {
		super("memory");
	}

	// ===== LOGGER =====

	public static class Logger implements AST.NullVisitor<Object, Object> {

		private void log(MEM.Access access) {
			if (access instanceof MEM.AbsAccess absAccess) {
				if (logger == null)
					return;
				logger.begElement("access");
				logger.addAttribute("size", Long.toString(absAccess.size));
				logger.addAttribute("label", absAccess.label.name);
				if (absAccess.init != null)
					logger.addAttribute("init", absAccess.init);
				logger.endElement();
			}
			if (access instanceof MEM.RelAccess relAccess) {
				if (logger == null)
					return;
				logger.begElement("access");
				logger.addAttribute("size", Long.toString(relAccess.size));
				logger.addAttribute("offset", Long.toString(relAccess.offset));
				if (relAccess.depth >= 0)
					logger.addAttribute("depth", Long.toString(relAccess.depth));
				logger.endElement();
			}
		}

		private void log(MEM.Frame frame) {
			if (logger == null)
				return;
			logger.begElement("frame");
			logger.addAttribute("label", frame.label.name);
			logger.addAttribute("depth", Long.toString(frame.depth));
			logger.addAttribute("locssize", Long.toString(frame.locsSize));
			logger.addAttribute("argssize", Long.toString(frame.argsSize));
			logger.addAttribute("size", Long.toString(frame.size));
			logger.addAttribute("FP", frame.FP.toString());
			logger.addAttribute("RV", frame.RV.toString());
			logger.endElement();
		}

		/** The logger the log should be written to. */
		private final compiler.common.logger.Logger logger;

		/**
		 * Construct a new visitor with a logger the log should be written to.
		 * 
		 * @param logger The logger the log should be written to.
		 */
		public Logger(final compiler.common.logger.Logger logger) {
			this.logger = logger;
		}

		@Override
		public Object visit(AST.VarDefn varDefn, Object arg) {
			if (Memory.accesses.get(varDefn) == null)
				return null;
			log(Memory.accesses.get(varDefn));
			return null;
		}

		@Override
		public Object visit(AST.DefFunDefn defFunDefn, Object arg) {
			if (Memory.frames.get(defFunDefn) == null)
				return null;
			log(Memory.frames.get(defFunDefn));
			return null;
		}

		@Override
		public Object visit(AST.ParDefn parDefn, Object arg) {
			if (Memory.accesses.get(parDefn) == null)
				return null;
			log(Memory.accesses.get(parDefn));
			return null;
		}

		@Override
		public Object visit(AST.CompDefn compDefn, Object arg) {
			if (Memory.accesses.get(compDefn) == null)
				return null;
			log(Memory.accesses.get(compDefn));
			return null;
		}

		@Override
		public Object visit(AST.AtomExpr atomExpr, Object arg) {
			switch (atomExpr.type) {
			case STR:
				log(Memory.strings.get(atomExpr));
				break;
			default:
				break;
			}
			return null;
		}

	}

}