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
import com.illposed.osc.OSCTypedBlob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import javax.sound.midi.ShortMessage;

/**
 * OSCJavaToByteArrayConverter is a helper class that translates
 * from Java types to their byte stream representations according to
 * the OSC spec.
 *
 * The implementation is based on
 * <a href="http://www.emergent.de">Markus Gaelli</a> and
 * Iannis Zannos's OSC implementation in Squeak (a Smalltalk dialect).
 *
 * This version includes bug fixes and improvements from
 * Martin Kaltenbrunner and Alex Potsides.
 *
 * @author Chandrasekhar Ramakrishnan
 * @author Martin Kaltenbrunner
 * @author Alex Potsides
 * @author Thomas Brand
 */
public class OSCJavaToByteArrayConverter extends AbstractJavaToByteArrayConverter {

	private final byte[] intBytes;
	private final byte[] longintBytes;

	public OSCJavaToByteArrayConverter() {
		super();

		this.intBytes = new byte[4];
		this.longintBytes = new byte[8];
	}

	/**
	 * Align the stream by padding it with '0's so it has a size divisible by 4.
	 */
	private void alignStream() {
		final int alignmentOverlap = stream.size() % 4;
		final int padLen = (4 - alignmentOverlap) % 4;
		for (int pci = 0; pci < padLen; pci++) {
			stream.write(0);
		}
	}

	/**
	 * Convert the contents of the output stream to a byte array.
	 * @return the byte array containing the byte stream
	 */
	public byte[] toByteArray() {
		return stream.toByteArray();
	}

	/**
	 * Write bytes into the byte stream.
	 * @param bytes bytes to be written
	 */
	public void write(byte[] bytes) {
		writeInteger32ToByteArray(bytes.length);
		writeUnderHandler(bytes);
		alignStream();
	}

	/**
	 * Write an integer into the byte stream.
	 * @param anInt the integer to be written
	 */
	public void write(int anInt) {
		writeInteger32ToByteArray(anInt);
	}

	/**
	 * Write a float into the byte stream.
	 * @param aFloat floating point number to be written
	 */
	public void write(Float aFloat) {
		writeInteger32ToByteArray(Float.floatToIntBits(aFloat));
	}

	/**
	 * Write a double into the byte stream (8 bytes).
	 * @param aDouble double precision floating point number to be written
	 */
	public void write(Double aDouble) {
		writeInteger64ToByteArray(Double.doubleToRawLongBits(aDouble));
	}

	/**
	 * @param anInt the integer to be written
	 */
	public void write(Integer anInt) {
		writeInteger32ToByteArray(anInt);
	}

	/**
	 * @param aLong the double precision integer to be written
	 */
	public void write(Long aLong) {
		writeInteger64ToByteArray(aLong);
	}

	/**
	 * @param timestamp the timestamp to be written
	 */
	public void write(Date timestamp) {
		writeInteger64ToByteArray(NTPTime.javaToNtpTimeStamp(timestamp.getTime()));
	}

	//
	public void write(ShortMessage midievent) {
		//write bytes (>0, <=3)
		byte[] b=midievent.getMessage();
		for(int i=0;i<midievent.getLength();i++)
		{
			stream.write(b[i]);
		}
		//pad to 4 bytes
		alignStream();
	}

	/**
	 * Write a string into the byte stream.
	 * @param aString the string to be written
	 */
	public void write(String aString) {
		final byte[] stringBytes = aString.getBytes(charset);
		writeUnderHandler(stringBytes);
		stream.write(0);
		alignStream();
	}

	/**
	 * Write a char into the byte stream, and ensure it is 4 byte aligned again.
	 * @param aChar the character to be written
	 */
	public void write(Character aChar) {
		//it's probably safe to assume that stream is aligned here (previous addition has taken care)
		stream.write(0);//((char)'\0');
		stream.write(0);
		stream.write(0);
		stream.write((char)aChar);
		/*
		stream.write((char) aChar);
		alignStream(); //aligning here after putting char would make it aligned on the 'left' side

		note: type 'c': an ascii character, sent as 32 bits
		where to put the char inside these 32 bits?
		-liblo: 'a': 00 00 00 61
		vs      'a'  61 00 00 00
		*/
	}

	/**
	 * Write a char into the byte stream.
	 * CAUTION, this does not ensure 4 byte alignment (it actually breaks it)!
	 * @param aChar the character to be written
	 */
	public void write(char aChar) {
		stream.write(aChar);
	}

        public void write(OSCTypedBlob typedBlob)
	{
		try {
			write((Character)typedBlob.getType());
			write((int)typedBlob.getCount());
			write(typedBlob.write());
		} catch(Exception e){throw new RuntimeException(e);}
	}

	/**
	 * Write types for the arguments.
	 * @param arguments the arguments to an OSCMessage
	 */
	public void writeTypes(Collection<Object> arguments) {

		///writeTypesArray(arguments);
		String tags=Tagger.getTypesArray(arguments);

		//iterate string as bytes, write to stream
		final byte[] stringBytes = tags.getBytes(charset);
		for(int i=0;i<stringBytes.length;i++)
		{
			stream.write(stringBytes[i]);
		}
		// we always need to terminate with a zero,
		// even if (especially when) the stream is already aligned.
		stream.write(0);
		// align the stream with padded bytes
		alignStream();
	}

	/**
	 * Write bytes to the stream, catching IOExceptions and converting them to
	 * RuntimeExceptions.
	 * @param bytes to be written to the stream
	 */
	private void writeUnderHandler(byte[] bytes) {

		try {
			stream.write(bytes);
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
					+ " IOException writing to a ByteArrayOutputStream", ex);
		}
	}

	/**
	 * Write a 32 bit integer to the byte array without allocating memory.
	 * @param value a 32 bit integer.
	 */
	private void writeInteger32ToByteArray(int value) {
		//byte[] intBytes = new byte[4];
		//I allocated the this buffer globally so the GC has less work

		intBytes[3] = (byte)value; value >>>= 8;
		intBytes[2] = (byte)value; value >>>= 8;
		intBytes[1] = (byte)value; value >>>= 8;
		intBytes[0] = (byte)value;

		writeUnderHandler(intBytes);
	}

	/**
	 * Write a 64 bit integer to the byte array without allocating memory.
	 * @param value a 64 bit integer.
	 */
	private void writeInteger64ToByteArray(long value) {
		longintBytes[7] = (byte)value; value >>>= 8;
		longintBytes[6] = (byte)value; value >>>= 8;
		longintBytes[5] = (byte)value; value >>>= 8;
		longintBytes[4] = (byte)value; value >>>= 8;
		longintBytes[3] = (byte)value; value >>>= 8;
		longintBytes[2] = (byte)value; value >>>= 8;
		longintBytes[1] = (byte)value; value >>>= 8;
		longintBytes[0] = (byte)value;

		writeUnderHandler(longintBytes);
	}
}//end class OSCJavaToByteArrayConverter
//EOF
