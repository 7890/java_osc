#!/bin/bash

FULLPATH="`pwd`/$0"
DIR=`dirname "$FULLPATH"`

src="$DIR"/src
build="$DIR"/_build
archive="$DIR"/archive
doc="$DIR"/doc

jsource=1.6
jtarget=1.6

#linux / osx different mktemp call
TMPFILE=`mktemp 2>/dev/null || mktemp -t /tmp`

JAVAC="javac -source $jsource -target $jtarget -nowarn"

NOW=`date +"%s"`

#========================================================================
checkAvail()
{
	which "$1" >/dev/null 2>&1
	ret=$?
	if [ $ret -ne 0 ]
	then
		echo "tool \"$1\" not found. please install"
		exit 1
	fi
}

#========================================================================
compile_java_osc()
{
	echo "building JavaOSC library (com.illposed.osc)"
	echo "==========================================="

	echo "compiling files in $src/main to directory $build ..."

	find "$src/main/com/illposed/osc/" -name *.java > "$TMPFILE"
	$JAVAC -classpath "$build" -sourcepath "$src/main" -d "$build" @"$TMPFILE"
}

#========================================================================
create_java_osc_jar()
{
	echo ""
	echo "creating JavaOSC jar"
	echo "===================="

	cur="`pwd`"

	echo "Manifest-Version: 1.0" > "$build"/Manifest.txt
#	echo "Main-Class: xxx" >> "$build"/Manifest.txt
	echo "" >> "$build"/Manifest.txt

	cd "$build"

	jar cfm JavaOSC_"$NOW".jar "$build"/Manifest.txt com/
	ls -l JavaOSC_"$NOW".jar

	echo "build_jar done."
}

#========================================================================
create_javadoc()
{
	echo "creating JavaOSC doc"
	echo "===================="

	cd "$src/main"
	javadoc -quiet -private -linksource -sourcetab 2 -d "$doc" \
		-classpath . -sourcepath . \
		com.illposed.osc \
		com.illposed.osc.utility
	cd "$DIR"
}

for tool in java javac jar javadoc date; \
	do checkAvail "$tool"; done

mkdir -p "$build"
rm -rf "$build"/*

compile_java_osc
create_java_osc_jar
create_javadoc

echo ""
echo "done."
