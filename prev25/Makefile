JAVA		= java
JAVAC		= javac --release 22 -encoding UTF8
ANTLR           = org.antlr.v4.Tool
ANTLRDIR	= lib/antlr-4.13.2-complete.jar
RM		= 'rm' -fr
FIND		= 'find'

all	:
	if [ -d src/compiler/phase/lexan ] ; then $(MAKE) -C src/compiler/phase/lexan ; fi
	if [ -d src/compiler/phase/synan ] ; then $(MAKE) -C src/compiler/phase/synan ; fi
	$(JAVAC) --module-path $(ANTLRDIR) --source-path src -d bin src/compiler/Compiler.java
	if [ -d doc ] ; then $(MAKE) -C doc ; fi
	@echo ":-) OK"

.PHONY	: clean
clean	:
	if [ -d doc ] ; then $(MAKE) -C doc clean ; fi
	if [ -d src ] ; then $(MAKE) -C prg clean ; fi
	if [ -d src/compiler/phase/lexan ] ; then $(MAKE) -C src/compiler/phase/lexan clean ; fi
	if [ -d src/compiler/phase/synan ] ; then $(MAKE) -C src/compiler/phase/synan clean ; fi
	$(FIND) . -type f -iname "*~" -exec $(RM) {} \;
	$(FIND) . -type f -iname "*.class" -exec $(RM) {} \;
	$(RM) bin
