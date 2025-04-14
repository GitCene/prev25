package compiler.phase.seman;

import java.util.*;

import compiler.common.report.*;
import compiler.phase.abstr.*;
import compiler.phase.abstr.AST.Nodes;
import compiler.phase.abstr.AST.ParDefn;
import compiler.phase.seman.NameResolver.SymbTable.CannotFndNameException;
import compiler.phase.seman.NameResolver.SymbTable.CannotInsNameException;

/**
 * Name resolver.
 * 
 * The name resolver connects each node of a abstract syntax tree where a name
 * is used with the node where it is defined. The only exceptions are struct and
 * union component names which are connected with their definitions by the type
 * resolver. The results of the name resolver are stored in
 * {@link compiler.phase.seman.SemAn#defAt}.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class NameResolver implements AST.FullVisitor<Object, NameResolver.Mode> {

	/** Constructs a new name resolver. */
	public NameResolver() {
	}

	/** Two passes of name resolving. */
	protected enum Mode {
		/** The first pass: declaring names. */
		DECLARE,
		/** The second pass: resolving names. */
		RESOLVE,
	}

	/** The symbol table. */
	private SymbTable symbTable = new SymbTable();

	
	@Override
	public Object visit(AST.Nodes< ? extends AST.Node> nodes, NameResolver.Mode mode) {
		// This gets called first.
		if (mode == null) {
			visit(nodes, Mode.DECLARE);
			visit(nodes, Mode.RESOLVE);
		} else {
			for (final AST.Node node : nodes)
				if ((node != null) || (!compiler.Compiler.devMode()))
					node.accept(this, mode);
		}
		return null;
	}

	// ----- Definitions -----

	@Override
	public Object visit(AST.TypDefn typDefn, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				try {
					symbTable.ins(typDefn.name, typDefn);
					typDefn.type.accept(this, mode);
				} catch (CannotInsNameException e) {
					throw new Report.Error(typDefn, "Name '" + typDefn.name + "' already exists in this scope.");
				}
				return null;
			case Mode.RESOLVE:
				if (typDefn.type != null || (!compiler.Compiler.devMode()))
					typDefn.type.accept(this, mode);
				return null;
			default:
				throw new Report.InternalError();
		}
	}


	@Override
	public Object visit(AST.VarDefn varDefn, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				try {
					symbTable.ins(varDefn.name, varDefn);
				} catch (CannotInsNameException e) {
					throw new Report.Error(varDefn, "Name '" + varDefn.name + "' already exists in this scope.");
				}
				return null;
			case Mode.RESOLVE:
			// type je lahko ime
				if (varDefn.type != null || (!compiler.Compiler.devMode()))
					varDefn.type.accept(this, mode);
				return null;
			default:
				throw new Report.InternalError();
		}
	}

	@Override
	public Object visit(AST.DefFunDefn defFunDefn, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				try {
					symbTable.ins(defFunDefn.name, defFunDefn);
				} catch (Exception e) {
					throw new Report.Error(defFunDefn, "Name '" + defFunDefn.name + "' already exists in this scope.");
				}
				return null;
			case Mode.RESOLVE:
				// Resolve parameter types
				if (defFunDefn.pars != null || (!compiler.Compiler.devMode()))
					for (final AST.ParDefn parDefn : defFunDefn.pars)
						if (parDefn.type != null || (!compiler.Compiler.devMode()))
							parDefn.type.accept(this, mode);
				// Resolve function type
				if ((defFunDefn.type != null) || (!compiler.Compiler.devMode()))
					defFunDefn.type.accept(this, mode);

				symbTable.newScope();
				// Declarations for inner block
				if (defFunDefn.pars != null || (!compiler.Compiler.devMode()))
					defFunDefn.pars.accept(this, Mode.DECLARE);
				// Resolve inner block
				if (defFunDefn.stmts != null || (!compiler.Compiler.devMode()))
					defFunDefn.stmts.accept(this, Mode.RESOLVE);

				symbTable.oldScope();
				return null;
			default:
				throw new Report.InternalError();
		}
	}

	@Override
	public Object visit(AST.ExtFunDefn extFunDefn, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				try {
					symbTable.ins(extFunDefn.name, extFunDefn);
				} catch (Exception e) {
					throw new Report.Error(extFunDefn, "Name '" + extFunDefn.name + "' already exists in this scope.");
				}
				return null;
			case Mode.RESOLVE:
				// Resolve parameter types
				if (extFunDefn.pars != null || (!compiler.Compiler.devMode()))
					for (final AST.ParDefn parDefn : extFunDefn.pars)
						if (parDefn.type != null || (!compiler.Compiler.devMode()))
							parDefn.type.accept(this, mode);
				// Resolve function type
				if ((extFunDefn.type != null) || (!compiler.Compiler.devMode()))
					extFunDefn.type.accept(this, mode);

				symbTable.newScope();
				// Declarations for inner block, I guess?
				if (extFunDefn.pars != null || (!compiler.Compiler.devMode()))
					extFunDefn.pars.accept(this, Mode.DECLARE);

				symbTable.oldScope();
				return null;
			default:
				throw new Report.InternalError();
		}
	}

	@Override
	public Object visit(AST.ParDefn parDefn, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				try {
					symbTable.ins(parDefn.name, parDefn);	
				} catch (CannotInsNameException e) {
					throw new Report.Error(parDefn, "Duplicate parameter name: " + parDefn.name);
				}
				return null;
			case Mode.RESOLVE:
				if ((parDefn.type != null) || (!compiler.Compiler.devMode()))
					parDefn.type.accept(this, mode);
				return null;
			default:
				throw new Report.InternalError();
		}
	}

	//public Object visit(AST.CompDefn compDefn, NameResolver.Mode mode) : todo.
	@Override
	public Object visit(AST.CompDefn compDefn, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				try {
					symbTable.ins(compDefn.name, compDefn);	
				} catch (CannotInsNameException e) {
					throw new Report.Error(compDefn, "Duplicate component name: " + compDefn.name);
				}
				if ((compDefn.type != null) || (!compiler.Compiler.devMode()))
					compDefn.type.accept(this, mode);
				return null;
			case Mode.RESOLVE:
				if ((compDefn.type != null) || (!compiler.Compiler.devMode()))
					compDefn.type.accept(this, mode);
				return null;
			default:
				throw new Report.InternalError();
		}
	}

	@Override
	public Object visit(AST.StrType strType, NameResolver.Mode mode) {
		return visit_helper(strType, mode);
	}
	
	@Override
	public Object visit(AST.UniType uniType, NameResolver.Mode mode) {
		return visit_helper(uniType, mode);
	}
	
	public Object visit_helper(AST.RecType recType, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				return null;
			case Mode.RESOLVE:
				symbTable.newScope();
				if (recType.comps != null || (!compiler.Compiler.devMode()))
					recType.comps.accept(this, mode);
				symbTable.oldScope();
				return null;
			default:
				throw new Report.InternalError();
		}
	}
	
	// ----- Statements -----

	@Override
	public Object visit(AST.LetStmt letStmt, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				return null;
			case Mode.RESOLVE:
				symbTable.newScope();
				if (letStmt.defns != null || (!compiler.Compiler.devMode())) {
					letStmt.defns.accept(this, Mode.DECLARE);
					letStmt.defns.accept(this, Mode.RESOLVE);
				}
				if ((letStmt.stmts != null) || (!compiler.Compiler.devMode())) {
					letStmt.stmts.accept(this, mode);
				}
				symbTable.oldScope();
				return null;
			default:
				throw new Report.InternalError();
		}
	}

	// ----- Types -----

	@Override
	public Object visit(AST.NameType nameType, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				return null;
			case Mode.RESOLVE:
				try {
					AST.Defn defn = symbTable.fnd(nameType.name);
					SemAn.defAt.put(nameType, defn);
					return null;
				} catch (CannotFndNameException e) {
					throw new Report.Error(nameType, "Undefined type name: " + nameType.name);
				}
			default:
				throw new Report.InternalError();
		}
	}

	// ----- Expressions -----
	
	@Override
	public Object visit(AST.NameExpr nameExpr, NameResolver.Mode mode) {
		switch (mode) {
			case Mode.DECLARE:
				return null;
			case Mode.RESOLVE:
				try {
					AST.Defn defn = symbTable.fnd(nameExpr.name);
					/*
					 * 
					 System.out.println("Found name " + defn.name + " in depth " + symbTable.currDepth + " among :");
					 for (String key : symbTable.allDefnsOfAllNames.keySet()) {
						System.out.printf("%s: ", key);
						for (NameResolver.SymbTable.ScopedDefn defl : symbTable.allDefnsOfAllNames.get(key)) {
							System.out.print(defl);
						}
						System.out.print("; ");
					}
					System.out.println();
					*/
					SemAn.defAt.put(nameExpr, defn);
					return null;
				} catch (CannotFndNameException e) {
					/*
					 * 
					 System.out.println("DID NOT FIND " + nameExpr.name + " in depth " + symbTable.currDepth + " among :");
					 for (String key : symbTable.allDefnsOfAllNames.keySet()) {
						System.out.printf("%s: ", key);
						for (NameResolver.SymbTable.ScopedDefn defl : symbTable.allDefnsOfAllNames.get(key)) {
							System.out.print(defl);
						}
						System.out.print("; ");
					}
					System.out.println();
					*/
					throw new Report.Error(nameExpr, "Undefined variable name: " + nameExpr.name);
				}
			default:
				throw new Report.InternalError();
		}
	}

	// ===== SYMBOL TABLE =====

	/**
	 * A symbol table.
	 */
	public class SymbTable {

		/**
		 * A symbol table record denoting a definition of a name within a certain scope.
		 */
		private class ScopedDefn {

			/** The depth of the scope the definition belongs to. */
			public final int depth;

			/** The definition. */
			public final AST.Defn defn;

			/**
			 * Constructs a new record denoting a definition of a name within a certain
			 * scope.
			 * 
			 * @param depth The depth of the scope the definition belongs to.
			 * @param defn  The definition.
			 */
			public ScopedDefn(int depth, AST.Defn defn) {
				this.depth = depth;
				this.defn = defn;
			}

			public String toString() {
				return String.format("[%s-%d]", this.defn.toString(), this.depth);
			}

		}

		/**
		 * A mapping of names into lists of records denoting definitions at different
		 * scopes. At each moment during the lifetime of a symbol table, the definition
		 * list corresponding to a particular name contains all definitions that name
		 * within currently active scopes: the definition at the inner most scope is the
		 * first in the list and is visible, the other definitions are hidden.
		 */
		private final HashMap<String, LinkedList<ScopedDefn>> allDefnsOfAllNames;

		/**
		 * The list of scopes. Each scope is represented by a list of names defined
		 * within it.
		 */
		private final LinkedList<LinkedList<String>> scopes;

		/** The depth of the currently active scope. */
		private int currDepth;

		/** Whether the symbol table can no longer be modified or not. */
		private boolean lock;

		/**
		 * Constructs a new symbol table.
		 */
		public SymbTable() {
			allDefnsOfAllNames = new HashMap<String, LinkedList<ScopedDefn>>();
			scopes = new LinkedList<LinkedList<String>>();
			currDepth = 0;
			lock = false;
			newScope();
		}

		/**
		 * Returns the depth of the currently active scope.
		 * 
		 * @return The depth of the currently active scope.
		 */
		public int currDepth() {
			return currDepth;
		}
		//tbd: funckije may not have proper location v ast viewu.

		/**
		 * Inserts a new definition of a name within the currently active scope or
		 * throws an exception if this name has already been defined within this scope.
		 * Once the symbol table is locked, any attempt to insert further definitions
		 * results in an internal error.
		 * 
		 * @param name The name.
		 * @param defn The definition.
		 * @throws CannotInsNameException Thrown if this name has already been defined
		 *                                within the currently active scope.
		 */
		public void ins(String name, AST.Defn defn) throws CannotInsNameException {
			if (lock)
				throw new Report.InternalError();

			LinkedList<ScopedDefn> allDefnsOfName = allDefnsOfAllNames.get(name);
			if (allDefnsOfName == null) {
				allDefnsOfName = new LinkedList<ScopedDefn>();
				allDefnsOfAllNames.put(name, allDefnsOfName);
			}

			if (!allDefnsOfName.isEmpty()) {
				ScopedDefn defnOfName = allDefnsOfName.getFirst();
				if (defnOfName.depth == currDepth)
					throw new CannotInsNameException();
			}

			allDefnsOfName.addFirst(new ScopedDefn(currDepth, defn));
			scopes.getFirst().addFirst(name);
		}

		/**
		 * Returns the currently visible definition of the specified name. If no
		 * definition of the name exists within these scopes, an exception is thrown.
		 * 
		 * @param name The name.
		 * @return The definition.
		 * @throws CannotFndNameException Thrown if the name is not defined within the
		 *                                currently active scope or any scope enclosing
		 *                                it.
		 */
		public AST.Defn fnd(String name) throws CannotFndNameException {
			LinkedList<ScopedDefn> allDefnsOfName = allDefnsOfAllNames.get(name);
			

			if (allDefnsOfName == null)
				throw new CannotFndNameException();

			if (allDefnsOfName.isEmpty())
				throw new CannotFndNameException();

			return allDefnsOfName.getFirst().defn;
		}

		/** Used for selecting the range of scopes. */
		public enum XScopeSelector {
			/** All live scopes. */
			ALL,
			/** Currently active scope. */
			ACT,
		}

		/**
		 * Constructs a new scope within the currently active scope. The newly
		 * constructed scope becomes the currently active scope.
		 */
		public void newScope() {
			if (lock)
				throw new Report.InternalError();

			currDepth++;
			scopes.addFirst(new LinkedList<String>());
		}

		/**
		 * Destroys the currently active scope by removing all definitions belonging to
		 * it from the symbol table. Makes the enclosing scope the currently active
		 * scope.
		 */
		public void oldScope() {
			if (lock)
				throw new Report.InternalError();

			if (currDepth == 0)
				throw new Report.InternalError();

			for (String name : scopes.getFirst()) {
				allDefnsOfAllNames.get(name).removeFirst();
			}
			scopes.removeFirst();
			currDepth--;
		}

		/**
		 * Prevents further modifications of this symbol table.
		 */
		public void lock() {
			lock = true;
		}

		/**
		 * An exception thrown when the name cannot be inserted into a symbol table.
		 */
		@SuppressWarnings("serial")
		public class CannotInsNameException extends Exception {

			/**
			 * Constructs a new exception.
			 */
			private CannotInsNameException() {
			}

		}

		/**
		 * An exception thrown when the name cannot be found in the symbol table.
		 */
		@SuppressWarnings("serial")
		public class CannotFndNameException extends Exception {

			/**
			 * Constructs a new exception.
			 */
			private CannotFndNameException() {
			}

		}

	}

}
