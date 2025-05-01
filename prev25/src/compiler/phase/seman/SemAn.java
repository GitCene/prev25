package compiler.phase.seman;

import java.util.function.*;
import java.util.*;

import compiler.common.report.*;
import compiler.phase.*;
import compiler.phase.abstr.*;

/**
 * Semantic analysis phase.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class SemAn extends Phase {

	/**
	 * Phase construction.
	 */
	public SemAn() {
		super("seman");
	}

	// Attribute IsDef:

	/** Tester for nodes that represent name usage. */
	private static final Predicate<AST.Node> validForDefAt = //
			(AST.Node node) -> (node instanceof AST.NameType) || (node instanceof AST.NameExpr) //;
				|| (node instanceof AST.CompExpr); // Let's suppose this doesn't break anything.

	/** Mapping of names to definitions. */
	public static final Attribute<AST.Defn> defAt = new Attribute<AST.Defn>(validForDefAt);

	// Attribute IsType:

	/** Tester for nodes defining types. */
	protected static final Predicate<AST.Node> validForIsType = //
			(AST.Node node) -> (node instanceof AST.TypDefn) || (node instanceof AST.Type);

	/** Attribute specifying what type is defined by a node. */
	public static final Attribute<TYP.Type> isType = new Attribute<TYP.Type>(validForIsType);

	// Attribute ofType:

	/** Tester for nodes that can be typed. */
	private static final Predicate<AST.Node> validForOfType = //
			(AST.Node node) -> (node instanceof AST.Expr) || //
					((node instanceof AST.Defn) && (!(node instanceof AST.TypDefn)));

	/** Attribute specifying what is a type of a node. */
	public static final Attribute<TYP.Type> ofType = new Attribute<TYP.Type>(validForOfType);

	// Attribute IsConst:

	/** Tester for nodes describing constant expressions. */
	private static final Predicate<AST.Node> validForIsConst = //
			(AST.Node node) -> (node instanceof AST.Expr);

	/** Attribute specifying whether an expression consists of constants only. */
	public static final Attribute<Boolean> isConst = new Attribute<Boolean>(validForIsConst);

	// Attribute IsAddr:

	/** Tester for nodes describing addressable exression. */
	private static final Predicate<AST.Node> validForIsAddr = //
			(AST.Node node) -> (node instanceof AST.Expr);

	/** Attribute specifying whether an expression is addressable or not. */
	public static final Attribute<Boolean> isAddr = new Attribute<Boolean>(validForIsAddr);

	// ===== ATTRIBUTES =====

	/**
	 * An attribute of the abstract syntax tree node.
	 *
	 * @param <Value> Values associated with nodes.
	 */
	public static class Attribute<Value> {

		/** Mapping of nodes to values. */
		private final Vector<Value> mapping;

		/** Checker for testing whether a node is a valid key. */
		private Predicate<AST.Node> keyChecker;

		/**
		 * Constructs a new attribute.
		 * 
		 * @param keyChecker Checker for testing whether a node is a valid key.
		 */
		public Attribute(Predicate<AST.Node> keyChecker) {
			this.mapping = new Vector<Value>();
			this.keyChecker = keyChecker;
		}

		/**
		 * Associates a value with the specified abstract syntax tree node.
		 * 
		 * @param node  The specified abstract syntax tree node.
		 * @param value The value.
		 * @return The value.
		 */
		public Value put(final AST.Node node, final Value value) {
			if (!(keyChecker.test(node)))
				throw new Report.InternalError();
			int id = node.id;
			while (id >= mapping.size())
				mapping.setSize(id + 1000);
			mapping.set(id, value);
			return value;
		}

		/**
		 * Returns a value associated with the specified abstract syntax tree node.
		 * 
		 * @param node The specified abstract syntax tree node.
		 * @return The value (or {@code null} if the value is not found).
		 */
		public Value get(final AST.Node node) {
			//System.out.println(node);
			if (!(keyChecker.test(node)))
				throw new Report.InternalError();
			int id = node.id;
			while (id >= mapping.size()) {
				//System.out.println("can't get node: " + node.toString());
				// There is problems with getting compdefns here.
				//System.out.println("Returning null becaue id ? ah?");
				return null;
			}
			return mapping.get(id);
		}

	}

	// ===== LOGGER =====

	/**
	 * Semantic analysis logger.
	 * 
	 * This logger prints out the XML description of semantic attributes that are
	 * attached to the abstract syntax tree. It does not traverse the entire
	 * abstract syntax tree. Instead, it is used as a
	 * {@link compiler.phase.abstr.Abstr.Logger} plug-in.
	 */
	public static class Logger implements AST.NullVisitor<Object, Object>, TYP.Visitor<Object, Boolean> {

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

		/**
		 * Produces the log of a node.
		 * 
		 * Dumps out the semantic attributes of a node. Unless in development mode where
		 * the missing information is replaced by a placeholder, the internal error is
		 * thrown if the information is missing.
		 * 
		 * @param node The node.
		 */
		private void log(final AST.Node node) {
			// attribute defAt:
			{
				if (validForDefAt.test(node)) {
					final AST.Defn defn = SemAn.defAt.get(node);
					logger.begElement("defat");
					if (defn == null) {
						if (compiler.Compiler.devMode())
							logger.addAttribute("none", "");
						else
							throw new Report.InternalError();
					} else {
						logger.addAttribute("id", Integer.toString(defn.id));
						logger.addAttribute("location", defn.location().toString());
					}
					logger.endElement();
				}
			}
			// attribute isType:
			{
				if (validForIsType.test(node)) {
					final TYP.Type type = SemAn.isType.get(node);
					logger.begElement("istype");
					if (type == null) {
						if (compiler.Compiler.devMode())
							logger.addAttribute("none", "");
						else
							throw new Report.InternalError();
					} else {
						type.accept(this, node instanceof AST.TypDefn);
					}
					logger.endElement();
				}
			}
			// attribute ofType:
			{
				if (validForOfType.test(node)) {
					final TYP.Type type = SemAn.ofType.get(node);
					logger.begElement("oftype");
					if (type == null) {
						if (compiler.Compiler.devMode())
							logger.addAttribute("none", "");
						else
							throw new Report.InternalError();
					} else {
						type.accept(this, node instanceof AST.TypDefn);
					}
					logger.endElement();
				}
			}
			// attribute isConst:
			{
				if (validForIsConst.test(node)) {
					final Boolean valOfIsConst = SemAn.isConst.get(node);
					logger.begElement("isconst");
					if (valOfIsConst == null) {
						if (compiler.Compiler.devMode())
							logger.addAttribute("none", "");
						else
							throw new Report.InternalError();
					} else {
						if (valOfIsConst == true)
							logger.addAttribute("true", "");
					}
					logger.endElement();
				}
			}
			// attribute isLValue:
			{
				if (validForIsAddr.test(node)) {
					final Boolean valOfIsLValue = SemAn.isAddr.get(node);
					logger.begElement("isaddr");
					if (valOfIsLValue == null) {
						if (compiler.Compiler.devMode())
							logger.addAttribute("none", "");
						else
							throw new Report.InternalError();
					} else {
						if (valOfIsLValue == true)
							logger.addAttribute("true", "");
					}
					logger.endElement();
				}
			}
		}

		// ===== AST VISITOR =====

		// ----- Trees -----

		@Override
		public Object visit(AST.Nodes<? extends AST.Node> nodes, Object arg) {
			log(nodes);
			return null;
		}

		// ----- Definitions -----

		@Override
		public Object visit(AST.TypDefn typDefn, Object arg) {
			log(typDefn);
			return null;
		}

		@Override
		public Object visit(AST.VarDefn varDefn, Object arg) {
			log(varDefn);
			return null;
		}

		@Override
		public Object visit(AST.DefFunDefn defFunDefn, Object arg) {
			log(defFunDefn);
			return null;
		}

		@Override
		public Object visit(AST.ExtFunDefn extFunDefn, Object arg) {
			log(extFunDefn);
			return null;
		}

		@Override
		public Object visit(AST.ParDefn parDefn, Object arg) {
			log(parDefn);
			return null;
		}

		@Override
		public Object visit(AST.CompDefn compDefn, Object arg) {
			log(compDefn);
			return null;
		}

		// ----- Statements -----

		@Override
		public Object visit(AST.LetStmt letStmt, Object arg) {
			log(letStmt);
			return null;
		}

		@Override
		public Object visit(AST.AssignStmt assignStmt, Object arg) {
			log(assignStmt);
			return null;
		}

		@Override
		public Object visit(AST.ExprStmt callStmt, Object arg) {
			log(callStmt);
			return null;
		}

		@Override
		public Object visit(AST.IfThenStmt ifThenStmt, Object arg) {
			log(ifThenStmt);
			return null;
		}

		@Override
		public Object visit(AST.IfThenElseStmt ifThenElseStmt, Object arg) {
			log(ifThenElseStmt);
			return null;
		}

		@Override
		public Object visit(AST.ReturnStmt returnStmt, Object arg) {
			log(returnStmt);
			return null;
		}

		@Override
		public Object visit(AST.WhileStmt whileStmt, Object arg) {
			log(whileStmt);
			return null;
		}

		// ----- Types -----

		@Override
		public Object visit(AST.AtomType atomType, Object arg) {
			log(atomType);
			return null;
		}

		@Override
		public Object visit(AST.ArrType arrType, Object arg) {
			log(arrType);
			return null;
		}

		@Override
		public Object visit(AST.PtrType ptrType, Object arg) {
			log(ptrType);
			return null;
		}

		@Override
		public Object visit(AST.StrType strType, Object arg) {
			log(strType);
			return null;
		}

		@Override
		public Object visit(AST.UniType uniType, Object arg) {
			log(uniType);
			return null;
		}

		@Override
		public Object visit(AST.FunType funType, Object arg) {
			log(funType);
			return null;
		}

		@Override
		public Object visit(AST.NameType nameType, Object arg) {
			log(nameType);
			return null;
		}

		// ----- Expressions -----

		@Override
		public Object visit(AST.ArrExpr arrExpr, Object arg) {
			log(arrExpr);
			return null;
		}

		@Override
		public Object visit(AST.AtomExpr atomExpr, Object arg) {
			log(atomExpr);
			return null;
		}

		@Override
		public Object visit(AST.BinExpr binExpr, Object arg) {
			log(binExpr);
			return null;
		}

		@Override
		public Object visit(AST.CallExpr callExpr, Object arg) {
			log(callExpr);
			return null;
		}

		@Override
		public Object visit(AST.CastExpr castExpr, Object arg) {
			log(castExpr);
			return null;
		}

		@Override
		public Object visit(AST.CompExpr compExpr, Object arg) {
			log(compExpr);
			return null;
		}

		@Override
		public Object visit(AST.NameExpr nameExpr, Object arg) {
			log(nameExpr);
			return null;
		}

		@Override
		public Object visit(AST.PfxExpr pfxExpr, Object arg) {
			log(pfxExpr);
			return null;
		}

		@Override
		public Object visit(AST.SfxExpr sfxExpr, Object arg) {
			log(sfxExpr);
			return null;
		}

		@Override
		public Object visit(AST.SizeExpr sizeExpr, Object arg) {
			log(sizeExpr);
			return null;
		}

		// ===== TYP VISITOR =====

		/**
		 * A wrapper for accepting this visitor.
		 * 
		 * Makes the given type accept this visitor. Unless in development mode where
		 * the missing type is replaced by a placeholder, the internal error is thrown
		 * if the type is missing, i.e., when {@code null} pointer is given instead of
		 * an object.
		 * 
		 * @param type The type that should accept this visitor.
		 * @param full {@code true} if the type should be printed out in-depth or
		 *             {@code false} if the type should be printed with the top-most
		 *             node only (applicable only for
		 *             {@link compiler.phase.seman.TYP.NameType}).
		 */
		private void safeAccept(final TYP.Type type, final Boolean full) {
			if (type == null) {
				if (compiler.Compiler.devMode()) {
					logger.begElement("typnode");
					logger.addAttribute("none", "");
					logger.endElement();
				} else
					throw new Report.InternalError();
			} else
				type.accept(this, full);
		}

		@Override
		public Object visit(TYP.Types<? extends TYP.Type> types, Boolean full) {
			for (TYP.Type type : types) {
				safeAccept(type, full);
			}
			return null;
		}

		@Override
		public Object visit(TYP.IntType intType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(intType.id));
			logger.addAttribute("label", "INT");
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(TYP.CharType charType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(charType.id));
			logger.addAttribute("label", "CHAR");
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(TYP.BoolType boolType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(boolType.id));
			logger.addAttribute("label", "BOOL");
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(TYP.VoidType voidType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(voidType.id));
			logger.addAttribute("label", "VOID");
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(TYP.ArrType arrType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(arrType.id));
			logger.addAttribute("label", "ARRAY[" + arrType.numElems + "]");
			safeAccept(arrType.elemType, false);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(TYP.PtrType ptrType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(ptrType.id));
			logger.addAttribute("label", "POINTER");
			safeAccept(ptrType.baseType, false);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(TYP.StrType strType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(strType.id));
			logger.addAttribute("label", "STRUCT");
			safeAccept(strType.compTypes, false);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(TYP.UniType uniType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(uniType.id));
			logger.addAttribute("label", "UNION");
			safeAccept(uniType.compTypes, false);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(TYP.NameType nameType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(nameType.id));
			logger.addAttribute("label", "NAME " + nameType.name);
			if (full)
				safeAccept(nameType.type(), false);
			logger.endElement();
			return null;
		}

		@Override
		public Object visit(TYP.FunType funType, Boolean full) {
			logger.begElement("typnode");
			logger.addAttribute("id", Integer.toString(funType.id));
			logger.addAttribute("label", "FUNCTION");
			funType.parTypes.accept(this, false);
			safeAccept(funType.resType, false);
			logger.endElement();
			return null;
		}

	}

}
