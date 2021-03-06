/*
 * Copyright (C) 2003-2014, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import java.net.DatagramSocket;

/**
 * OSCPort is an abstract superclass, to send OSC messages,
 * use {@link OSCPortOut}.
 * To listen for OSC messages, use {@link OSCPortIn}.
 *
 * @author Chandrasekhar Ramakrishnan
 */
public class OSCPort {

	private final DatagramSocket socket;
	private int port;

	public static final int DEFAULT_SC_OSC_PORT = 57110;
	public static final int DEFAULT_SC_LANG_OSC_PORT = 57120;

	protected boolean debug=false;

	//number of *datagrams* and bytes successfully sent or received.
	//a datagram can be a bundle, messages therein aren't counted separately.
	//for receive: considered success if datagram could be received and converted 
	//to OSCPacket, prior to dispatching.
	protected long successfully_processed_count=0;
	protected long successfully_processed_bytes=0;

	protected OSCPort(DatagramSocket socket, int port) {
		this.socket = socket;
		this.port = port;
		this.debug=false;
	}

	/**
	 * The port that the SuperCollider <b>synth</b> engine
	 * usually listens to.
	 * @return default SuperCollider <b>synth</b> UDP port
	 * @see #DEFAULT_SC_OSC_PORT
	 */
	public static int defaultSCOSCPort() {
		return DEFAULT_SC_OSC_PORT;
	}

	/**
	 * The port that the SuperCollider <b>language</b> engine
	 * usually listens to.
	 * @return default SuperCollider <b>language</b> UDP port
	 * @see #DEFAULT_SC_LANG_OSC_PORT
	 */
	public static int defaultSCLangOSCPort() {
		return DEFAULT_SC_LANG_OSC_PORT;
	}

	/**
	 * Returns the socket associated with this port.
	 * @return this ports socket
	 */
	protected DatagramSocket getSocket() {
		return socket;
	}

	/**
	 * Returns the port number associated with this port.
	 * @return this ports number
	 */
	protected int getPort() {
		return port;
	}

	/**
	 * Close the socket and free-up resources.
	 * It is recommended that clients call this when they are done with the
	 * port.
	 */
	public void close() {
		socket.close();
	}

	//
	public void setPort(int port)
	{
		this.port=port;
	}

	//
	public void setDebug(boolean enabled)
	{
		debug=enabled;
	}

	//
	public boolean getDebug()
	{
		return debug;
	}

	//
	public long getSuccessfullyProcessedCount()
	{
		return successfully_processed_count;
	}

	//
	public long getSuccessfullyProcessedBytes()
	{
		return successfully_processed_bytes;
	}
}//end class OSCPort
//EOF
