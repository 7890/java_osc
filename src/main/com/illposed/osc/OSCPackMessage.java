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

	public OSCPackMessage() {
		super();
	}

	public OSCPackMessage(String address) {
		super(address);
	}

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

	//return concrete converter
	@Override
        public JavaToByteArrayConverter getConverter()
        {
                final JavaToByteArrayConverter stream=new OSCJavaToOSCPackByteArrayConverter();
                return stream;
        }
}//end class OSCPackMessage
//EOF
