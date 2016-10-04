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
import java.util.Date;

public class OSCPackBundle extends OSCBundle {

	public OSCPackBundle() {
		super(TIMESTAMP_IMMEDIATE);
	}

	public OSCPackBundle(Date timestamp) {
		super(null, timestamp);
	}

	public OSCPackBundle(Collection<OSCPacket> packets) {
		super(packets, TIMESTAMP_IMMEDIATE);
	}

	public OSCPackBundle(Collection<OSCPacket> packets, Date timestamp) {
		super(packets, timestamp);
	}

	@Override
        protected byte[] computeByteArray(JavaToByteArrayConverter stream) {
                stream.write("#b");//undle"); ///only difference to superclass method
                computeTimeTagByteArray(stream);
                byte[] packetBytes;
                for (final OSCPacket pkg : packets) {
                        packetBytes = pkg.getByteArray();
                        stream.write(packetBytes);
                }
                return stream.toByteArray();
        }

	//implement abstract method from abstract superclass
	public JavaToByteArrayConverter getConverter()
	{
		final JavaToByteArrayConverter stream=new OSCJavaToOSCPackByteArrayConverter();
		return stream;
	}
}//end class OSCPackBundle
//EOF
