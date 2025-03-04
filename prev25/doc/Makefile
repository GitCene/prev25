JAVADOC		= javadoc -Xmaxerrs 100000 -Xmaxwarns 100000
ANTLRDIR	= ../lib/antlr-4.13.2-complete.jar
RM		= 'rm' -fr
FIND		= 'find'

all	:
	$(JAVADOC) --module-path $(ANTLRDIR) --class-path ../src -d javadoc -private -subpackages compiler ../src/module-info.java

.PHONY	: clean
clean	:
	$(RM) javadoc
