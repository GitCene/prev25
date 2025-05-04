package compiler.phase.imclin;

import java.util.*;
import compiler.phase.*;
import compiler.phase.abstr.*;
import compiler.phase.memory.*;
import compiler.phase.imcgen.*;

/**
 * Linearization of intermediate code.
 */
public class ImcLin extends Phase {

	/** All data chunks of the program. */
	private final static Vector<LIN.DataChunk> dataChunks = new Vector<LIN.DataChunk>();

	/** All code chinks of the program. */
	private final static Vector<LIN.CodeChunk> codeChunks = new Vector<LIN.CodeChunk>();

	/**
	 * Constructs a new phase for the linearization of intermediate code.
	 */
	public ImcLin() {
		super("imclin");
	}

	public void log() {
		Logger linLogger = new Logger(logger);
		for (LIN.DataChunk dataChunk : dataChunks)
			linLogger.log(dataChunk);
		for (LIN.CodeChunk codeChunk : codeChunks)
			linLogger.log(codeChunk);
	}

	/**
	 * Adds a data chunk to a collection of all data chunks of the program.
	 * 
	 * @param dataChunk A data chunk.
	 */
	public static void addDataChunk(LIN.DataChunk dataChunk) {
		dataChunks.add(dataChunk);
	}

	/**
	 * Returns a collection of all data chunks of the program.
	 * 
	 * @return A collection of all data chunks of the program.
	 */
	public static Vector<LIN.DataChunk> dataChunks() {
		return new Vector<LIN.DataChunk>(dataChunks);
	}

	/**
	 * Adds a code chunk to a collection of all code chunks of the program.
	 * 
	 * @param codeChunk A code chunk.
	 */
	public static void addCodeChunk(LIN.CodeChunk codeChunk) {
		codeChunks.add(codeChunk);
	}

	/**
	 * Returns a collection of all code chunks of the program.
	 * 
	 * @return A collection of all code chunks of the program.
	 */
	public static Vector<LIN.CodeChunk> codeChunks() {
		return new Vector<LIN.CodeChunk>(codeChunks);
	}

	// ===== LOGGER =====

	public static class Logger implements AST.NullVisitor<Object, String> {

		/** The logger the log should be written to. */
		private final compiler.common.logger.Logger logger;

		/**
		 * Constructs a new visitor with a logger the log should be written to.
		 * 
		 * @param logger The logger the log should be written to.
		 */
		public Logger(compiler.common.logger.Logger logger) {
			this.logger = logger;
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

		// *** CHUNK LOGGER ***

		public void log(LIN.DataChunk dataChunk) {
			if (logger == null)
				return;
			logger.begElement("datachunk");
			logger.addAttribute("label", dataChunk.label.name);
			logger.addAttribute("size", Long.toString(dataChunk.size));
			logger.addAttribute("init", dataChunk.init);
			logger.endElement();
		}

		public void log(LIN.CodeChunk codeChunk) {
			if (logger == null)
				return;
			logger.begElement("codechunk");
			logger.addAttribute("prologuelabel", codeChunk.frame.label.name);
			logger.addAttribute("bodylabel", codeChunk.entryLabel.name);
			logger.addAttribute("epiloguelabel", codeChunk.exitLabel.name);
			log(codeChunk.frame);
			for (IMC.Stmt stmt : codeChunk.stmts()) {
				logger.begElement("stmt");
				stmt.log(logger);
				logger.endElement();
			}
			logger.endElement();
		}

	}

}
