.SECONDARY:
.PHONY: clean cleaner cleanest

LATEX=xelatex -halt-on-error
BIBTEX=bibtex

all : preprint.pdf

preprint.pdf : *.tex Makefile
	$(LATEX) preprint.tex

# preprint.bbl : ca.bib biblio.tex
# 	$(LATEX) $*
# 	$(BIBTEX) $*
# 	$(LATEX) $*

clean :
	rm -f *.bbl
	rm -f *.blg
	rm -f *.log
	rm -f *.aux
	rm -f *.snm
	rm -f *.toc
	rm -f *.nav
	rm -f *.out
	rm -rf _region* *.prv

cleaner : clean

cleanest : cleaner
	rm -f preprint.pdf
