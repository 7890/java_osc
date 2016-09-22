/*
 * Copyright (C) 2004-2014, C. Ramakrishnan / Illposed Software.
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCImpulse;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class to convert a byte array,
 * conforming to the OSCPack byte stream format,
 * into Java objects.
 *
 * @author Chandrasekhar Ramakrishnan
 * @author Thomas Brand
 */
public class OSCPackByteArrayToJavaConverter extends AbstractByteArrayToJavaConverter {

	private MessageUnpacker unpacker;

	public OSCPacket convert(byte[] bytes, int bytesLength) {

		final OSCPacket packet;

		if(bytes[0]!='!')
		{
			//not a OSCPack byte array!
			return null; ///throw runtime error?
		}

		//skip "!"
		unpacker = MessagePack.newDefaultUnpacker(bytes, 1, bytesLength-1);

		if (isBundle(bytes))
		{
			packet = convertBundle(unpacker);
		}
		else 
		{
			packet = convertMessage(unpacker);
		}

		return packet;
	}

	public OSCPacket convert(byte[] bytes, int bytesLength, String remoteHost, int remotePort) {
		this.remoteHost=remoteHost;
		this.remotePort=remotePort;
		return convert(bytes, bytesLength);
	}

	/**
	 * Checks whether my byte array is a bundle.
	 * From the OSC 1.0 specifications:
	 * <quote>
	 * The contents of an OSC packet must be either an OSC Message
	 * or an OSC Bundle. The first byte of the packet's contents unambiguously
	 * distinguishes between these two alternatives.
	 * </quote>
	 * @return true if it the byte array is a bundle, false o.w.
	 */

	private boolean isBundle(final byte[] bytes) {
		// The shortest valid packet may be no shorter then 4 bytes,
		// thus we may assume to always have a byte at index 1. (first is ! to indicate OSCPack)
		return bytes[1] == BUNDLE_IDENTIFIER;
	}

	/**
	 * Converts the byte array to a bundle.
	 * Assumes that the byte array is a bundle.
	 * @return a bundle containing the data specified in the byte stream
	 */
///CURRENTLY UNTESTED AND WRONG
	private OSCBundle convertBundle(final MessageUnpacker up) {

		// skip the "#bundle " stuff
		///rawInput.addToStreamPosition(BUNDLE_START.length() + 1);
		try
		{
			///#bundle stuff LENGTH
			up.unpackByte();

			long t=up.unpackLong();

			///final Date timestamp = readTimeTag(rawInput);
			final Date timestamp = readTimeTag(t);
			final OSCBundle bundle = new OSCBundle(timestamp);

			final OSCPackByteArrayToJavaConverter myConverter
					= new OSCPackByteArrayToJavaConverter();
			myConverter.setCharset(charset);


			///while (rawInput.getStreamPosition() < rawInput.getBytesLength())
			while(1==1)
			{
				// recursively read through the stream and convert packets you find
				final int packetLength;
				try{
					packetLength = up.unpackInt();
				}catch(Exception e)
				{
					break;
				}

				if (packetLength == 0) {
					throw new IllegalArgumentException("Packet length may not be 0");
				}
				///System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), packetBytes, 0, packetLength);

				final byte[] packetBytes = up.readPayload(packetLength);

				final OSCPacket packet = myConverter.convert(packetBytes, packetLength);
				bundle.addPacket(packet);
			}
			return bundle;
		}
		catch(Exception e)
		{e.printStackTrace();}
		return null; ///ev. throw runtime exception
	}// end convertBundle()

	/**
	 * Converts the byte array to a simple message.
	 * Assumes that the byte array is a message.
	 * @return a message containing the data specified in the byte stream
	 */
	private OSCMessage convertMessage(final MessageUnpacker up) {

		final OSCMessage message = new OSCMessage();

		try
		{
			message.setAddress(up.unpackString());

			message.setRemoteHost(this.remoteHost);
			message.setRemotePort(this.remotePort);

			String typestmp=up.unpackString();

			final CharSequence types;
			if(typestmp!=null && !typestmp.equals(""))
			{
				types=typestmp;
			}
			else
			{
				types=NO_ARGUMENT_TYPES;
			}

			message.setTypetagString(""+types);

			for (int ti = 0; ti < types.length(); ++ti) {
				if ('[' == types.charAt(ti)) {
					// we're looking at an array -- read it in

					message.addArgument(readArray(up, types, ++ti));

					// then increment i to the end of the array
					while (types.charAt(ti) != ']') {
						ti++;
					}
				} else {

					message.addArgument(readArgument(up, types.charAt(ti)));
				}
			}
			return message;

		}
		catch (Exception e)
		{e.printStackTrace();} ///ev. throw runtime exception
		return null;
	}//end convertMessage()

