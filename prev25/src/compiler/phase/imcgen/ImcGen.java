package compiler.phase.imcgen;

import java.util.function.*;

import compiler.phase.*;
import compiler.phase.abstr.*;
import compiler.phase.seman.*;
import compiler.phase.memory.*;

/**
 * Intermediate code generation phase.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class ImcGen extends Phase {

	private static final Predicate<AST.Node> validForEntryLabel = //
			(AST.Node node) -> (node instanceof AST.DefFunDefn);

	/** The entry point of the body. */
	public static final SemAn.Attribute<MEM.Label> entryLabel = new SemAn.Attribute<MEM.Label>(validForEntryLabel);

	private static final Predicate<AST.Node> validForExitLabel = //
			(AST.Node node) -> (node instanceof AST.DefFunDefn);

	/** The entry point of the epilogue. */
	public static final SemAn.Attribute<MEM.Label> exitLabel = new SemAn.Attribute<MEM.Label>(validForExitLabel);

	private static final Predicate<AST.Node> validForStmt = //
			(AST.Node node) -> (node instanceof AST.Stmt);

	/** Maps statements to intermediate code. */
	public static final SemAn.Attribute<IMC.Stmt> stmt = new SemAn.Attribute<IMC.Stmt>(validForStmt);

	private static final Predicate<AST.Node> validForExpr = //
			(AST.Node node) -> (node instanceof AST.Expr);

	/** Maps expressions to intermediate code. */
	public static final SemAn.Attribute<IMC.Expr> expr = new SemAn.Attribute<IMC.Expr>(validForExpr);

	/**
	 * Phase construction.
	 */
	public ImcGen() {
		super("imcgen");
	}

	// ===== LOGGER =====

	public static class Logger implements AST.NullVisitor<Object, Object> {

		/**
		 * Logs all attributes of a node.
		 * 
		 * @param node The node.
		 */
		private void logAttributes(final AST.Node node) {
			switch (node) {
			case AST.Expr expr -> {
				IMC.Expr exprImc = ImcGen.expr.get(expr);
				if (expr != null)
					exprImc.log(logger);
				break;
			}
			case AST.Stmt stmt -> {
				IMC.Stmt stmtImc = ImcGen.stmt.get(stmt);
				if (stmt != null)
					stmtImc.log(logger);
				break;
			}
			default -> {
			}
			}
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

		// ----- Trees -----

		@Override
		public Object visit(AST.Nodes<? extends AST.Node> nodes, Object arg) {
			return null;
		}

		// ----- Definitions -----

		@Override
		public Object visit(AST.TypDefn typeDefn, Object arg) {
			logAttributes(typeDefn);
			return null;
		}

		@Override
		public Object visit(AST.VarDefn varDefn, Object arg) {
			logAttributes(varDefn);
			return null;
		}

		@Override
		public Object visit(AST.DefFunDefn defFunDefn, Object arg) {
			logger.begElement("labels");
			MEM.Frame frame = Memory.frames.get(defFunDefn);
			MEM.Label prologueLabel = (frame == null) ? null : frame.label;
			MEM.Label bodyLabel = ImcGen.entryLabel.get(defFunDefn);
			MEM.Label epilogueLabel = ImcGen.exitLabel.get(defFunDefn);
			if (prologueLabel != null)
				logger.addAttribute("prologue", prologueLabel.name);
			if (bodyLabel != null)
				logger.addAttribute("body", bodyLabel.name);
			if (epilogueLabel != null)
				logger.addAttribute("epilogue", epilogueLabel.name);
			logger.endElement();
			logAttributes(defFunDefn);
			return null;
		}

		@Override
		public Object visit(AST.ExtFunDefn extFunDefn, Object arg) {
			logAttributes(extFunDefn);
			return null;
		}

		@Override
		public Object visit(AST.ParDefn parDefn, Object arg) {
			logAttributes(parDefn);
			return null;
		}

		@Override
		public Object visit(AST.CompDefn compDefn, Object arg) {
			logAttributes(compDefn);
			return null;
		}

		// ----- Statements -----

		@Override
		public Object visit(AST.LetStmt letStmt, Object arg) {
			logAttributes(letStmt);
			return null;
		}

		@Override
		public Object visit(AST.AssignStmt assignStmt, Object arg) {
			logAttributes(assignStmt);
			return null;
		}

		@Override
		public Object visit(AST.ExprStmt callStmt, Object arg) {
			logAttributes(callStmt);
			return null;
		}

		@Override
		public Object visit(AST.IfThenStmt ifThenStmt, Object arg) {
			logAttributes(ifThenStmt);
			return null;
		}

		@Override
		public Object visit(AST.IfThenElseStmt ifThenElseStmt, Object arg) {
			logAttributes(ifThenElseStmt);
			return null;
		}

		@Override
		public Object visit(AST.ReturnStmt returnStmt, Object arg) {
			logAttributes(returnStmt);
			return null;
		}

		@Override
		public Object visit(AST.WhileStmt whileStmt, Object arg) {
			logAttributes(whileStmt);
			return null;
		}

		// ----- Types -----

		@Override
		public Object visit(AST.AtomType atomType, Object arg) {
			logAttributes(atomType);
			return null;
		}

		@Override
		public Object visit(AST.ArrType arrType, Object arg) {
			logAttributes(arrType);
			return null;
		}

		@Override
		public Object visit(AST.PtrType ptrType, Object arg) {
			logAttributes(ptrType);
			return null;
		}

		@Override
		public Object visit(AST.StrType strType, Object arg) {
			logAttributes(strType);
			return null;
		}

		@Override
		public Object visit(AST.UniType uniType, Object arg) {
			logAttributes(uniType);
			return null;
		}

		@Override
		public Object visit(AST.FunType funType, Object arg) {
			logAttributes(funType);
			return null;
		}

		@Override
		public Object visit(AST.NameType nameType, Object arg) {
			logAttributes(nameType);
			return null;
		}

		// ----- Expressions -----

		@Override
		public Object visit(AST.ArrExpr arrExpr, Object arg) {
			logAttributes(arrExpr);
			return null;
		}

		@Override
		public Object visit(AST.AtomExpr atomExpr, Object arg) {
			logAttributes(atomExpr);
			return null;
		}

		@Override
		public Object visit(AST.BinExpr binExpr, Object arg) {
			logAttributes(binExpr);
			return null;
		}

		@Override
		public Object visit(AST.CallExpr callExpr, Object arg) {
			logAttributes(callExpr);
			return null;
		}

		@Override
		public Object visit(AST.CastExpr castExpr, Object arg) {
			logAttributes(castExpr);
			return null;
		}

		@Override
		public Object visit(AST.CompExpr compExpr, Object arg) {
			logAttributes(compExpr);
			return null;
		}

		@Override
		public Object visit(AST.NameExpr nameExpr, Object arg) {
			logAttributes(nameExpr);
			return null;
		}

		@Override
		public Object visit(AST.PfxExpr pfxExpr, Object arg) {
			logAttributes(pfxExpr);
			return null;
		}

		@Override
		public Object visit(AST.SfxExpr sfxExpr, Object arg) {
			logAttributes(sfxExpr);
			return null;
		}

		@Override
		public Object visit(AST.SizeExpr sizeExpr, Object arg) {
			logAttributes(sizeExpr);
			return null;
		}

	}

}
