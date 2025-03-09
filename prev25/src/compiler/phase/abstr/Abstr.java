package compiler.phase.abstr;

import java.util.*;

import compiler.common.report.*;
import compiler.phase.*;

/**
 * Abstract syntax phase.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class Abstr extends Phase {

	/** The abstract syntax tree. */
	public static AST.Node tree;

	/**
	 * Phase construction.
	 */
	public Abstr() {
		super("abstr");
	}

	// ===== LOGGER =====

	/**
	 * Abstract syntax logger.
	 * 
	 * This logger traverses the abstract syntax tree and produces an XML
	 * description of it. Additionally, other loggers can be plugged-in to include
	 * attribute values computed in subsequent phases.
	 */
	public static class Logger implements AST.Visitor<Object, String> {

		/** The logger the log should be written to. */
		private final compiler.common.logger.Logger logger;

		/** A list of subvisitors for logging results of the subsequent phases. */
		private final LinkedList<AST.Visitor<?, ?>> subvisitors;

		/**
		 * Construct a new visitor with a logger the log should be written to.
		 * 
		 * @param logger The logger the log should be written to.
		 */
		public Logger(final compiler.common.logger.Logger logger) {
			this.logger = logger;
			this.subvisitors = new LinkedList<AST.Visitor<?, ?>>();
		}

		/**
		 * Adds a new subvisitor to this visitor.
		 * 
		 * @param subvisitor The subvisitor.
		 */
		public void addSubvisitor(AST.Visitor<?, ?> subvisitor) {
			subvisitors.addLast(subvisitor);
		}

		/**
		 * A wrapper for accepting this visitor.
		 * 
		 * Makes the given node accept this visitor. Unless in development mode where
		 * the missing node is replaced by a placeholder, the internal error is thrown
		 * if the node is missing, i.e., when {@code null} pointer is given instead of a
		 * node.
		 * 
		 * @param node          The node that should accept this visitor.
		 * @param elemClassName The name of the class of the name accepting this visitor
		 *                      (needed only if the node is of type
		 *                      {@link compiler.phase.abstr.AST.Nodes}).
		 */
		private void safeAccept(final AST.Node node, final String elemClassName) {
			if (node == null) {
				if (compiler.Compiler.devMode()) {
					logger.begElement("astnode");
					logger.addAttribute("none", "");
					logger.endElement();
				} else
					throw new Report.InternalError();
			} else
				node.accept(this, elemClassName);
		}

		/**
		 * A wrapper for printing out locations.
		 * 
		 * Prints out the location. Unless in development mode where the missing
		 * location is replaced by a placeholder, the internal error is thrown if the
		 * location is missing, i.e., when {@code null} pointer is given instead of a
		 * location.
		 * 
		 * @param location The location.
		 */
		private void safeLocation(final Location location) {
			if (location == null) {
				if (compiler.Compiler.devMode()) {
					logger.begElement("location");
					logger.addAttribute("none", "");
					logger.endElement();
				} else
					throw new Report.InternalError();
			} else
				location.log(logger);
		}

		// ----- Trees -----

		@Override
		public Object visit(AST.Nodes<? extends AST.Node> nodes, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(nodes.location());
			logger.addAttribute("id", Integer.toString(nodes.id));
			logger.addAttribute("label", elemClassName);
			final String subElemClassName = elemClassName.replaceFirst("^[A-Za-z0-9]*<", "").replaceFirst(">$", "");
			for (AST.Node node : nodes)
				safeAccept(node, subElemClassName);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				nodes.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		// ----- Definitions -----

		@Override
		public Object visit(AST.TypDefn typDefn, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			typDefn.location().log(logger);
			logger.addAttribute("id", Integer.toString(typDefn.id));
			logger.addAttribute("label", typDefn.getClass().getSimpleName());
			logger.addAttribute("name", typDefn.name);
			safeAccept(typDefn.type, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				typDefn.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.VarDefn varDefn, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(varDefn.location());
			logger.addAttribute("id", Integer.toString(varDefn.id));
			logger.addAttribute("label", varDefn.getClass().getSimpleName());
			logger.addAttribute("name", varDefn.name);
			safeAccept(varDefn.type, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				varDefn.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.DefFunDefn defFunDefn, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(defFunDefn.location());
			logger.addAttribute("id", Integer.toString(defFunDefn.id));
			logger.addAttribute("label", defFunDefn.getClass().getSimpleName());
			logger.addAttribute("name", defFunDefn.name);
			safeAccept(defFunDefn.pars, "Nodes<ParDefn>");
			safeAccept(defFunDefn.stmts, "Nodes<Stmt>");
			safeAccept(defFunDefn.type, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				defFunDefn.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.ExtFunDefn extFunDefn, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(extFunDefn.location());
			logger.addAttribute("id", Integer.toString(extFunDefn.id));
			logger.addAttribute("label", extFunDefn.getClass().getSimpleName());
			logger.addAttribute("name", extFunDefn.name);
			safeAccept(extFunDefn.pars, "Nodes<ParDefn>");
			safeAccept(extFunDefn.type, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				extFunDefn.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.ParDefn parDefn, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(parDefn.location());
			logger.addAttribute("id", Integer.toString(parDefn.id));
			logger.addAttribute("label", parDefn.getClass().getSimpleName());
			logger.addAttribute("name", parDefn.name);
			safeAccept(parDefn.type, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				parDefn.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.CompDefn compDefn, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(compDefn.location());
			logger.addAttribute("id", Integer.toString(compDefn.id));
			logger.addAttribute("label", compDefn.getClass().getSimpleName());
			logger.addAttribute("name", compDefn.name);
			safeAccept(compDefn.type, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				compDefn.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		// ----- Statements -----

		@Override
		public Object visit(AST.LetStmt letStmt, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(letStmt.location());
			logger.addAttribute("id", Integer.toString(letStmt.id));
			logger.addAttribute("label", letStmt.getClass().getSimpleName());
			safeAccept(letStmt.defns, "Nodes<Defn>");
			safeAccept(letStmt.stmts, "Nodes<Stmt>");
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				letStmt.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.AssignStmt assignStmt, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(assignStmt.location());
			logger.addAttribute("id", Integer.toString(assignStmt.id));
			logger.addAttribute("label", assignStmt.getClass().getSimpleName());
			safeAccept(assignStmt.dstExpr, null);
			safeAccept(assignStmt.srcExpr, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				assignStmt.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.ExprStmt exprStmt, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(exprStmt.location());
			logger.addAttribute("id", Integer.toString(exprStmt.id));
			logger.addAttribute("label", exprStmt.getClass().getSimpleName());
			safeAccept(exprStmt.expr, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				exprStmt.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.IfThenStmt ifThenStmt, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(ifThenStmt.location());
			logger.addAttribute("id", Integer.toString(ifThenStmt.id));
			logger.addAttribute("label", ifThenStmt.getClass().getSimpleName());
			safeAccept(ifThenStmt.condExpr, null);
			safeAccept(ifThenStmt.thenStmt, "Nodes<Stmt>");
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				ifThenStmt.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.IfThenElseStmt ifThenElseStmt, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(ifThenElseStmt.location());
			logger.addAttribute("id", Integer.toString(ifThenElseStmt.id));
			logger.addAttribute("label", ifThenElseStmt.getClass().getSimpleName());
			safeAccept(ifThenElseStmt.condExpr, null);
			safeAccept(ifThenElseStmt.thenStmt, "Nodes<Stmt>");
			safeAccept(ifThenElseStmt.elseStmt, "Nodes<Stmt>");
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				ifThenElseStmt.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.ReturnStmt retStmt, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(retStmt.location());
			logger.addAttribute("id", Integer.toString(retStmt.id));
			logger.addAttribute("label", retStmt.getClass().getSimpleName());
			safeAccept(retStmt.retExpr, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				retStmt.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.WhileStmt whileStmt, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(whileStmt.location());
			logger.addAttribute("id", Integer.toString(whileStmt.id));
			logger.addAttribute("label", whileStmt.getClass().getSimpleName());
			safeAccept(whileStmt.condExpr, null);
			safeAccept(whileStmt.stmts, "Nodes<Stmt>");
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				whileStmt.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		// ----- Types -----

		@Override
		public Object visit(AST.AtomType atomType, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(atomType.location());
			logger.addAttribute("id", Integer.toString(atomType.id));
			logger.addAttribute("label", atomType.getClass().getSimpleName());
			logger.addAttribute("name", atomType.type.name());
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				atomType.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.ArrType arrType, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(arrType.location());
			logger.addAttribute("id", Integer.toString(arrType.id));
			logger.addAttribute("label", arrType.getClass().getSimpleName());
			logger.addAttribute("name", arrType.getClass().getSimpleName() + "[" + arrType.numElems + "]");
			safeAccept(arrType.elemType, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				arrType.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.PtrType ptrType, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(ptrType.location());
			logger.addAttribute("id", Integer.toString(ptrType.id));
			logger.addAttribute("label", ptrType.getClass().getSimpleName());
			safeAccept(ptrType.baseType, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				ptrType.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.StrType strType, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(strType.location());
			logger.addAttribute("id", Integer.toString(strType.id));
			logger.addAttribute("label", strType.getClass().getSimpleName());
			safeAccept(strType.comps, "Nodes<CompDefn>");
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				strType.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.UniType uniType, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(uniType.location());
			logger.addAttribute("id", Integer.toString(uniType.id));
			logger.addAttribute("label", uniType.getClass().getSimpleName());
			safeAccept(uniType.comps, "Nodes<CompDefn>");
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				uniType.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.FunType funType, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(funType.location());
			logger.addAttribute("id", Integer.toString(funType.id));
			logger.addAttribute("label", funType.getClass().getSimpleName());
			safeAccept(funType.parTypes, "Nodes<Type>");
			safeAccept(funType.resType, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				funType.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.NameType nameType, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(nameType.location());
			logger.addAttribute("id", Integer.toString(nameType.id));
			logger.addAttribute("label", nameType.getClass().getSimpleName());
			logger.addAttribute("name", nameType.name);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				nameType.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		// ----- Expressions -----

		@Override
		public Object visit(AST.ArrExpr arrExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(arrExpr.location());
			logger.addAttribute("id", Integer.toString(arrExpr.id));
			logger.addAttribute("label", arrExpr.getClass().getSimpleName());
			safeAccept(arrExpr.arrExpr, null);
			safeAccept(arrExpr.idx, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				arrExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.AtomExpr atomExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(atomExpr.location());
			logger.addAttribute("id", Integer.toString(atomExpr.id));
			logger.addAttribute("label", atomExpr.getClass().getSimpleName());
			logger.addAttribute("name", atomExpr.value);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				atomExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.BinExpr binExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(binExpr.location());
			logger.addAttribute("id", Integer.toString(binExpr.id));
			logger.addAttribute("label", binExpr.getClass().getSimpleName());
			logger.addAttribute("name", binExpr.oper.name());
			safeAccept(binExpr.fstExpr, null);
			safeAccept(binExpr.sndExpr, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				binExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.CallExpr callExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(callExpr.location());
			logger.addAttribute("id", Integer.toString(callExpr.id));
			logger.addAttribute("label", callExpr.getClass().getSimpleName());
			safeAccept(callExpr.funExpr, null);
			safeAccept(callExpr.argExprs, "Nodes<Expr>");
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				callExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.CastExpr castExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(castExpr.location());
			logger.addAttribute("id", Integer.toString(castExpr.id));
			logger.addAttribute("label", castExpr.getClass().getSimpleName());
			safeAccept(castExpr.expr, null);
			safeAccept(castExpr.type, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				castExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.CompExpr compExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(compExpr.location());
			logger.addAttribute("id", Integer.toString(compExpr.id));
			logger.addAttribute("label", compExpr.getClass().getSimpleName());
			logger.addAttribute("name", "." + compExpr.name);
			safeAccept(compExpr.recExpr, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				compExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.NameExpr nameExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(nameExpr.location());
			logger.addAttribute("id", Integer.toString(nameExpr.id));
			logger.addAttribute("label", nameExpr.getClass().getSimpleName());
			logger.addAttribute("name", nameExpr.name);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				nameExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.PfxExpr pfxExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(pfxExpr.location());
			logger.addAttribute("id", Integer.toString(pfxExpr.id));
			logger.addAttribute("label", pfxExpr.getClass().getSimpleName());
			logger.addAttribute("name", pfxExpr.oper.name());
			safeAccept(pfxExpr.subExpr, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				pfxExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.SfxExpr sfxExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(sfxExpr.location());
			logger.addAttribute("id", Integer.toString(sfxExpr.id));
			logger.addAttribute("label", sfxExpr.getClass().getSimpleName());
			logger.addAttribute("name", sfxExpr.oper.name());
			safeAccept(sfxExpr.subExpr, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				sfxExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(AST.SizeExpr sizeExpr, String elemClassName) {
			if (logger == null)
				return null;
			logger.begElement("astnode");
			safeLocation(sizeExpr.location());
			logger.addAttribute("id", Integer.toString(sizeExpr.id));
			logger.addAttribute("label", sizeExpr.getClass().getSimpleName());
			safeAccept(sizeExpr.type, null);
			for (AST.Visitor<?, ?> subvisitor : subvisitors)
				sizeExpr.accept(subvisitor, null);
			logger.endElement();
			return null;
		}

	}

}
