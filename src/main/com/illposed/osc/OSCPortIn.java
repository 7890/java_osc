/*
 * Copyright (C) 2004-2014, C. Ramakrishnan / Illposed Software.
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import com.illposed.osc.utility.ByteArrayToJavaConverter;
import com.illposed.osc.utility.OSCByteArrayToJavaConverter;
import com.illposed.osc.utility.OSCPackByteArrayToJavaConverter;
import com.illposed.osc.utility.OSCShortcutPacketDispatcher;
import com.illposed.osc.utility.OSCPatternAddressSelector;
import com.illposed.osc.utility.Debug;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.net.*;

/**
 * OSCPortIn is the class that listens for OSC messages.
 *
 * An example:<br>
 * (loosely based on {com.illposed.osc.OSCPortTest#testReceiving()})
 * <blockquote><pre>{@code
 * receiver = new OSCPortIn(OSCPort.DEFAULT_SC_OSC_PORT());
 * OSCListener listener = new OSCListener() {
 * 	public void acceptMessage(java.util.Date time, OSCMessage message) {
 * 		System.out.println("Message received!");
 * 	}
 * };
 * receiver.addListener("/message/receiving", listener);
 * receiver.startListening();
 * }</pre></blockquote>
 *
 * Then, using a program such as SuperCollider or sendOSC, send a message
 * to this computer, port {@link #DEFAULT_SC_OSC_PORT},
 * with the address "/message/receiving".
 *
 * @author Chandrasekhar Ramakrishnan
 * @author Thomas Brand
 */
public class OSCPortIn extends OSCPort implements Runnable {

	/**
	 * Buffers were 1500 bytes in size, but were
	 * increased to 1536, as this is a common MTU.
	 */
//	private static final int BUFFER_SIZE = 1536;
	public static final int BUFFER_SIZE = 65507;

/*
(2^16)-1 = 65535
65535 - 8 byte UDP header - 20 bytes IP header = 65507 (IPv4)

http://stackoverflow.com/questions/9203403/java-datagrampacket-udp-maximum-send-recv-buffer-size

DatagramPacket is just a wrapper on a UDP based socket, so the usual UDP rules apply.

64 kilobytes is the theoretical maximum size of a complete IP datagram, but only 
576 bytes are guaranteed to be routed. On any given network path, the link with 
the smallest Maximum Transmit Unit will determine the actual limit. 
(1500 bytes, less headers is the common maximum, but it is impossible to predict 
how many headers there will be so its safest to limit messages to around 1400 bytes.)

If you go over the MTU limit, IPv4 will automatically break the datagram up into 
fragments and reassemble them at the end, but only up to 64 kilobytes and only if 
all fragments make it through. If any fragment is lost, or if any device decides it 
doesn't like fragments, then the entire packet is lost.
*/

	/** state for listening */
	private boolean listening;
	private final OSCByteArrayToJavaConverter converter;
	private final OSCPackByteArrayToJavaConverter pack_converter;
	private final OSCShortcutPacketDispatcher dispatcher;

	/**
	 * Create an OSCPort that listens using a specified socket.
	 * @param socket DatagramSocket to listen on.
	 */
	public OSCPortIn(DatagramSocket socket) {
		super(socket, socket.getLocalPort());

		this.converter = new OSCByteArrayToJavaConverter();
		this.pack_converter = new OSCPackByteArrayToJavaConverter();
		this.dispatcher = new OSCShortcutPacketDispatcher();
	}

	/**
	 * Create an OSCPort that listens on the specified port.
	 * Strings will be decoded using the systems default character set.
	 * @param port UDP port to listen on.
	 * @throws SocketException if the port number is invalid,
	 *   or there is already a socket listening on it
	 */
	public OSCPortIn(int port) throws SocketException {
		this(new DatagramSocket(port));
	}

	/**
	 * Create an OSCPort that listens on the specified port,
	 * and decodes strings with a specific character set.
	 * @param port UDP port to listen on.
	 * @param charset how to decode strings read from incoming packages.
	 *   This includes message addresses and string parameters.
	 * @throws SocketException if the port number is invalid,
	 *   or there is already a socket listening on it
	 */
	public OSCPortIn(int port, Charset charset) throws SocketException {
		this(port);

		this.converter.setCharset(charset);
		this.pack_converter.setCharset(charset);
	}

	/**
	 * Run the loop that listens for OSC on a socket until
	 * {@link #isListening()} becomes false.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		final byte[] buffer = new byte[BUFFER_SIZE];
		final DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
		final DatagramSocket socket = getSocket();
		while (listening) {
			try {
				try {
					socket.receive(packet);
				} catch (SocketException ex) {
					if (listening) {
						throw ex;
					} else {
						// if we closed the socket while receiving data,
						// the exception is expected/normal, so we hide it
						continue;
					}
				}

				if(debug)
				{
					System.err.println("OSCPortIn: DatagramPacket received ("+packet.getLength()+" bytes):");
					Debug.hexdump(buffer,packet.getLength());
				}
				//decide which bytearray to java converter to use
				final ByteArrayToJavaConverter conv;
				if(buffer[0]=='!') //OSCPack
				{
					conv=pack_converter;
				}
				else //it will be checked later on if message starts with '/'
				{
					conv=converter;
				}

				//create common datastructure, to be dispatched to listeners
				final OSCPacket oscPacket = conv.convert(buffer,
						packet.getLength(),packet.getAddress().getHostAddress(),packet.getPort());

				//update stats, considering success here
				//dispatcher & friends can still fail
				//message consumers already have updated stats (including this message)
				successfully_processed_count++;
				successfully_processed_bytes+=packet.getLength();

				dispatcher.dispatchPacket(oscPacket);
			} catch (Exception ex) {
				ex.printStackTrace(); // XXX This may not be a good idea, as this could easily lead to a never ending series of exceptions thrown (due to the non-exited while loop), and because the user of the lib may want to handle this case himself
			}
		}
	}

	/**
	 * Start listening for incoming OSCPackets
	 */
	public void startListening() {
		listening = true;
		final Thread thread = new Thread(this);
		// The JVM exits when the only threads running are all daemon threads.
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Stop listening for incoming OSCPackets
	 */
	public void stopListening() {
		listening = false;
	}

	/**
	 * Am I listening for packets?
	 * @return true if this port is in listening mode
	 */
	public boolean isListening() {
		return listening;
	}

	/**
	 * Registers a listener that will be notified of incoming messages,
	 * if their address matches the given pattern.
	 *
	 * @param addressSelector either a fixed address like "/sc/mixer/volume",
	 *   or a selector pattern (a mix between wildcards and regex)
	 *   like "/??/mixer/*", see {@link OSCPatternAddressSelector} for details
	 * @param listener will be notified of incoming packets, if they match
	 */
	public void addListener(String addressSelector, OSCListener listener) {
		this.addListener(new OSCPatternAddressSelector(addressSelector), listener);
	}

	/**
	 * Registers a listener that will be notified of incoming messages,
	 * if their address matches the given selector.
	 * @param addressSelector a custom address selector
	 * @param listener will be notified of incoming packets, if they match
	 */
	public void addListener(AddressSelector addressSelector, OSCListener listener) {
		dispatcher.addListener(addressSelector, listener);
	}
}//end class OSCPortIn
//EOF
