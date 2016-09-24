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
import com.illposed.osc.OSCPackMessage;

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

		if(bytes.length>0 && bytes[0]!='!')
		{
			//not a OSCPack byte array!
			throw new IllegalArgumentException("OSCPack byte stream doesn't start with '!'");
		}

		//skip "!"
		unpacker = MessagePack.newDefaultUnpacker(bytes, 1, bytesLength-1);

		//there are no packed bundles. however one can put packed messages inside bundles.

		packet = convertMessage(unpacker);

		return packet;
	}

	public OSCPacket convert(byte[] bytes, int bytesLength, String remoteHost, int remotePort) {
		this.remoteHost=remoteHost;
		this.remotePort=remotePort;
		return convert(bytes, bytesLength);
	}

	/**
	 * Converts the byte array to a simple message.
	 * Assumes that the byte array is a message.
	 * @return a message containing the data specified in the byte stream
	 */
	private OSCMessage convertMessage(final MessageUnpacker up) {

		final OSCMessage message = new OSCMessage(); ////ev. return OSCPackMessage

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
