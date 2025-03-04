package compiler.phase.synan;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import compiler.common.report.*;
import compiler.phase.*;
import compiler.phase.lexan.*;

/**
 * Syntax analysis phase.
 */
public class SynAn extends Phase {

	/** The parse tree. */
	public static Prev25Parser.SourceContext tree;

	/** The ANTLR parser that actually performs syntax analysis. */
	public final Prev25Parser parser;

	/**
	 * Phase construction: sets up logging and the ANTLR lexer and parser.
	 * 
	 * @param lexan The lexical analyzer.
	 */
	public SynAn(final LexAn lexan) {
		super("synan");
		parser = new Prev25Parser(new CommonTokenStream(lexan.lexer));
		parser.removeErrorListeners();
		parser.addErrorListener(new BaseErrorListener() {
			public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
					final int charPositionInLine, final String msg, final RecognitionException e) {
				throw new Report.Error(new Location(line, charPositionInLine), //
						"Unexpected symbol '" + ((LexAn.LocLogToken) offendingSymbol).getText() + "'.");
			}
		});
	}

	/**
	 * Logs a parse tree.
	 * 
	 * @param tree The parse tree to be logged.
	 */
	public void log(final ParseTree tree) {
		if (logger == null)
			return;
		if (tree instanceof TerminalNodeImpl) {
			final TerminalNodeImpl node = (TerminalNodeImpl) tree;
			((LexAn.LocLogToken) (node.getPayload())).log(logger);
		}
		if (tree instanceof ParserRuleContext) {
			final ParserRuleContext node = (ParserRuleContext) tree;
			logger.begElement("node");
			logger.addAttribute("label", Prev25Parser.ruleNames[node.getRuleIndex()]);
			final int numChildren = node.getChildCount();
			for (int i = 0; i < numChildren; i++)
				log(node.getChild(i));
			logger.endElement();
		}
	}

}
