/*
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import com.illposed.osc.utility.JavaToByteArrayConverter;
import com.illposed.osc.utility.OSCJavaToOSCPackByteArrayConverter;

import java.util.Collection;

/**
 * An simple (non-bundle) OSCPack message.
 *
 * An OSCPack <i>Message</i> is made up of
 * an <i>Address Pattern</i> (the receiver of the message)
 * and <i>Arguments</i> (the content of the message).
 *
 * @author Thomas Brand
 */
public class OSCPackMessage extends OSCMessage {
	/**
	 * Creates an empty OSCPack message.
	 * In order to send this OSCPack message,
	 * you need to set the address and optionally some arguments.
	 */
	public OSCPackMessage() {
		super();
	}

	/**
	 * Creates an OSCPack message with an address already initialized.
	 * @param address the recipient of this OSCPack message
	 */
	public OSCPackMessage(String address) {
		super(address);
	}

	/**
	 * Creates an OSCPack message with an address
	 * and arguments already initialized.
	 * @param address the recipient of this OSC message
	 * @param arguments the data sent to the receiver
	 */
	public OSCPackMessage(String address, Collection<Object> arguments) {

		super(address,arguments);
	}

	//compared to plain OSC: omit ",". Used internally.
	@Override
	protected void computeArgumentsByteArray(JavaToByteArrayConverter stream) {
		stream.writeTypes(arguments);
		for (final Object argument : arguments) {
			stream.write(argument);
		}
	}

	/**
	 * Generate a representation of this packet conforming to the
	 * the OSCPack byte stream specification (currently non-existing ...). Used Internally.
	 */
	@Override
	protected byte[] computeByteArray() {
		//creating concrete converter (implementing interface JavaToByteArrayConverter)
		final OSCJavaToOSCPackByteArrayConverter stream = new OSCJavaToOSCPackByteArrayConverter();
		stream.setCharset(charset);
		return computeByteArray(stream);
	}
}//end class OSCPackMessage
//EOF
