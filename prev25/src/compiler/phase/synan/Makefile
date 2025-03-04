JAVA		= java
ANTLR		= org.antlr.v4.Tool
ANTLRDIR	= ../../../../lib/antlr-4.13.2-complete.jar
RM		= 'rm' -fr

parser	: Prev25Parser.java Prev25Parser.interp Prev25Parser.tokens 
Prev25Parser.java Prev25Parser.interp Prev25Parser.tokens : Prev25Parser.g4 ../lexan/Prev25Lexer.java ../lexan/Prev25Lexer.interp ../lexan/Prev25Lexer.tokens
	$(JAVA) -cp $(ANTLRDIR):$(CLASSPATH) $(ANTLR) -no-listener -no-visitor -lib ../lexan $<
	mv Prev25Parser.java Prev25Parser.java-orig
	sed 's/@SuppressWarnings({"all"/@SuppressWarnings({"doclint:missing", "all"/' < Prev25Parser.java-orig > Prev25Parser.java
	$(RM) Prev25Parser.java-orig ../../../../bin/compiler/phase/synan/Prev25Parser.class

../lexan/Prev25Lexer.java ../lexan/Prev25Lexer.interp ../lexan/Prev25Lexer.tokens :
	$(MAKE) -C ../lexan

.PHONY	: clean
clean	:
	$(RM) *~
	$(RM) Prev25*.java *interp *tokens
