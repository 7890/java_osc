/*
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

import com.illposed.osc.OSCPacket;

import java.nio.charset.Charset;

public interface ByteArrayToJavaConverter {
	//methods that all implementing classes of ByteArrayToJavaConverter need to provide
	public Charset getCharset();
	public void setCharset(Charset charset);
	public OSCPacket convert(byte[] bytes, int bytesLength);
	public OSCPacket convert(byte[] bytes, int bytesLength, String remoteHost, int remotePort);
}
