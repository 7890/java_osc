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

public class OSCShortcutPackMessage extends OSCShortcutMessage {

	public OSCShortcutPackMessage() {
		super();
	}

	public OSCShortcutPackMessage(String address) {
		super(address);
	}

	public OSCShortcutPackMessage(String address, Collection<Object> arguments) {

		super(address,arguments);
	}

	@Override
	protected void computeArgumentsByteArray(JavaToByteArrayConverter stream) {
		stream.writeTypes(arguments);
		for (final Object argument : arguments) {
			stream.write(argument);
		}
	}

	@Override
	public JavaToByteArrayConverter getConverter()
	{
		final JavaToByteArrayConverter stream=new OSCJavaToOSCPackByteArrayConverter();
		return stream;
	}
}//end class OSCShortcutPackMessage
//EOF
