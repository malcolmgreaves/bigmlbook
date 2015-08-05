[![Stories in Ready](https://badge.waffle.io/malcolmgreaves/bigmlbook.png?label=ready&title=Ready)](https://waffle.io/malcolmgreaves/bigmlbook)
[![Build Status](https://travis-ci.org/malcolmgreaves/bigmlbook.svg?branch=master)](https://travis-ci.org/malcolmgreaves/bigmlbook) [![Coverage Status](https://img.shields.io/coveralls/malcolmgreaves/bigmlbook.svg)](https://coveralls.io/r/malcolmgreaves/bigmlbook)
==========================================================================================
Using sbt for building, tests, running programs, and interative shell.
==========================================================================================

We recommend using the following SBT options:
	 SBT_OPTS="-Xmx2G -XX:MaxPermSize=724M  -XX:+UseConcMarkSweepGC  -XX:+CMSClassUnloadingEnabled

./sbt test
	Downloads dependencies, compiles, packages, and runs all unit tests" 

./sbt
	Interactive sbt shell

[TODO] Unified running programs
[TODO] Interative spark shell with all project jars in classpath

==========================================================================================
To compile all dependencies into bytecode and pacakge into a single jar
==========================================================================================

$ sbt assembly

==========================================================================================
To create an IntelliJ IDEA or Eclipse project
==========================================================================================

$ sbt gen-idea

or

$ sbt eclipse
