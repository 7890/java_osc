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
import java.nio.charset.Charset;

/**
 * AbstractOSCPacket is the abstract superclass for the various
 * kinds of OSC Messages.
 *
 * The actual packets are:
 * <ul>
 * <li>{@link OSCMessage}: simple OSC messages
 * <li>{@link OSCPackMessage}: packed (using MsgPack library) OSC messages
 * <li>{@link OSCBundle}: OSC messages with timestamps and/or made up of multiple messages
 * </ul>
 */
public abstract class AbstractOSCPacket implements OSCPacket {

	/** Used to encode message addresses and string parameters. */
	protected Charset charset;
	protected byte[] byteArray;

	protected String remoteHost="";
	protected int remotePort=0;
	protected String typetags="";

	public AbstractOSCPacket() {
		this.charset = Charset.defaultCharset();
		this.byteArray = null;
	}

	//implement interface
	public Charset getCharset() {
		return charset;
	}

	//implement interface
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	//implement interface
	public byte[] getByteArray() {
		if (byteArray == null) {
			byteArray = computeByteArray();
		}
/*
		try
		{
			java.io.FileOutputStream stream = new java.io.FileOutputStream("/tmp/msg.bin");
			stream.write(byteArray);
			stream.close();
		}
		catch(Exception e){}
*/
		return byteArray;
	}

	protected void contentChanged() {
		byteArray = null;
	}

	public String getRemoteHost()
	{
		return remoteHost;
	}
	public int getRemotePort()
	{
		return remotePort;
	}
	public String getTypetagString()
	{
		return typetags;
	}
	public void setRemoteHost(String s)
	{
		remoteHost=s;
	}
	public void setRemotePort(int i)
	{
		remotePort=i;
	}
	public void setTypetagString(String typetags)
	{
		this.typetags=typetags;
	}

	/**
	 * Generate a representation of this packet conforming to the
	 * the OSC byte stream specification. Used Internally.
	 * Default initialized with {@link OSCJavaToByteArrayConverter} (this method can be overriden if needed)
	 */
	protected byte[] computeByteArray() {
		//default initialize with OSCJavaToByteArrayConverter (this method can be overriden if needed)
		final JavaToByteArrayConverter stream = new OSCJavaToByteArrayConverter();
		stream.setCharset(charset);
		return computeByteArray(stream);
	}

	/**
	 * Produces a byte array representation of this packet.
	 * The exending class must define this method.
	 * @param stream where to write the arguments to
	 * @return the OSC or OSCPack specification conform byte array representation of this packet
	 */
	protected abstract byte[] computeByteArray(JavaToByteArrayConverter stream);
}//end class AbstractOSCPacket
//EOF
