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
 * AbstractJavaToByteArrayConverter is an abstract helper class that translates
 * from Java types to their byte stream representations according to
 * the concrete byte-level spec.
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
public abstract class AbstractJavaToByteArrayConverter implements JavaToByteArrayConverter {

	/**
	 * baseline NTP time if bit-0=0 is 7-Feb-2036 @ 06:28:16 UTC
	 */
	protected static final long MSB_0_BASE_TIME = 2085978496000L;
	/**
	 * baseline NTP time if bit-0=1 is 1-Jan-1900 @ 01:00:00 UTC
	 */
	protected static final long MSB_1_BASE_TIME = -2208988800000L;

	protected final ByteArrayOutputStream stream;
	/** Used to encode message addresses and string parameters. */
	protected Charset charset;

	public AbstractJavaToByteArrayConverter() {

		this.stream = new ByteArrayOutputStream();
		this.charset = Charset.defaultCharset();
	}

	/**
	 * Returns the character set used to encode message addresses
	 * and string parameters.
	 * @return the character-encoding-set used by this converter
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Sets the character set used to encode message addresses
	 * and string parameters.
	 * @param charset the desired character-encoding-set to be used by this converter
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	/**
	 * Checks whether the given object is represented by a type that comes without data.
	 * @param anObject the object to inspect
	 * @return whether the object to check consists of only its type information
	 */
	protected boolean isNoDataObject(Object anObject) {
		return ((anObject instanceof OSCImpulse)
				|| (anObject instanceof Boolean)
				|| (anObject == null));
	}

	/**
	 * Write an object into the byte stream.
	 * @param anObject (usually) one of Float, Double, String, Character, Integer, Long,
	 *   or array of these.
	 */
	public void write(Object anObject) {
		// Can't do switch on class
		if (anObject instanceof Collection) {
			final Collection<Object> theArray = (Collection<Object>) anObject;
			for (final Object entry : theArray) {
				write(entry);
			}
		} else if (anObject instanceof Float) {
			write((Float) anObject);
		} else if (anObject instanceof Double) {
			write((Double) anObject);
		} else if (anObject instanceof String) {
			write((String) anObject);
		} else if (anObject instanceof byte[]) {
			write((byte[]) anObject);
		} else if (anObject instanceof Character) {
			write((Character) anObject);
		} else if (anObject instanceof Integer) {
			write((Integer) anObject);
		} else if (anObject instanceof Long) {
			write((Long) anObject);
		} else if (anObject instanceof Date) {
			write((Date) anObject);
		} else if (anObject instanceof ShortMessage) {
			write((ShortMessage) anObject);
		} else if (anObject instanceof OSCTypedBlob) {
			write((OSCTypedBlob) anObject);
		} else if (!isNoDataObject(anObject)) {
			throw new UnsupportedOperationException("Do not know how to write an object of class: "
					+ anObject.getClass());
		}
	}

	/**
	 * Convert the contents of the output stream to a byte array.
	 * @return the byte array containing the byte stream
	 */
	public abstract byte[] toByteArray();

	/**
	 * Write bytes into the byte stream.
	 * @param bytes bytes to be written
	 */
	public abstract void write(byte[] bytes);

	/**
	 * Write an integer into the byte stream.
	 * @param anInt the integer to be written
	 */
	public abstract void write(int anInt);

	/**
	 * Write a float into the byte stream.
	 * @param aFloat floating point number to be written
	 */
	public abstract void write(Float aFloat);

	/**
	 * Write a double into the byte stream (8 bytes).
	 * @param aDouble double precision floating point number to be written
	 */
	public abstract void write(Double aDouble);

	/**
	 * @param anInt the integer to be written
	 */
	public abstract void write(Integer anInt);

	/**
	 * @param aLong the double precision integer to be written
	 */
	public abstract void write(Long aLong);

	/**
	 * @param timestamp the timestamp to be written
	 */
	public abstract void write(Date timestamp);

	//
	public abstract void write(ShortMessage midievent);

	/**
	 * Write a string into the byte stream.
	 * @param aString the string to be written
	 */
	public abstract void write(String aString);

	/**
	 * Write a char into the byte stream, and ensure it is 4 byte aligned again.
	 * @param aChar the character to be written
	 */
	public abstract void write(Character aChar);

	/**
	 * Write a char into the byte stream.
	 * CAUTION, this does not ensure 4 byte alignment (it actually breaks it)!
	 * @param aChar the character to be written
	 */
	public abstract void write(char aChar);

	///
	public abstract void write(OSCTypedBlob typedBlob);

	/**
	 * Write the OSC specification type tag for the type a certain Java type
	 * converts to.
	 * @param typeClass Class of a Java object in the arguments
	 */
///	public abstract void writeType(Class typeClass);

	/**
	 * Write types for the arguments.
	 * @param arguments the arguments to an OSCMessage
	 */
	public abstract void writeTypes(Collection<Object> arguments);
}//end class AbstractJavaToByteArrayConverter
//EOF
