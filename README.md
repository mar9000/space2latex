# Space2Latex
Space2Latex can convert a set of Confluence Wiki pages into one or more LaTeX documents.

It has been developed to build the pdf version of the [MPS Confluence wiki](https://confluence.jetbrains.com/pages/viewpage.action?pageId=59935410)

There are other projects that can translate from HTML-like format to LaTeX/pdf, probably the most updated one is [Pandoc](http://pandoc.org/). The main difference with Space2Latex is that they translate a single document into another one, while Space2Latex is a sort of *bookbinder* due to the fact that it assembles one or more pages into a single document and **HTML links that point to an included page are translated to internal pdf links.**

# Introduction

Space2Latex works in two phases:

  1. download: with the `--command=download` parameter it downloads one or more pages through the [Confluence REST API](https://developer.atlassian.com/confdev/confluence-rest-api) and save pages with the associated images into the directory you pass as parameter.
  
  1. format: with the `--command=format` parameter it create one or more TeX files that can be processed with `pdflatex`. The TeX files that should be created, as well as parts to create, which pages to include in each TeX file, authors, etc., are specified with a simple XML file written by hand.

The format process uses [StringTemplate](http://www.stringtemplate.org/) to output the resulting text. There is a default template that can be customized.

## Getting started

Once you have cloned the github project you should be able to perform the following actions either using Eclipse or ant:

  1. compile the project either with Eclipse or with ant.
  1. download the example page.
  1. format the example page.

## Download

## Format

## Limitations

Space2Latex is today able to translate only the HTML elements and Confluence XML elements actually used by the MPS Confluence wiki. Translate to LaTeX/pdf a generic Confluence space is not in the scope of this project.

