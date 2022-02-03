#!/bin/bash


/usr/bin/find $1 -name \*.log -execdir zip -m {}.zip {} \;
 
#/usr/bin/find $1 \( -name \*.zip -o -name \*.gz \) -execdir /cygdrive/c/Program\ Files/7-Zip/7z x -y -bd {} \; -execdir rm {} \;
