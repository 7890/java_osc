/*
 * Copyright (C) 2004-2014, C. Ramakrishnan / Illposed Software.
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCBundle;

import java.nio.charset.Charset;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Abstract utility class implementing ByteArrayToJavaConverter to convert a byte array,
 * conforming to the concrete byte stream format,
 * into Java objects. OSCPacket is the common interface used for results from calls to convert().
 *
 * @author Chandrasekhar Ramakrishnan
 * @author Thomas Brand
 */
public abstract class AbstractByteArrayToJavaConverter implements ByteArrayToJavaConverter {

	protected static final String BUNDLE_START = "#bundle";
	protected static final char BUNDLE_IDENTIFIER = BUNDLE_START.charAt(0);
	protected static final String NO_ARGUMENT_TYPES = "";

	protected String remoteHost="";
	protected int remotePort=0;

	/** Used to decode message addresses and string parameters. */
	protected Charset charset;

	/**
	 * Creates a helper object for converting from a byte array
	 * to an {@link OSCPacket} object.
	 */
	public AbstractByteArrayToJavaConverter() {

		this.charset = Charset.defaultCharset();
	}

	/**
	 * Returns the character set used to decode message addresses
	 * and string parameters.
	 * @return the character-encoding-set used by this converter
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Sets the character set used to decode message addresses
	 * and string parameters.
	 * @param charset the desired character-encoding-set to be used by this converter
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	/**
	 * Converts a byte array into an {@link OSCPacket}
	 * (either an {@link OSCMessage}, {@link OSCPackMessage} or {@link OSCBundle}).
	 * @param bytes the storage containing the raw OSC packet
	 * @param bytesLength indicates how many bytes the package consists of (<code>&lt;= bytes.length</code>)
	 * @return the successfully parsed OSC packet; in case of a problem,
	 *   a <code>RuntimeException</code> is thrown
	 */
	public abstract OSCPacket convert(byte[] bytes, int bytesLength);

	//have a way to know where the packet came from
	public abstract OSCPacket convert(byte[] bytes, int bytesLength, String remoteHost, int remotePort);

	/**
	 * Reads the time tag and convert it to a Java Date object.
	 * A timestamp is a 64 bit number representing the time in NTP format.
	 * The first 32 bits are seconds since 1900, the second 32 bits are
	 * fractions of a second.
	 * @return a {@link Date}
	 */
	protected Date readTimeTag(long t) {

		try
		{
			ByteBuffer buffer = ByteBuffer.allocate(8);//Long.BYTES);
			buffer.order(ByteOrder.BIG_ENDIAN);
			buffer.putLong(t);
			byte[] bytes=buffer.array();
			int index=0;

			final byte[] secondBytes = new byte[8];
			final byte[] fractionBytes = new byte[8];
			for (int bi = 0; bi < 4; bi++) {
				// clear the higher order 4 bytes
				secondBytes[bi] = 0;
				fractionBytes[bi] = 0;
			}
			// while reading in the seconds & fraction, check if
			// this timetag has immediate semantics
			boolean isImmediate = true;
			for (int bi = 4; bi < 8; bi++) {
				secondBytes[bi] = bytes[index]; index++;

				if (secondBytes[bi] > 0) {
					isImmediate = false;
				}
			}
			for (int bi = 4; bi < 8; bi++) {
				fractionBytes[bi] = bytes[index]; index++;

				if (bi < 7) {
					if (fractionBytes[bi] > 0) {
						isImmediate = false;
					}
				} else {
					if (fractionBytes[bi] > 1) {
						isImmediate = false;
					}
				}
			}

			if (isImmediate) {

				return OSCBundle.TIMESTAMP_IMMEDIATE;
			}

			final long secsSince1900 = new BigInteger(secondBytes).longValue();
			long secsSince1970 = secsSince1900 - OSCBundle.SECONDS_FROM_1900_TO_1970;

			// no point maintaining times in the distant past
			if (secsSince1970 < 0) {
				secsSince1970 = 0;
			}
			long fraction = new BigInteger(fractionBytes).longValue();

			// this line was cribbed from jakarta commons-net's NTP TimeStamp code
			fraction = (fraction * 1000) / 0x100000000L;

			// I do not know where, but I'm losing 1ms somewhere...
			fraction = (fraction > 0) ? fraction + 1 : 0;
			final long millisecs = (secsSince1970 * 1000) + fraction;
			return new Date(millisecs);

		}
		catch(Exception e)
		{e.printStackTrace();} ///ev. throw runtime exception
		return null; //new Date();///
	}//end readTimeTag()
} //end abstract class AbstractByteArrayToJavaConverter
//EOF
