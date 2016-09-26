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

#	find "$src/main/com/illposed/osc/" -name *.java > "$TMPFILE"
	find "$src/main/" -name *.java > "$TMPFILE"

	$JAVAC -classpath "$build" -d "$build" @"$TMPFILE"

	echo "compiling files in $src/test to directory $build ..."
	ls -1 "$src/test/"*.java > "$TMPFILE"
	$JAVAC -classpath "$build" -d "$build" @"$TMPFILE"
}

#========================================================================
compile_msgpack()
{
	echo "building msgpack library (org.msgpack.*)"
	echo "========================================"

	cp "$archive"/msgpack-java_0.8.9.tar.gz "$build"
	cd "$build"
	tar xf msgpack-java_0.8.9.tar.gz
	cd msgpack-java-0.8.9

	find "msgpack-core/src/main/" -name *.java > "$TMPFILE"

	$JAVAC -classpath "$build" -d "$build" @"$TMPFILE" 2>/dev/null
}

#========================================================================
create_java_osc_jar()
{
	echo ""
	echo "creating JavaOSC jar"
	echo "===================="

	cur="`pwd`"

	echo "Manifest-Version: 1.0" > "$build"/Manifest.txt
	echo "Main-Class: Main" >> "$build"/Manifest.txt
	echo "" >> "$build"/Manifest.txt

	cd "$build"

	cp "$src"/main/oscsend.java .
	cp "$src"/main/oscdump.java .
	cp -r "$doc" "$build"
	cp "$DIR"/LICENSE "$build"

	mkdir -p resources/images/
	cp "$src"/main/gfx/app_icon.png resources/images/

	#include oscsend, oscdump source as examples
	#include generated javadoc
	#include license
	jar cfm JavaOSC_"$NOW".jar "$build"/Manifest.txt com/ org/ \
		*.class oscsend.java oscdump.java doc LICENSE resources/
	ls -l JavaOSC_"$NOW".jar

	echo "build_jar done."
	echo "start with:"
	echo "java -jar "$build"/JavaOSC_"$NOW".jar"
}

#========================================================================
create_javadoc()
{
	echo "creating msgpack and JavaOSC doc"
	echo "================================"

	cd "$src/main"
	javadoc -quiet -private -linksource -sourcetab 2 -d "$doc" \
		-classpath . -sourcepath . \
		com.illposed.osc \
		com.illposed.osc.utility \

	cd "$build"/msgpack-java-0.8.9
	find "msgpack-core/src/main/" -name *.java > "$TMPFILE"
	javadoc -quiet -private -sourcetab 2 -d "$doc/msgpack" \
		-classpath . -sourcepath . \
		@"$TMPFILE"


	cd "$DIR"

}

for tool in java javac jar javadoc date; \
	do checkAvail "$tool"; done

mkdir -p "$build"
rm -rf "$build"/*

compile_msgpack
compile_java_osc
create_java_osc_jar
#create_javadoc

echo ""
echo "done."
