SRCDIR = src
default:
	$(MAKE) -C $(SRCDIR)

run:
	$(MAKE) -C $(SRCDIR) run

javadoc:
	$(MAKE) -C $(SRCDIR) javadoc

clean:
	$(MAKE) -C $(SRCDIR) clean
