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

/**
 */
public class OSCJavaToOSCPackByteArrayConverter extends AbstractJavaToByteArrayConverter {

	private MessagePacker packer;

	public OSCJavaToOSCPackByteArrayConverter() {

		super();

		this.packer = MessagePack.newDefaultPacker(
			new OutputStreamBufferOutput(this.stream)
		);
		//add first byte to indicate non-osc compatible

		try
		{
			packer.packByte((byte)'!');
		} catch (Exception ex)
		{
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * Convert the contents of the output stream to a byte array.
	 * @return the byte array containing the byte stream
	 */
	public byte[] toByteArray() {
		try {
			packer.flush();
		return stream.toByteArray();
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * Write bytes into the byte stream.
	 * @param bytes bytes to be written
	 */
	public void write(byte[] bytes) {
		try {
			packer.packBinaryHeader(bytes.length);
			packer.writePayload(bytes);
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * Write an integer into the byte stream.
	 * @param anInt the integer to be written
	 */
	public void write(int anInt) {
		try {
			packer.packInt(anInt);
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * Write a float into the byte stream.
	 * @param aFloat floating point number to be written
	 */
	public void write(Float aFloat) {
		try {
			packer.packFloat(aFloat);
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * Write a double into the byte stream (8 bytes).
	 * @param aDouble double precision floating point number to be written
	 */
	public void write(Double aDouble) {
		try {
			packer.packDouble(aDouble);
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * @param anInt the integer to be written
	 */
	public void write(Integer anInt) {
		try {
			packer.packInt((int)anInt);
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * @param aLong the double precision integer to be written
	 */
	public void write(Long aLong) {
		try {
			packer.packLong(aLong);
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * @param timestamp the timestamp to be written
	 */
	public void write(Date timestamp) {
		try {
			packer.packLong(javaToNtpTimeStamp(timestamp.getTime()));
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * Write a string into the byte stream.
	 * @param aString the string to be written
	 */
	public void write(String aString) {

		final byte[] stringBytes = aString.getBytes(charset);
		int slen=aString.length();

		try {
			packer.packRawStringHeader(slen);
			packer.writePayload(stringBytes);
			//packer.packString(aString); //ev. issue with charset
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * Write a char into the byte stream, and ensure it is 4 byte aligned again.
	 * @param aChar the character to be written
	 */
	public void write(Character aChar) {

		write(aChar.charValue());
	}

	/**
	 * Write a char into the byte stream.
	 * CAUTION, this does not ensure 4 byte alignment (it actually breaks it)!
	 * @param aChar the character to be written
	 */
	public void write(char aChar) {
		try {
			packer.packByte((byte)aChar);

		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
				+ " IOException writing to packer", ex);
		}
	}

	/**
	 * Write the OSC specification type tag for the type a certain Java type
	 * converts to.
	 * @param typeClass Class of a Java object in the arguments
	 */
	public void writeType(Class typeClass, StringBuffer sb) {

		// A big ol' else-if chain -- what's polymorphism mean, again?
		// I really wish I could extend the base classes!
		if (Integer.class.equals(typeClass)) {
			sb.append("i");
		} else if (Long.class.equals(typeClass)) {
			sb.append("h");
		} else if (Date.class.equals(typeClass)) {
			sb.append("t");
		} else if (Float.class.equals(typeClass)) {
			sb.append("f");
		} else if (Double.class.equals(typeClass)) {
			sb.append("d");
		} else if (String.class.equals(typeClass)) {
			sb.append("s");
		} else if (byte[].class.equals(typeClass)) {
			sb.append("b");
		} else if (Character.class.equals(typeClass)) {
			sb.append("c");
		} else if (OSCImpulse.class.equals(typeClass)) {
			sb.append("I");
		} else {
			throw new UnsupportedOperationException("Do not know the OSC type for the java class: "
					+ typeClass);
		}
	}

	/**
	 * Write the types for an array element in the arguments.
	 * @param arguments array of base Objects
	 */
	private void writeTypesArray(Collection<Object> arguments) {

		StringBuffer sb=new StringBuffer();
		try {
			for (final Object argument : arguments) {
				if (null == argument) {
					sb.append("N");
				} else if (argument instanceof Collection) {
					// If the array at i is a type of array, write a '['.
					// This is used for nested arguments.
					sb.append("[");
					// fill the [] with the SuperCollider types corresponding to
					// the object (e.g., Object of type String needs -s).
					// XXX Why not call this function, recursively? 
					// The only reason would be, to not allow nested arrays, but the 
					// specification does not say anythign about them not being allowed.
					writeTypesArray((Collection<Object>) argument);
					// close the array
					sb.append("]");
				} else if (Boolean.TRUE.equals(argument)) {
					sb.append("T");
				} else if (Boolean.FALSE.equals(argument)) {
					sb.append("F");
				} else {
					// go through the array and write the superCollider types as shown
					// in the above method.
					// The classes derived here are used as the arg to the above method.
					writeType(argument.getClass(),sb);
				}
			}
			packer.packString(sb.toString());
		}
		catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
					+ " IOException writing to packer", ex);
		}
	}

	/**
	 * Write types for the arguments.
	 * @param arguments the arguments to an OSCMessage
	 */
	public void writeTypes(Collection<Object> arguments) {

		writeTypesArray(arguments);
	}
}//end class OSCJavaToOSCPackByteArrayConverter
//EOF
