#!/bin/bash

#xml ant file to build tarsos.jar
build_file=/home/joren/workspace/Tarsos/build/build.xml

#build jar and documentation
ant -q -f $build_file

