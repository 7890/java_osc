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
import com.illposed.osc.OSCMessage;

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
	 * (either an {@link OSCMessage} or a {@link OSCBundle}).
	 * @param bytes the storage containing the raw OSC packet
	 * @param bytesLength indicates how many bytes the package consists of (<code>&lt;= bytes.length</code>)
	 * @return the successfully parsed OSC packet; in case of a problem,
	 *   a <code>RuntimeException</code> is thrown
	 */
	public abstract OSCPacket convert(byte[] bytes, int bytesLength);

	//have a way to know where the packet came from
	public abstract OSCPacket convert(byte[] bytes, int bytesLength, String remoteHost, int remotePort);

	//create a list of java objects from a blob (packed or not) from given typetags
	public abstract List<Object> convertArguments(byte[] blob, String typetags);
} //end abstract class AbstractByteArrayToJavaConverter
//EOF
