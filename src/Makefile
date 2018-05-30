JFLAGS =
JC = javac
SRCDIR = $(shell pwd)
JAVADOCDIR = doc
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
		  ProcessDB.java \
		  DBImage.java \
		  PhotoMosaic.java

MAIN = PhotoMosaic

default: classes

classes: $(CLASSES:.java=.class)

run: classes
	cd .. && java -cp $(SRCDIR) $(MAIN)

javadoc: classes
	javadoc -d $(JAVADOCDIR) $(CLASSES)

clean:
	$(RM) *.class
