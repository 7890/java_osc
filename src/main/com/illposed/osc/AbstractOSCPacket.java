/*
 * Copyright (C) 2003-2014, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import com.illposed.osc.utility.OSCJavaToByteArrayConverter;
import java.nio.charset.Charset;

/**
 * OSCPacket is the abstract superclass for the various
 * kinds of OSC Messages.
 *
 * The actual packets are:
 * <ul>
 * <li>{@link OSCMessage}: simple OSC messages
 * <li>{@link OSCBundle}: OSC messages with timestamps
 *   and/or made up of multiple messages
 * </ul>
 */
abstract class AbstractOSCPacket implements OSCPacket {

	/** Used to encode message addresses and string parameters. */
	private Charset charset;
	private byte[] byteArray;

	private String remoteHost="";
	private int remotePort=0;
	private String typetags="";

	public AbstractOSCPacket() {
		this.charset = Charset.defaultCharset();
		this.byteArray = null;
	}

	@Override
	public Charset getCharset() {
		return charset;
	}

	@Override
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	/**
	 * Generate a representation of this packet conforming to the
	 * the OSC byte stream specification. Used Internally.
	 */
	private byte[] computeByteArray() {
		final OSCJavaToByteArrayConverter stream = new OSCJavaToByteArrayConverter();
		stream.setCharset(charset);
		return computeByteArray(stream);
	}

	/**
	 * Produces a byte array representation of this packet.
	 * @param stream where to write the arguments to
	 * @return the OSC specification conform byte array representation
	 *   of this packet
	 */
	protected abstract byte[] computeByteArray(OSCJavaToByteArrayConverter stream);

	@Override
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
}
