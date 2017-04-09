CLASSPATH=".:/usr/share/java/antlr-complete.jar:$$CLASSPATH"
SETUP=cd src && CLASSPATH=$(CLASSPATH)

help:
	@echo "Please use \`make <target>' where <target> is one of:"
	@echo "  help"
	@echo "    to show this message"
	@echo "  src/org/pchapin/phoebe/PhoebeLexer.java"
	@echo "    to compile a lexer from PhoebeLexer.g4"
	@echo "  src/org/pchapin/phoebe/PhoebeLexer.class"
	@echo "    to compile a lexer from PhoebeLexer.java"
	@echo "  src/org/pchapin/phoebe/TreeNode.class"
	@echo "    to compile TreeNode.scala"
	@echo "  src/org/pchapin/phoebe/PrettyPrint.class"
	@echo "    to compile PrettyPrint.scala"
	@echo "  src/org/pchapin/phoebe/Main.class"
	@echo "    to compile Main.scala"
	@echo "  run"
	@echo "    to run Main"

src/org/pchapin/phoebe/PhoebeLexer.java: src/org/pchapin/phoebe/PhoebeLexer.g4
	$(SETUP) java org.antlr.v4.Tool org/pchapin/phoebe/PhoebeLexer.g4

src/org/pchapin/phoebe/PhoebeLexer.class: src/org/pchapin/phoebe/PhoebeLexer.java
	$(SETUP) javac org/pchapin/phoebe/PhoebeLexer.java

src/org/pchapin/phoebe/TreeNode.class: src/org/pchapin/phoebe/TreeNode.scala
	$(SETUP) scalac org/pchapin/phoebe/TreeNode.scala

src/org/pchapin/phoebe/PrettyPrint.class: src/org/pchapin/phoebe/PrettyPrint.scala
	$(SETUP) scalac org/pchapin/phoebe/PrettyPrint.scala

src/org/pchapin/phoebe/Main.class: \
		src/org/pchapin/phoebe/Main.scala \
		src/org/pchapin/phoebe/PhoebeLexer.class \
		src/org/pchapin/phoebe/PrettyPrint.class \
		src/org/pchapin/phoebe/TreeNode.class
	$(SETUP) scalac org/pchapin/phoebe/Main.scala

run: src/org/pchapin/phoebe/Main.class
	$(SETUP) scala org.pchapin.phoebe.Main ../examples/bubble.pcd

.PHONY: help run
