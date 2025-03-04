JAVA		= java
ANTLR		= org.antlr.v4.Tool
ANTLRDIR	= ../../../../lib/antlr-4.13.2-complete.jar
RM		= 'rm' -fr

lexer	: Prev25Lexer.java Prev25Lexer.interp Prev25Lexer.tokens
Prev25Lexer.java Prev25Lexer.interp Prev25Lexer.tokens : Prev25Lexer.g4
	$(JAVA) -cp $(ANTLRDIR):$(CLASSPATH) $(ANTLR) $<
	mv Prev25Lexer.java Prev25Lexer.java-orig
	sed 's/@SuppressWarnings({"all"/@SuppressWarnings({"doclint:missing", "all"/' < Prev25Lexer.java-orig > Prev25Lexer.java
	$(RM) Prev25Lexer.java-orig ../../../../bin/compiler/phase/lexan/Prev25Lexer.class

.PHONY	: clean
clean	:
	$(RM) *~
	$(RM) Prev25*.java* *interp *tokens
