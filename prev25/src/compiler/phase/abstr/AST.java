package compiler.phase.abstr;

import java.util.*;
import java.util.function.*;

import compiler.common.report.*;

/**
 * Abstract syntax tree.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class AST {

	/** (Unused but included to keep javadoc happy.) */
	private AST() {
		throw new Report.InternalError();
	}

	// ===== NODES =====

	// ----- Trees ----

	/**
	 * An abstract node of an abstract syntax tree.
	 */
	public static abstract class Node implements Locatable {

		/** The number of nodes constructed so far. */
		private static int numNodes = 0;

		/** The unique id of this node. */
		public final int id;

		/** The location of this node. */
		private Location location;

		/**
		 * Constructs a node of an abstract syntax tree.
		 * 
		 * @param location The location.
		 */
		public Node(final Locatable location) {
			id = numNodes++;
			this.location = location.location();
		}

		@Override
		public final void relocate(final Locatable location) {
			this.location = location.location();
		}

		@Override
		public final Location location() {
			return location;
		}

		/**
		 * The acceptor method.
		 * 
		 * @param <Result>   The result type.
		 * @param <Argument> The argument type.
		 * @param visitor    The visitor accepted by this acceptor.
		 * @param arg        The argument.
		 * @return The result.
		 */
		public abstract <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg);

	}

	/**
	 * A sequence of nodes of an abstract syntax tree.
	 * 
	 * @param <N> The type of nodes stored in this sequence.
	 */
	public static class Nodes<N extends Node> extends Node implements Iterable<N> {

		/** The nodes stored in this sequence. */
		private final N[] nodes;

		/**
		 * Constructs a sequence of nodes.
		 */
		public Nodes() {
			this(new Location(0, 0), new Vector<N>());
		}

		/**
		 * Constructs a sequence of nodes.
		 * 
		 * @param nodes The nodes stored in this sequence (no {@code null}s allowed).
		 */
		@SuppressWarnings("unchecked")
		public Nodes(final List<N> nodes) {
			super(nodes.isEmpty() ? new Location(0, 0) : new Location(nodes.getFirst(), nodes.getLast()));
			this.nodes = (N[]) (new Node[nodes.size()]);
			int index = 0;
			for (final N n : nodes)
				this.nodes[index++] = n;
			if (this.nodes.length == 0)
				relocate(new Location(0, 0));
			else
				relocate(new Location(this.nodes[0], this.nodes[this.nodes.length - 1]));
		}

		/**
		 * Constructs a sequence of nodes.
		 * 
		 * @param location The location.
		 * @param nodes    The nodes stored in this sequence (no {@code null}s allowed).
		 */
		@SuppressWarnings("unchecked")
		public Nodes(final Locatable location, final List<N> nodes) {
			super(location);
			this.nodes = (N[]) (new Node[nodes.size()]);
			int index = 0;
			for (final N n : nodes)
				this.nodes[index++] = n;
		}

		/**
		 * Returns the node at the specified position in this sequence.
		 * 
		 * @param index The index of the node to return.
		 * @return The node at the specified index.
		 */
		public final N get(final int index) {
			return nodes[index];
		}

		/**
		 * Returns the number of nodes in this sequence.
		 * 
		 * @return The number of nodes in this sequence.
		 */
		public final int size() {
			return nodes.length;
		}

		// Iterable<N>

		@Override
		public void forEach(final Consumer<? super N> action) throws NullPointerException {
			for (final N n : this)
				action.accept(n);
		}

		@Override
		public Iterator<N> iterator() {
			return new NodesIterator();
		}

		// Iterator.

		/**
		 * Iterator over nodes with the removal operation blocked.
		 * 
		 * It is assumed that the underlying array of nodes is immutable.
		 */
		private final class NodesIterator implements Iterator<N> {

			/** Constructs a new iterator. */
			private NodesIterator() {
			}

			/** The index of the next node to be returned. */
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < nodes.length;
			}

			@Override
			public N next() throws NoSuchElementException {
				if (index < nodes.length)
					return nodes[index++];
				else
					throw new NoSuchElementException("");
			}

			@Override
			public void remove() {
				throw new Report.InternalError();
			}

			@Override
			public void forEachRemaining(final Consumer<? super N> action) {
				while (hasNext())
					action.accept(next());
			}

		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ----- Definitions -----

	/**
	 * A definition of a name.
	 */
	public static abstract class Defn extends Node {

		/** The defined name. */
		public final String name;

		/** The type of the defined name. */
		public final Type type;

		/**
		 * Constructs a definition of a name.
		 * 
		 * @param location The location.
		 * @param name     The defined name.
		 * @param type     The type of the defined name.
		 */
		public Defn(final Locatable location, final String name, final Type type) {
			super(location);
			this.name = name;
			this.type = type;
		}

		public String toString() {
			return String.format("(%s:%s)", this.name, this.type.id);
		}

	}

	/**
	 * A stand-alone definition of a name.
	 */
	public static abstract class FullDefn extends Defn {

		/**
		 * Constructs a stand-alone definition of a name.
		 * 
		 * @param location The location.
		 * @param name     The defined name.
		 * @param type     The type of the defined name.
		 */
		public FullDefn(final Locatable location, final String name, final Type type) {
			super(location, name, type);
		}

	}

	/**
	 * A definition of a name which is a part of a stand-alone definition of another
	 * name.
	 */
	public static abstract class PartDefn extends Defn {

		/**
		 * Constructs a definition of a name which is a part of a stand-alone definition
		 * of another name.
		 * 
		 * @param location The location.
		 * @param name     The defined name.
		 * @param type     The type of the defined name.
		 */
		public PartDefn(final Locatable location, final String name, final Type type) {
			super(location, name, type);
		}

	}

	/**
	 * A definition of a type.
	 */
	public static class TypDefn extends FullDefn {

		/**
		 * Constructs a definition of a type.
		 * 
		 * @param location The location.
		 * @param name     The name of the this type.
		 * @param type     The type of the this type.
		 */
		public TypDefn(final Locatable location, final String name, final Type type) {
			super(location, name, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of a variable.
	 */
	public static class VarDefn extends FullDefn {

		/**
		 * Constructs a definition of a variable.
		 * 
		 * @param location The location.
		 * @param name     The name of the this variable.
		 * @param type     The type of the this variable.
		 */
		public VarDefn(final Locatable location, final String name, final Type type) {
			super(location, name, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of a function.
	 */
	public static abstract class FunDefn extends FullDefn {

		/** The parameters. */
		public final Nodes<ParDefn> pars;

		/**
		 * Constructs a definition of a function.
		 * 
		 * @param location The location.
		 * @param name     The name of this function.
		 * @param pars     The parameters (no {@code null}s allowed).
		 * @param type     The result type of this function.
		 */
		public FunDefn(final Locatable location, final String name, final List<ParDefn> pars, final Type type) {
			super(location, name, type);
			this.pars = new Nodes<ParDefn>(pars);
		}

	}

	/**
	 * A definition of a defined function.
	 */
	public static class DefFunDefn extends FunDefn {

		/** The statements. */
		public final Nodes<Stmt> stmts;

		/**
		 * Constructs a definition of a defined function.
		 * 
		 * @param location The location.
		 * @param name     The name of this function.
		 * @param pars     The parameters (no {@code null}s allowed).
		 * @param type     The result type of this function.
		 * @param stmts    The statements ({@code null} not allowed).
		 */
		public DefFunDefn(final Locatable location, final String name, final List<ParDefn> pars, final Type type,
				final List<Stmt> stmts) {
			super(location, name, pars, type);
			this.stmts = new Nodes<Stmt>(stmts);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of an external function.
	 */
	public static class ExtFunDefn extends FunDefn {

		/**
		 * Constructs a definition of a defined function.
		 * 
		 * @param location The location.
		 * @param name     The name of this function.
		 * @param pars     The parameters (no {@code null}s allowed).
		 * @param type     The result type of this function.
		 */
		public ExtFunDefn(final Locatable location, final String name, final List<ParDefn> pars, final Type type) {
			super(location, name, pars, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A parameter definition.
	 */
	public static class ParDefn extends PartDefn {

		/**
		 * Constructs a parameter definition.
		 * 
		 * @param location The location.
		 * @param name     The name of this parameter.
		 * @param type     The type of this parameter.
		 */
		public ParDefn(final Locatable location, final String name, final Type type) {
			super(location, name, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of a record component.
	 */
	public static class CompDefn extends PartDefn {

		/**
		 * Constructs a definition of a record component.
		 * 
		 * @param location The location.
		 * @param name     The name of this record component.
		 * @param type     The type of this record component.
		 */
		public CompDefn(final Locatable location, final String name, final Type type) {
			super(location, name, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ----- Statements -----

	/**
	 * A statement.
	 */
	public static abstract class Stmt extends Node {

		/**
		 * Constructs a statement.
		 * 
		 * @param location The location.
		 */
		public Stmt(final Locatable location) {
			super(location);
		}

	}

	/**
	 * A let statement.
	 */
	public static class LetStmt extends Stmt {

		/** The definitions. */
		public final Nodes<FullDefn> defns;

		/** The inner statements. */
		public final Nodes<Stmt> stmts;

		/**
		 * Constructs a let statement.
		 * 
		 * @param location The location.
		 * @param defns    The definitions.
		 * @param stmts    The inner statements.
		 */
		public LetStmt(final Locatable location, final List<FullDefn> defns, final List<Stmt> stmts) {
			super(location);
			this.defns = new Nodes<FullDefn>(defns);
			this.stmts = new Nodes<Stmt>(stmts);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * An assignment statement.
	 */
	public static class AssignStmt extends Stmt {

		/** The destination. */
		public final Expr dstExpr;

		/** The source. */
		public final Expr srcExpr;

		/**
		 * Construct an assignment statement.
		 * 
		 * @param location The location.
		 * @param dstExpr  The destination.
		 * @param srcExpr  The source.
		 */
		public AssignStmt(final Locatable location, final Expr dstExpr, final Expr srcExpr) {
			super(location);
			this.dstExpr = dstExpr;
			this.srcExpr = srcExpr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * An expression statement.
	 */
	public static class ExprStmt extends Stmt {

		/** The expression. */
		public final Expr expr;

		/**
		 * Constructs an expression statement.
		 * 
		 * @param location The location.
		 * @param expr     The expression.
		 */
		public ExprStmt(final Locatable location, final Expr expr) {
			super(location);
			this.expr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * An if-then statement.
	 */
	public static class IfThenStmt extends Stmt {

		/** The condition. */
		public final Expr condExpr;

		/** The statements in the then branch. */
		public final Nodes<Stmt> thenStmt;

		/**
		 * Constructs an if-then-else statement.
		 * 
		 * @param location  The location.
		 * @param condExpr  The condition.
		 * @param thenStmts The statements in the then branch (no {@code null}s
		 *                  allowed).
		 */
		public IfThenStmt(final Locatable location, final Expr condExpr, final List<Stmt> thenStmts) {
			super(location);
			this.condExpr = condExpr;
			this.thenStmt = new Nodes<Stmt>(thenStmts);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * An if-then-else statement.
	 */
	public static class IfThenElseStmt extends IfThenStmt {

		/** The statements in the else branch. */
		public final Nodes<Stmt> elseStmt;

		/**
		 * Constructs an if-then-else statement.
		 * 
		 * @param location  The location.
		 * @param cond      The condition.
		 * @param thenStmts The statements in the then branch (no {@code null}s
		 *                  allowed).
		 * @param elseStmts The statements in the else branch (no {@code null}s
		 *                  allowed).
		 */
		public IfThenElseStmt(final Locatable location, final Expr cond, final List<Stmt> thenStmts,
				final List<Stmt> elseStmts) {
			super(location, cond, thenStmts);
			this.elseStmt = new Nodes<Stmt>(elseStmts);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A return statement.
	 */
	public static class ReturnStmt extends Stmt {

		/** The return value. */
		public final Expr retExpr;

		/**
		 * Constructs a return statement.
		 * 
		 * @param location The location.
		 * @param retExpr  The return value.
		 */
		public ReturnStmt(final Locatable location, final Expr retExpr) {
			super(location);
			this.retExpr = retExpr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A while statement.
	 */
	public static class WhileStmt extends Stmt {

		/** The condition. */
		public final Expr condExpr;

		/** The inner statements. */
		public final Nodes<Stmt> stmts;

		/**
		 * Constructs a while statement.
		 * 
		 * @param location The location.
		 * @param condExpr The condition.
		 * @param stmts    The inner statements.
		 */
		public WhileStmt(final Locatable location, final Expr condExpr, final List<Stmt> stmts) {
			super(location);
			this.condExpr = condExpr;
			this.stmts = new Nodes<Stmt>(stmts);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ----- Types -----

	/**
	 * A type.
	 */
	public static abstract class Type extends Node {

		/**
		 * Constructs a type.
		 * 
		 * @param location The location.
		 */
		public Type(final Locatable location) {
			super(location);
		}

	}

	/**
	 * An atomic type.
	 */
	public static class AtomType extends Type {

		/** The atomic types. */
		public enum Type {
			/** Type {@code int}. */
			INT,
			/** Type {@code char}. */
			CHAR,
			/** Type {@code bool}. */
			BOOL,
			/** Type {@code void}. */
			VOID,
		};

		/** The kind of this atomic type. */
		public final Type type;

		/**
		 * Constructs an atomic type.
		 * 
		 * @param location The location.
		 * @param type     The atomic type.
		 */
		public AtomType(final Locatable location, final Type type) {
			super(location);
			this.type = type;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * An array type.
	 */
	public static class ArrType extends Type {

		/** The type of elements in this array. */
		public final Type elemType;

		/** The number of elements. */
		public final String numElems;

		/**
		 * Constructs an array type.
		 * 
		 * @param location The location.
		 * @param elemType The type of elements in this array.
		 * @param numElems The number of elements.
		 */
		public ArrType(final Location location, final Type elemType, final String numElems) {
			super(location);
			this.elemType = elemType;
			this.numElems = numElems;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A pointer type.
	 */
	public static class PtrType extends Type {

		/** The base type pointers of this type point to. */
		public final Type baseType;

		/**
		 * Constructs a pointer type.
		 * 
		 * @param location The location.
		 * @param baseType The base type pointers of this type point to.
		 */
		public PtrType(final Locatable location, final Type baseType) {
			super(location);
			this.baseType = baseType;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A record type, i.e., either a struct or union.
	 */
	public static abstract class RecType extends Type {

		/** The components. */
		public final Nodes<CompDefn> comps;

		/**
		 * Constructs a record type.
		 * 
		 * @param location The location.
		 * @param comps    The components of this union (no {@code null}s allowed).
		 */
		public RecType(final Location location, final List<CompDefn> comps) {
			super(location);
			this.comps = new Nodes<CompDefn>(comps);
		}

	}

	/**
	 * A struct type.
	 */
	public static class StrType extends RecType {

		/**
		 * Constructs a struct type.
		 * 
		 * @param location The location.
		 * @param comps    The components of this struct (no {@code null}s allowed).
		 */
		public StrType(final Location location, final List<CompDefn> comps) {
			super(location, comps);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A union type.
	 */
	public static class UniType extends RecType {

		/**
		 * Constructs a union type.
		 * 
		 * @param location The location.
		 * @param comps    The components of this union (no {@code null}s allowed).
		 */
		public UniType(final Location location, final List<CompDefn> comps) {
			super(location, comps);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of a function type.
	 */
	public static class FunType extends Type {

		/** Types of parameters. */
		public final Nodes<Type> parTypes;

		/** A type of the function result. */
		public final Type resType;

		/**
		 * Constructs a function type.
		 * 
		 * @param location The location.
		 * @param parTypes The types of parameters.
		 * @param type     The result type.
		 */
		public FunType(final Locatable location, final List<Type> parTypes, final Type type) {
			super(location);
			this.parTypes = new Nodes<Type>(parTypes);
			this.resType = type;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A type name.
	 */
	public static class NameType extends Type {

		/** The name. */
		public final String name;

		/**
		 * Constructs a type name.
		 * 
		 * @param location The location.
		 * @param name     The name of type.
		 */
		public NameType(final Locatable location, final String name) {
			super(location);
			this.name = name;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ----- Expressions -----

	/**
	 * An expression.
	 */
	public static abstract class Expr extends Node {

		/**
		 * Constructs an expression.
		 * 
		 * @param location The location.
		 */
		public Expr(final Locatable location) {
			super(location);
		}

	}

	/**
	 * An array access expression.
	 */
	public static class ArrExpr extends Expr {

		/** The array. */
		public final Expr arrExpr;

		/** The index. */
		public final Expr idx;

		/**
		 * Constructs an array access expression.
		 * 
		 * @param location The location.
		 * @param arrExpr  The array.
		 * @param idx      The index.
		 */
		public ArrExpr(final Locatable location, final Expr arrExpr, final Expr idx) {
			super(location);
			this.arrExpr = arrExpr;
			this.idx = idx;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Atom expression, i.e., a constant.
	 */
	public static class AtomExpr extends Expr {

		/** Types. */
		public enum Type {
			/** Constant of type {@code int}. */
			INT,
			/** Constant of type {@code char}. */
			CHAR,
			/** Constant of type {@code bool}. */
			BOOL,
			/** Constant of a pointer type. */
			PTR,
			/** Constant of type {@code ^char}. */
			STR,
		};

		/** The type of a constant. */
		public final Type type;

		/** The value of a constant. */
		public final String value;

		/**
		 * Constructs an atom expression, i.e., a constant.
		 * 
		 * @param location The location.
		 * @param type     The type of this constant.
		 * @param value    The value of this constant.
		 */
		public AtomExpr(final Locatable location, final Type type, final String value) {
			super(location);
			this.type = type;
			this.value = value;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A binary expression.
	 */
	public static class BinExpr extends Expr {

		/** Operators. */
		public enum Oper {
			/** Logical or. */
			OR,
			/** Logical and. */
			AND,
			/** Equal. */
			EQU,
			/** Not equal. */
			NEQ,
			/** Less than. */
			LTH,
			/** Greater than. */
			GTH,
			/** Less of equal. */
			LEQ,
			/** Greater or equal. */
			GEQ,
			/** Multiply. */
			MUL,
			/** Divide. */
			DIV,
			/** Modulo. */
			MOD,
			/** Add. */
			ADD,
			/** Subtract. */
			SUB,
		};

		/** The operator. */
		public final Oper oper;

		/** The first subexpression. */
		public final Expr fstExpr;

		/** The second subexpression. */
		public final Expr sndExpr;

		/**
		 * Constructs a binary expression.
		 * 
		 * @param location The location.
		 * @param oper     The operator.
		 * @param fstExpr  The first subexpression.
		 * @param sndExpr  The second subexpression.
		 */
		public BinExpr(final Locatable location, final Oper oper, final Expr fstExpr, final Expr sndExpr) {
			super(location);
			this.oper = oper;
			this.fstExpr = fstExpr;
			this.sndExpr = sndExpr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A function call.
	 */
	public static class CallExpr extends Expr {

		/** The function. */
		public Expr funExpr;

		/** The arguments. */
		public final Nodes<Expr> argExprs;

		/**
		 * Constructs a function call.
		 * 
		 * @param location The location.
		 * @param funExpr  The function.
		 * @param argExprs The arguments (no {@code null}s allowed).
		 */
		public CallExpr(final Locatable location, final Expr funExpr, final List<Expr> argExprs) {
			super(location);
			this.funExpr = funExpr;
			this.argExprs = new Nodes<Expr>(argExprs);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A cast expression.
	 */
	public static class CastExpr extends Expr {

		/** The type. */
		public final Type type;

		/** The expression. */
		public final Expr expr;

		/**
		 * Constructs a cast expression.
		 * 
		 * @param location The location.
		 * @param type     The type.
		 * @param expr     The expression.
		 */
		public CastExpr(final Locatable location, final Type type, final Expr expr) {
			super(location);
			this.type = type;
			this.expr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A component access expression.
	 */
	public static class CompExpr extends Expr {

		/** The container. */
		public final Expr recExpr;

		/** The component name. */
		public final String name;

		/**
		 * Constructs a component access expression.
		 * 
		 * @param location The location.
		 * @param recExpr  The container.
		 * @param name     The component name.
		 */
		public CompExpr(final Locatable location, final Expr recExpr, final String name) {
			super(location);
			this.recExpr = recExpr;
			this.name = name;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A name (of a variable or a function).
	 */
	public static class NameExpr extends Expr {

		/** The name. */
		public String name;

		/**
		 * Constructs a name (of a variable or a function).
		 * 
		 * @param location The location.
		 * @param name     The name.
		 */
		public NameExpr(final Locatable location, final String name) {
			super(location);
			this.name = name;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A prefix expression.
	 */
	public static class PfxExpr extends Expr {

		/** Operators. */
		public enum Oper {
			/** Sign. */
			ADD,
			/** Sign. */
			SUB,
			/** Negation. */
			NOT,
			/** Reference. */
			PTR,
		};

		/** The operator. */
		public final Oper oper;

		/** The subexpression. */
		public final Expr subExpr;

		/**
		 * Constructs a prefix expression.
		 * 
		 * @param location The location.
		 * @param subExpr  The operator.
		 * @param expr     The subexpression.
		 */
		public PfxExpr(final Locatable location, final Oper subExpr, final Expr expr) {
			super(location);
			this.oper = subExpr;
			this.subExpr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A suffix expression.
	 */
	public static class SfxExpr extends Expr {

		/** Operators. */
		public enum Oper {
			/** Dereference. */
			PTR,
		};

		/** The operator. */
		public final Oper oper;

		/** The subexpression. */
		public final Expr subExpr;

		/**
		 * Constructs a suffix expression.
		 * 
		 * @param location The location.
		 * @param subExpr  The operator.
		 * @param expr     The subexpression.
		 */
		public SfxExpr(final Locatable location, final Oper subExpr, final Expr expr) {
			super(location);
			this.oper = subExpr;
			this.subExpr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A {@code sizeof} expression.
	 */
	public static class SizeExpr extends Expr {

		/** The type. */
		public final Type type;

		/**
		 * Constructs a {code sizeof} expression.
		 * 
		 * @param location The location.
		 * @param type     The type.
		 */
		public SizeExpr(final Locatable location, final Type type) {
			super(location);
			this.type = type;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ===== VISITORS =====

	/**
	 * An abstract syntax tree visitor.
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 */
	public interface Visitor<Result, Argument> {

		// ----- Trees -----

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(Nodes<? extends Node> nodes, Argument arg) {
			throw new Report.InternalError();
		}

		// ----- Definitions -----

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(TypDefn typDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(VarDefn varDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(DefFunDefn defFunDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ExtFunDefn extFunDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ParDefn parDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(CompDefn compDefn, Argument arg) {
			throw new Report.InternalError();
		}

		// ----- Statements -----

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(LetStmt letStmt, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(AssignStmt assignStmt, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ExprStmt exprStmt, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(IfThenStmt ifThenStmt, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(IfThenElseStmt ifTheElseStmt, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ReturnStmt returnStmt, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(WhileStmt whileStmt, Argument arg) {
			throw new Report.InternalError();
		}

		// ----- Types -----

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(AtomType atomType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ArrType arrType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(PtrType ptrType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(StrType strType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(UniType uniType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(FunType funType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(NameType nameType, Argument arg) {
			throw new Report.InternalError();
		}

		// ----- Expressions -----

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ArrExpr arrExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(AtomExpr atomExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(BinExpr binExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(CallExpr callExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(CastExpr castExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(CompExpr compExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(NameExpr nameExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(PfxExpr pfxExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(SfxExpr sfxExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(SizeExpr sizeExpr, Argument arg) {
			throw new Report.InternalError();
		}

	}

	/**
	 * An abstract syntax tree visitor that does nothing.
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 */
	public static interface NullVisitor<Result, Argument> extends Visitor<Result, Argument> {

		// ----- Trees -----

		@Override
		public default Result visit(Nodes<? extends Node> nodes, Argument arg) {
			return null;
		}

		// ----- Definitions -----

		@Override
		public default Result visit(TypDefn typDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(VarDefn varDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(DefFunDefn defFunDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ExtFunDefn extFunDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ParDefn parDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(CompDefn compDefn, Argument arg) {
			return null;
		}

		// ----- Statements -----

		@Override
		public default Result visit(LetStmt letStmt, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(AssignStmt assignStmt, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ExprStmt callStmt, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(IfThenStmt ifThenStmt, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(IfThenElseStmt ifThenElseStmt, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ReturnStmt returnStmt, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(WhileStmt whileStmt, Argument arg) {
			return null;
		}

		// ----- Types -----

		@Override
		public default Result visit(AtomType atomType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ArrType arrType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(PtrType ptrType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(StrType strType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(UniType uniType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(FunType funType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(NameType nameType, Argument arg) {
			return null;
		}

		// ----- Expressions -----

		@Override
		public default Result visit(ArrExpr arrExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(AtomExpr atomExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(BinExpr binExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(CallExpr callExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(CastExpr castExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(CompExpr compExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(NameExpr nameExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(PfxExpr pfxExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(SfxExpr sfxExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(SizeExpr sizeExpr, Argument arg) {
			return null;
		}

	}

	/**
	 * An abstract syntax tree visitor that traverses the entire abstract syntax
	 * tree.
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 */
	public interface FullVisitor<Result, Argument> extends Visitor<Result, Argument> {

		// ----- Trees -----

		@Override
		public default Result visit(Nodes<? extends Node> nodes, Argument arg) {
			for (final Node node : nodes)
				if ((node != null) || (!compiler.Compiler.devMode()))
					node.accept(this, arg);
			return null;
		}

		// ----- Definitions -----

		@Override
		public default Result visit(TypDefn typDefn, Argument arg) {
			if ((typDefn.type != null) || (!compiler.Compiler.devMode()))
				typDefn.type.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(VarDefn varDefn, Argument arg) {
			if ((varDefn.type != null) || (!compiler.Compiler.devMode()))
				varDefn.type.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(DefFunDefn defFunDefn, Argument arg) {
			if ((defFunDefn.pars != null) || (!compiler.Compiler.devMode()))
				defFunDefn.pars.accept(this, arg);
			if ((defFunDefn.type != null) || (!compiler.Compiler.devMode()))
				defFunDefn.type.accept(this, arg);
			if ((defFunDefn.stmts != null) || (!compiler.Compiler.devMode()))
				defFunDefn.stmts.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(ExtFunDefn extFunDefn, Argument arg) {
			if ((extFunDefn.pars != null) || (!compiler.Compiler.devMode()))
				extFunDefn.pars.accept(this, arg);
			if ((extFunDefn.type != null) || (!compiler.Compiler.devMode()))
				extFunDefn.type.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(ParDefn parDefn, Argument arg) {
			if ((parDefn.type != null) || (!compiler.Compiler.devMode()))
				parDefn.type.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(CompDefn compDefn, Argument arg) {
			if ((compDefn.type != null) || (!compiler.Compiler.devMode()))
				compDefn.type.accept(this, arg);
			return null;
		}

		// ----- Statements -----

		@Override
		public default Result visit(LetStmt letStmt, Argument arg) {
			if ((letStmt.defns != null) || (!compiler.Compiler.devMode()))
				letStmt.defns.accept(this, arg);
			if ((letStmt.stmts != null) || (!compiler.Compiler.devMode()))
				letStmt.stmts.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(AssignStmt assignStmt, Argument arg) {
			if ((assignStmt.dstExpr != null) || (!compiler.Compiler.devMode()))
				assignStmt.dstExpr.accept(this, arg);
			if ((assignStmt.srcExpr != null) || (!compiler.Compiler.devMode()))
				assignStmt.srcExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(ExprStmt callStmt, Argument arg) {
			if ((callStmt.expr != null) || (!compiler.Compiler.devMode()))
				callStmt.expr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(IfThenStmt ifThenStmt, Argument arg) {
			if ((ifThenStmt.condExpr != null) || (!compiler.Compiler.devMode()))
				ifThenStmt.condExpr.accept(this, arg);
			if ((ifThenStmt.thenStmt != null) || (!compiler.Compiler.devMode()))
				ifThenStmt.thenStmt.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(IfThenElseStmt ifThenElseStmt, Argument arg) {
			if ((ifThenElseStmt.condExpr != null) || (!compiler.Compiler.devMode()))
				ifThenElseStmt.condExpr.accept(this, arg);
			if ((ifThenElseStmt.thenStmt != null) || (!compiler.Compiler.devMode()))
				ifThenElseStmt.thenStmt.accept(this, arg);
			if ((ifThenElseStmt.elseStmt != null) || (!compiler.Compiler.devMode()))
				ifThenElseStmt.elseStmt.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(ReturnStmt returnStmt, Argument arg) {
			if ((returnStmt.retExpr != null) || (!compiler.Compiler.devMode()))
				returnStmt.retExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(WhileStmt whileStmt, Argument arg) {
			if ((whileStmt.condExpr != null) || (!compiler.Compiler.devMode()))
				whileStmt.condExpr.accept(this, arg);
			if ((whileStmt.stmts != null) || (!compiler.Compiler.devMode()))
				whileStmt.stmts.accept(this, arg);
			return null;
		}

		// ----- Types -----

		@Override
		public default Result visit(AtomType atomType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ArrType arrType, Argument arg) {
			if ((arrType.elemType != null) || (!compiler.Compiler.devMode()))
				arrType.elemType.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(PtrType ptrType, Argument arg) {
			if ((ptrType.baseType != null) || (!compiler.Compiler.devMode()))
				ptrType.baseType.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(StrType strType, Argument arg) {
			if ((strType.comps != null) || (!compiler.Compiler.devMode()))
				strType.comps.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(UniType uniType, Argument arg) {
			if ((uniType.comps != null) || (!compiler.Compiler.devMode()))
				uniType.comps.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(FunType funType, Argument arg) {
			if ((funType.parTypes != null) || (!compiler.Compiler.devMode()))
				funType.parTypes.accept(this, arg);
			if ((funType.resType != null) || (!compiler.Compiler.devMode()))
				funType.resType.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(NameType nameType, Argument arg) {
			return null;
		}

		// ----- Expressions -----

		@Override
		public default Result visit(ArrExpr arrExpr, Argument arg) {
			if ((arrExpr.arrExpr != null) || (!compiler.Compiler.devMode()))
				arrExpr.arrExpr.accept(this, arg);
			if ((arrExpr.idx != null) || (!compiler.Compiler.devMode()))
				arrExpr.idx.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(AtomExpr atomExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(BinExpr binExpr, Argument arg) {
			if ((binExpr.fstExpr != null) || (!compiler.Compiler.devMode()))
				binExpr.fstExpr.accept(this, arg);
			if ((binExpr.sndExpr != null) || (!compiler.Compiler.devMode()))
				binExpr.sndExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(CallExpr callExpr, Argument arg) {
			if ((callExpr.funExpr != null) || (!compiler.Compiler.devMode()))
				callExpr.funExpr.accept(this, arg);
			if ((callExpr.argExprs != null) || (!compiler.Compiler.devMode()))
				callExpr.argExprs.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(CastExpr castExpr, Argument arg) {
			if ((castExpr.type != null) || (!compiler.Compiler.devMode()))
				castExpr.type.accept(this, arg);
			if ((castExpr.expr != null) || (!compiler.Compiler.devMode()))
				castExpr.expr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(CompExpr compExpr, Argument arg) {
			if ((compExpr.recExpr != null) || (!compiler.Compiler.devMode()))
				compExpr.recExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(NameExpr nameExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(PfxExpr pfxExpr, Argument arg) {
			if ((pfxExpr.subExpr != null) || (!compiler.Compiler.devMode()))
				pfxExpr.subExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(SfxExpr sfxExpr, Argument arg) {
			if ((sfxExpr.subExpr != null) || (!compiler.Compiler.devMode()))
				sfxExpr.subExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(SizeExpr sizeExpr, Argument arg) {
			if ((sizeExpr.type != null) || (!compiler.Compiler.devMode()))
				sizeExpr.type.accept(this, arg);
			return null;
		}

	}

}
