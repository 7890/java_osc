/*
 * Copyright (C) 2003-2014, C. Ramakrishnan / Illposed Software.
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import com.illposed.osc.utility.JavaToByteArrayConverter;
import com.illposed.osc.utility.OSCJavaToByteArrayConverter;
import com.illposed.osc.utility.ByteArrayToJavaConverter;
import com.illposed.osc.utility.OSCByteArrayToJavaConverter;
import com.illposed.osc.utility.OSCPackByteArrayToJavaConverter;
import com.illposed.osc.utility.Tagger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An simple (non-bundle) OSC message.
 *
 * An OSC <i>Message</i> is made up of
 * an <i>Address Pattern</i> (the receiver of the message)
 * and <i>Arguments</i> (the content of the message).
 *
 * @author Chandrasekhar Ramakrishnan
 * @author Thomas Brand
 */
public class OSCMessage extends AbstractOSCPacket {

	/**
	 * Java regular expression pattern matching a single invalid character.
	 * The invalid characters are:
	 * ' ', '#', '*', ',', '?', '[', ']', '{', '}'
	 */
	protected static final Pattern ILLEGAL_ADDRESS_CHAR
			= Pattern.compile("[ \\#\\*\\,\\?\\[\\]\\{\\}]");

	protected String address;
	protected List<Object> arguments;

	/**
	 * Creates an empty OSC Message.
	 * In order to send this OSC message,
	 * you need to set the address and optionally some arguments.
	 */
	public OSCMessage() {
		this(null);
	}

	/**
	 * Creates an Message with an address already initialized.
	 * @param address the recipient of this OSC message
	 */
	public OSCMessage(String address) {
		this(address, null);
	}

	/**
	 * Creates an Message with an address
	 * and arguments already initialized.
	 * @param address the recipient of this OSC message
	 * @param arguments the data sent to the receiver
	 */
	public OSCMessage(String address, Collection<Object> arguments) {

		super();
		checkAddress(address);
		this.address = address;
		if (arguments == null) {
			this.arguments = new LinkedList<Object>();
		} else {
			this.arguments = new ArrayList<Object>(arguments);
		}
	}

	//
	public OSCMessage(final String address, final String typetags, final byte[] blobBytes) {
		this(address, null);
		setTypetagString(typetags);

		//test if packed blob to choose converter
		final ByteArrayToJavaConverter conv;
		if(blobBytes.length > 0 && blobBytes[0]=='!')
		{
			conv=new OSCPackByteArrayToJavaConverter();
		}
		else
		{
			conv=new OSCByteArrayToJavaConverter();
		}
		addArguments(conv.convertArguments(blobBytes, typetags));
	}

	/**
	 * The receiver of this message.
	 * @return the receiver of this OSC Message
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * Set the address of this message.
	 * @param address the receiver of the message
	 */
	public OSCMessage setAddress(String address) {
		checkAddress(address);
		this.address = address;
		contentChanged();
		return this;
	}

	/**
	 * Add an argument to the list of arguments.
	 * @param argument a Float, Double, String, Character, Integer, Long, Boolean, null or an array of these
	 */
	public OSCMessage addArgument(Object argument) {
		arguments.add(argument);
		contentChanged();
		return this;
	}

	//wrapper
	public OSCMessage add(Object argument) {
		addArgument(argument);
		return this;
	}

	public OSCMessage addArguments(Collection<Object> arguments) {
		for (Object obj : arguments) {
			addArgument(obj);
		}
		return this;
	}

	//
	public OSCMessage clearArguments() {
		arguments.clear();
		contentChanged();
		return this;
	}

	/**
	 * The arguments of this message.
	 * @return the arguments to this message
	 */
	public List<Object> getArguments() {
		return Collections.unmodifiableList(arguments);
	}

	/**
	 * Throws an exception if the given address is invalid.
	 * We explicitly allow <code>null</code> here,
	 * because we want to allow to set the address in a lazy fashion.
	 * @param address to be checked for validity
	 */
	protected static void checkAddress(String address) {
		// NOTE We explicitly allow <code>null</code> here,
		// because we want to allow to set in a lazy fashion.
		if ((address != null) && !isValidAddress(address)) {
			throw new IllegalArgumentException("Not a valid OSC address: " + address);
		}
	}

	/**
	 * Checks whether a given string is a valid OSC <i>Address Pattern</i>.
	 * @param address to be checked for validity
	 * @return true if the supplied string constitutes a valid OSC address
	 */
	public static boolean isValidAddress(String address) {
		return (address != null)
				&& !address.isEmpty()
				&& address.charAt(0) == '/'
				&& !address.contains("//")
				&& !ILLEGAL_ADDRESS_CHAR.matcher(address).find();
	}

	/**
	 * Convert the address into a byte array.
	 * Used internally only.
	 * @param stream where to write the address to
	 */
	protected void computeAddressByteArray(JavaToByteArrayConverter stream) {
		stream.write(address);
	}

	/**
	 * Convert the arguments into a byte array.
	 * Used internally only.
	 * @param stream where to write the arguments to
	 */
	protected void computeArgumentsByteArray(JavaToByteArrayConverter stream) {
		stream.write(',');
		stream.writeTypes(arguments);
		computePlainArgumentsByteArray(stream);
	}

	//
	protected void computePlainArgumentsByteArray(JavaToByteArrayConverter stream) {
		for (final Object argument : arguments) {
			stream.write(argument);
		}
	}

	//implement abstract method from abstract superclass
	protected byte[] computeByteArray(JavaToByteArrayConverter stream) {
		computeAddressByteArray(stream);
		computeArgumentsByteArray(stream);
		return stream.toByteArray();
	}

	//implement abstract method from abstract superclass
	//to be overridden by subclasses of OSCMessage
	public JavaToByteArrayConverter getConverter()
	{
		final JavaToByteArrayConverter stream=new OSCJavaToByteArrayConverter();
		return stream;
	}
}//end class OSCMessage
//EOF
