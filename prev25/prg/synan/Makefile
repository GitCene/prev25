JAVA            = java
ANTLRDIR        = ../lib/antlr-4.13.2-complete.jar
XSLTPROC	= xsltproc
RM              = 'rm' -fr

PHASE		= all
TARGETPHASE	?= $(PHASE)
LOGGEDPHASE	?= $(PHASE)

.PHONY	: %
%	: %.prev25
	$(RM) -fr $@-*.xml $@-*.html
	if $(JAVA) -cp ../bin:../src:$(ANTLRDIR) compiler.Compiler --xsl=../lib/xsl/ --logged-phase=$(LOGGEDPHASE) --target-phase=$(TARGETPHASE) $(OPTIONS) $< ; then for XML in `'ls' -1 $@-*.xml` ; do $(XSLTPROC) $$XML -o `basename $$XML xml`html; done ; fi

.PHONY  : clean
clean   :
	$(RM) *~
	$(RM) *xml *html