	/**
	 * Reads an object of the type specified by the type char.
	 * @param type type of the argument to read
	 * @return a Java representation of the argument
	 */
	private Object readArgument(final MessageUnpacker up, final char type) {

		try
		{
			switch (type) {
				///case 'u' :
				///	return readUnsignedInteger(rawInput);
				case 'i' :
					return up.unpackInt();
				case 'h' :
					return up.unpackLong();
				case 'f' :
					return up.unpackFloat();
				case 'd' :
					return up.unpackDouble();
				case 's' :
					return up.unpackString();
				case 'b' :
					///return readBlob(rawInput);
					int len=up.unpackBinaryHeader();
					final byte[] b=new byte[len];
					up.readPayload(b);
					return b;
				case 'c' :
					return (char)up.unpackByte();
				case 'N' :
					return null;
				case 'T' :
					return Boolean.TRUE;
				case 'F' :
					return Boolean.FALSE;
				case 'I' :
					return OSCImpulse.INSTANCE;
				case 't' :
					return readTimeTag(up.unpackLong());
				default:
					// XXX Maybe we should let the user choose what to do in this
					//   case (we encountered an unknown argument type in an
					//   incomming message):
					//   just ignore (return null), or throw an exception?
//					throw new UnsupportedOperationException(
//							"Invalid or not yet supported OSC type: '" + type + "'");
					return null;
			}//end switch cae
		} catch(Exception e)
		{e.printStackTrace();} ///ev. throw runtime exception
		return null;
	}//end readArgument()

	/**
	 * Reads the time tag and convert it to a Java Date object.
	 * A timestamp is a 64 bit number representing the time in NTP format.
	 * The first 32 bits are seconds since 1900, the second 32 bits are
	 * fractions of a second.
	 * @return a {@link Date}
	 */
	private Date readTimeTag(long t) {

		try
		{
			ByteBuffer buffer = ByteBuffer.allocate(8);//Long.BYTES);
			buffer.order(ByteOrder.BIG_ENDIAN);
			buffer.putLong(t);
			byte[] bytes=buffer.array();
			int index=0;

			final byte[] secondBytes = new byte[8];
			final byte[] fractionBytes = new byte[8];
			for (int bi = 0; bi < 4; bi++) {
				// clear the higher order 4 bytes
				secondBytes[bi] = 0;
				fractionBytes[bi] = 0;
			}
			// while reading in the seconds & fraction, check if
			// this timetag has immediate semantics
			boolean isImmediate = true;
			for (int bi = 4; bi < 8; bi++) {
				secondBytes[bi] = bytes[index]; index++;

				if (secondBytes[bi] > 0) {
					isImmediate = false;
				}
			}
			for (int bi = 4; bi < 8; bi++) {
				fractionBytes[bi] = bytes[index]; index++;

				if (bi < 7) {
					if (fractionBytes[bi] > 0) {
						isImmediate = false;
					}
				} else {
					if (fractionBytes[bi] > 1) {
						isImmediate = false;
					}
				}
			}

			if (isImmediate) {

				return OSCBundle.TIMESTAMP_IMMEDIATE;
			}

			final long secsSince1900 = new BigInteger(secondBytes).longValue();
			long secsSince1970 = secsSince1900 - OSCBundle.SECONDS_FROM_1900_TO_1970;

			// no point maintaining times in the distant past
			if (secsSince1970 < 0) {
				secsSince1970 = 0;
			}
			long fraction = new BigInteger(fractionBytes).longValue();

			// this line was cribbed from jakarta commons-net's NTP TimeStamp code
			fraction = (fraction * 1000) / 0x100000000L;

			// I do not know where, but I'm losing 1ms somewhere...
			fraction = (fraction > 0) ? fraction + 1 : 0;
			final long millisecs = (secsSince1970 * 1000) + fraction;
			return new Date(millisecs);

		}
		catch(Exception e)
		{e.printStackTrace();} ///ev. throw runtime exception
		return null; //new Date();///
	}//end readTimeTag()

	/**
	 * Reads an array from the byte stream.
	 * @param types
	 * @param pos at which position to start reading
	 * @return the array that was read
	 */
	private List<Object> readArray(final MessageUnpacker up, final CharSequence types, int pos) {

		int arrayLen = 0;
		while (types.charAt(pos + arrayLen) != ']') {
			arrayLen++;
		}
		final List<Object> array = new ArrayList<Object>(arrayLen);
		for (int ai = 0; ai < arrayLen; ai++) {
			array.add(readArgument(up, types.charAt(pos + ai)));
		}
		return array;
	}
} //end class OSCPackByteArrayToJavaConverter
//EOF
