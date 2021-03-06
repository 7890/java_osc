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
import com.illposed.osc.OSCTypedBlob;
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
import javax.sound.midi.ShortMessage;

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

		if (isBundle(bytes)) {
			packet = convertBundle(unpacker);
		} else {
			packet = convertMessage(unpacker);
		}
		return packet;
	}

	public OSCPacket convert(byte[] bytes, int bytesLength, String remoteHost, int remotePort) {
		this.remoteHost=remoteHost;
		this.remotePort=remotePort;
		return convert(bytes, bytesLength);
	}

	private boolean isBundle(final byte[] bytes) {
		// The shortest valid packet may be no shorter then 4 bytes,
		// thus we may assume to always have a byte at index 2.
		//!.#bundle
		return bytes[2] == BUNDLE_IDENTIFIER;
	}

	///
	private OSCBundle convertBundle(final MessageUnpacker up) {

		long t;
		final Date timestamp;
		final OSCBundle bundle;
		final OSCByteArrayToJavaConverter conv_regular;
		final OSCPackByteArrayToJavaConverter conv_packed;

		try
		{
			// skip the "#bundle " stuff
			up.unpackString();

			t=up.unpackLong(); //ntp timestamp
			timestamp = NTPTime.readTimeTag(t);

			bundle = new OSCBundle(timestamp);

			bundle.setRemoteHost(remoteHost);
			bundle.setRemotePort(remotePort);

			///
			conv_regular = new OSCByteArrayToJavaConverter();
			conv_regular.setCharset(charset);

			conv_packed = new OSCPackByteArrayToJavaConverter();
			conv_packed.setCharset(charset);
		}
		catch(Exception e){throw new IllegalArgumentException("could not parse OSCPack bundle");}

		while (1==1)
		{
			// recursively read through the stream and convert packets you find
			final int packetLength;
			try{
				packetLength = up.unpackBinaryHeader(); //byte count of (that) one message item inside blob

				if (packetLength == 0) {
					throw new IllegalArgumentException("Packet length may not be 0");
				}

				final byte[] packetBytes = up.readPayload(packetLength);

				//decide which converter to use. messages inside blobs can be packed.
				ByteArrayToJavaConverter conv;
				if(packetBytes.length>0 && packetBytes[0]=='!')
				{
					conv=conv_packed;
				}
				else
				{
					conv=conv_regular;
				}

				final OSCPacket packet = conv.convert(packetBytes, packetLength, remoteHost, remotePort);
				bundle.addPacket(packet);
			}catch(Exception e){/*e.printStackTrace();*/break;}
		}
		return bundle;
	}//end convertBundle()

	//
	public List<Object> convertArguments(final MessageUnpacker up, final CharSequence types)
	{
		final List<Object> args=new ArrayList<Object>();

		for (int ti = 0; ti < types.length(); ++ti) {
			if ('[' == types.charAt(ti)) {
				// we're looking at an array -- read it in
				args.add(readArray(up, types, ++ti));
				// then increment i to the end of the array
				while (types.charAt(ti) != ']') {
					ti++;
				}
			} else {
				args.add(readArgument(up, types.charAt(ti)));
			}
		}
		return args;
	}

	//wrapper
	public List<Object> convertArguments(final byte[] bytes, final String types)
	{
		if(bytes.length>0 && bytes[0]!='!')
		{
			//not a OSCPack byte array!
			throw new IllegalArgumentException("OSCPack byte stream doesn't start with '!'");
		}
		//skip "!"
		MessageUnpacker unpacker= MessagePack.newDefaultUnpacker(bytes, 1, bytes.length-1);
		return convertArguments(unpacker,types);
	}

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
			message.setRemoteHost(remoteHost);
			message.setRemotePort(remotePort);
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
			message.addArguments(convertArguments(up,types));
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
					int len1=up.unpackBinaryHeader();
					final byte[] b1=new byte[len1];
					up.readPayload(b1);
					return b1;
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
				case 'm' :
					int len2=up.unpackBinaryHeader();
					final byte[] b2=new byte[len2];
					up.readPayload(b2);
					//first byte of OSC type 'm' message is midi port.
					//skip it for now
					if(len2==2) {      return new ShortMessage( (int)(b2[1] & 0xff), 0,                   0 ); }
					else if(len2==3) { return new ShortMessage( (int)(b2[1] & 0xff), (int)(b2[2] & 0xff), 0 ); }
					else if(len2==4) { return new ShortMessage( (int)(b2[1] & 0xff), (int)(b2[2] & 0xff), (int)(b2[3] & 0xff));}
				case 't' :
					return NTPTime.readTimeTag(up.unpackLong());
				case 'B' :
					char blob_type=(char)up.unpackByte();
					int item_count=up.unpackInt();
					int len3=up.unpackBinaryHeader();
					final byte[] b3=new byte[len3];
					up.readPayload(b3);
			                return new OSCTypedBlob(blob_type,item_count,b3);
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
