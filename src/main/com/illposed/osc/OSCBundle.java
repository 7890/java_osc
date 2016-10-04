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
import com.illposed.osc.utility.NTPTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * A bundle represents a collection of OSC packets
 * (either messages or other bundles)
 * and has a time-tag which can be used by a scheduler to execute
 * a bundle in the future,
 * instead of immediately.
 * {@link OSCMessage}s are executed immediately.
 *
 * Bundles should be used if you want to send multiple messages to be executed
 * atomically together, or you want to schedule one or more messages to be
 * executed in the future.
 *
 * @author Chandrasekhar Ramakrishnan
 * @author Thomas Brand
 */
public class OSCBundle extends AbstractOSCPacket {

	/**
	 * 2208988800 seconds -- includes 17 leap years
	 */
	public static final long SECONDS_FROM_1900_TO_1970 = 2208988800L;

	/**
	 * The Java representation of an OSC timestamp with the semantics of
	 * "immediately".
	 */
	//the precision is nearest millisecond
	public static final Date TIMESTAMP_IMMEDIATE = new Date(0); ///

	//convention on how to encode IMMEDIATE to indicate non-delayed processing
	//#define LO_TT_IMMEDIATE ((lo_timetag){0U,1U})   uint32_t sec; uint32_t frac;
	//from the osc spec:
	//The time tag value consisting of 63 zero bits followed by a one in the least 
	//signifigant bit is a special case meaning "immediately."
	public static final long TT_IMMEDIATE = 1;

	protected Date timestamp;
	protected List<OSCPacket> packets;

	/**
	 * Create a new empty OSCBundle with a timestamp of immediately.
	 * You can add packets to the bundle with addPacket()
	 */
	public OSCBundle() {
		this(TIMESTAMP_IMMEDIATE);
	}

	/**
	 * Create an OSCBundle with the specified timestamp.
	 * @param timestamp the time to execute the bundle
	 */
	public OSCBundle(Date timestamp) {
		this(null, timestamp);
	}

	/**
	 * Creates an OSCBundle made up of the given packets
	 * with a timestamp of now.
	 * @param packets array of OSCPackets to initialize this object with
	 */
	public OSCBundle(Collection<OSCPacket> packets) {
		this(packets, TIMESTAMP_IMMEDIATE);
	}

	/**
	 * Create an OSCBundle, specifying the packets and timestamp.
	 * @param packets the packets that make up the bundle
	 * @param timestamp the time to execute the bundle
	 */
	public OSCBundle(Collection<OSCPacket> packets, Date timestamp) {

		if (null == packets) {
			this.packets = new LinkedList<OSCPacket>();
		} else {
			this.packets = new ArrayList<OSCPacket>(packets);
		}
		this.timestamp = clone(timestamp);
	}

	private static Date clone(final Date toBeCloned) {
		return (toBeCloned == null) ? toBeCloned : (Date) toBeCloned.clone();
	}

	/**
	 * Return the time the bundle will execute.
	 * @return a Date
	 */
	public Date getTimestamp() {
		return clone(timestamp);
	}

	/**
	 * Set the time the bundle will execute.
	 * @param timestamp Date
	 */
	public OSCBundle setTimestamp(Date timestamp) {
		this.timestamp = clone(timestamp);
		return this;
	}

	/**
	 * Add a packet to the list of packets in this bundle.
	 * @param packet OSCMessage or OSCBundle
	 */
	public OSCBundle addPacket(OSCPacket packet) {
		packets.add(packet);
		contentChanged();
		return this;
	}

	//wrapper
	public OSCBundle add(OSCPacket packet) {
		packets.add(packet);
		contentChanged();
		return this;
	}

	/**
	 * Get the packets contained in this bundle.
	 * @return the packets contained in this bundle.
	 */
	public List<OSCPacket> getPackets() {
		return Collections.unmodifiableList(packets);
	}

	/**
	 * Convert the time-tag (a Java Date) into the OSC byte stream.
	 * Used Internally.
	 * @param stream where to write the time-tag to
	 */
	protected void computeTimeTagByteArray(JavaToByteArrayConverter stream) {
		if ((null == timestamp) || (timestamp.equals(TIMESTAMP_IMMEDIATE))) {

			///stream.write(NTPTime.javaToNtpTimeStamp(0));
			stream.write(TT_IMMEDIATE);
		}
		else {
			stream.write(NTPTime.javaToNtpTimeStamp(timestamp.getTime()));
		}
	}

	//implement abstract method from abstract superclass
	protected byte[] computeByteArray(JavaToByteArrayConverter stream) {
		stream.write("#bundle");
		computeTimeTagByteArray(stream);
		byte[] packetBytes;
		for (final OSCPacket pkg : packets) {
			packetBytes = pkg.getByteArray();
			stream.write(packetBytes);
		}
		return stream.toByteArray();
	}

	//implement abstract method from abstract superclass
	//to be overridden by subclasses of OSCBundle
	public JavaToByteArrayConverter getConverter()
	{
		final JavaToByteArrayConverter stream=new OSCJavaToByteArrayConverter();
		return stream;
	}
}//end class OSCBundle
//EOF
