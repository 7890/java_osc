/*
 * Copyright (C) 2003-2014, C. Ramakrishnan / Illposed Software.
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

import com.illposed.osc.OSCImpulse;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.buffer.OutputStreamBufferOutput;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import javax.sound.midi.ShortMessage;

/*
class to help writing OSCPack byte stream:

! (byte) bang, indicating OSCPack
path (string)
typetags (string)
args (allowed OSC types)

the items are encoded using MessagePack (http://msgpack.org)

a parser should understand the msgpack format and then:
-parse !
-get address as string (valid osc path, starting with '/')
-get typetags as string 
-get args according to typetags

*/
public class OSCJavaToOSCPackByteArrayConverter extends AbstractJavaToByteArrayConverter {

	private MessagePacker packer;

	public OSCJavaToOSCPackByteArrayConverter() {

		super();

		this.packer = MessagePack.newDefaultPacker(
			new OutputStreamBufferOutput(this.stream)
		);

		//add first byte to indicate OSCPack (non-osc compatible)
		try{packer.packByte((byte)'!');} catch (Exception e){throwEx("",e);}
	}

	public byte[] toByteArray() {
		try {
			packer.flush();
			return stream.toByteArray();
		} catch (IOException e) {throwEx("",e);}
		return null;
	}

	public void write(byte[] bytes) {
		try {
			packer.packBinaryHeader(bytes.length);
			packer.writePayload(bytes);
		} catch (IOException e) {throwEx("",e);}
	}

	public void write(int anInt) {
		try {
			packer.packInt(anInt);
		} catch (IOException e) {throwEx("",e);}
	}

	public void write(Float aFloat) {
		try {
			packer.packFloat(aFloat);
		} catch (IOException e) {throwEx("",e);}
	}

	public void write(Double aDouble) {
		try {
			packer.packDouble(aDouble);
		} catch (IOException e) {throwEx("",e);}
	}

	public void write(Integer anInt) {
		try {
			packer.packInt((int)anInt);
		} catch (IOException e) {throwEx("",e);}
	}

	public void write(Long aLong) {
		try {
			packer.packLong(aLong);
		} catch (IOException e) {throwEx("",e);}
	}

	public void write(Date timestamp) {
		try {
			packer.packLong(NTPTime.javaToNtpTimeStamp(timestamp.getTime()));
		} catch (IOException e) {throwEx("",e);}
	}

	public void write(ShortMessage midievent) {
		//write bytes (>0, <=3)
		try {
			packer.packBinaryHeader(midievent.getLength());
			packer.writePayload(midievent.getMessage());
		} catch (IOException e) {throwEx("",e);}

	}

	public void write(String aString) {
		try {
			final byte[] stringBytes = aString.getBytes(charset);
			int slen=aString.length();
			packer.packRawStringHeader(slen);
			packer.writePayload(stringBytes);
			//packer.packString(aString); //ev. issue with charset
		} catch (IOException e) {throwEx("",e);}
	}

	public void write(Character aChar) {
		write(aChar.charValue());
	}

	public void write(char aChar) {
		try {
			packer.packByte((byte)aChar);
		} catch (IOException e) {throwEx("",e);}
	}

	public void writeType(Class typeClass, StringBuffer sb) {

		Tagger.addTypeToStringBuffer(typeClass, sb);
	}

	private void writeTypesArray(Collection<Object> arguments) {
		try {
			packer.packString(Tagger.getTypesArray(arguments));
		} catch (IOException e) {throwEx("",e);}
	}

	//this looks redundant to writeTypesArray (for this converter) however implemented here to satisfy interface
	public void writeTypes(Collection<Object> arguments) {

		writeTypesArray(arguments);
	}

	private void throwEx(String msg, Exception e)
	{
		throw new RuntimeException("There was an error in OSCJavaToOSCPackByteArrayConverter: "+msg,e);
	}

}//end class OSCJavaToOSCPackByteArrayConverter
//EOF
