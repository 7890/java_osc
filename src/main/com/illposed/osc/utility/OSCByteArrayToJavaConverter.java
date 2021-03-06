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
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.sound.midi.ShortMessage;

/**
 * Utility class to convert a byte array,
 * conforming to the OSC byte stream format,
 * into Java objects.
 *
 * @author Chandrasekhar Ramakrishnan
 * @author Thomas Brand
 */
public class OSCByteArrayToJavaConverter extends AbstractByteArrayToJavaConverter {

	private static class Input {

		private final byte[] bytes;
		private final int bytesLength;
		private int streamPosition;

		Input(final byte[] bytes, final int bytesLength) {

			this.bytes = bytes;
			this.bytesLength = bytesLength;
			this.streamPosition = 0;
		}

		public byte[] getBytes() {
			return bytes;
		}

		public int getBytesLength() {
			return bytesLength;
		}

		public int getAndIncreaseStreamPositionByOne() {
			return streamPosition++;
		}

		public void addToStreamPosition(int toAdd) {
			streamPosition += toAdd;
		}

		public int getStreamPosition() {
			return streamPosition;
		}
	}

	/**
	 * Converts a byte array into an {@link OSCPacket}
	 * (either an {@link OSCMessage} or {@link OSCBundle}).
	 * @param bytes the storage containing the raw OSC packet
	 * @param bytesLength indicates how many bytes the package consists of (<code>&lt;= bytes.length</code>)
	 * @return the successfully parsed OSC packet; in case of a problem,
	 *   a <code>RuntimeException</code> is thrown
	 */
	public OSCPacket convert(byte[] bytes, int bytesLength) {

		final Input rawInput = new Input(bytes, bytesLength);
		final OSCPacket packet;
		if (isBundle(rawInput)) {
			packet = convertBundle(rawInput);
		} else {
			packet = convertMessage(rawInput);
		}

		return packet;
	}

	//
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
	private boolean isBundle(final Input rawInput) {
		// The shortest valid packet may be no shorter then 4 bytes,
		// thus we may assume to always have a byte at index 0.
		return rawInput.getBytes()[0] == BUNDLE_IDENTIFIER;
	}

	/**
	 * Converts the byte array to a bundle.
	 * Assumes that the byte array is a bundle.
	 * @return a bundle containing the data specified in the byte stream
	 */
	private OSCBundle convertBundle(final Input rawInput) {
		// skip the "#bundle " stuff
		rawInput.addToStreamPosition(BUNDLE_START.length() + 1);

		final Date timestamp = readTimeTag(rawInput);
		final OSCBundle bundle = new OSCBundle(timestamp);

		bundle.setRemoteHost(remoteHost);
		bundle.setRemotePort(remotePort);

		final OSCByteArrayToJavaConverter conv_regular
				= new OSCByteArrayToJavaConverter();
		conv_regular.setCharset(charset);

		final OSCPackByteArrayToJavaConverter conv_packed
				= new OSCPackByteArrayToJavaConverter();
		conv_packed.setCharset(charset);

		while (rawInput.getStreamPosition() < rawInput.getBytesLength()) {
			// recursively read through the stream and convert packets you find
//			Debug.hexdump(rawInput.getBytes(),rawInput.getBytesLength());

			//align to 4 bytes boundary. bundle did align (packed) blobs
			int mod=rawInput.getStreamPosition() % 4;
			if(mod!=0)
			{
				rawInput.addToStreamPosition(4-mod);
				//re-evaluate pos < length
				continue;
			}

			final int packetLength = readInteger(rawInput); //byte count of (that) one message item inside blob
			if (packetLength == 0) {
				///throw new IllegalArgumentException("Packet length may not be 0");
				break;
			}
/*
			///packed bundles or messages might be not a multiple of 4
			else if ((packetLength % 4) != 0) {
				throw new IllegalArgumentException("Packet length has to be a multiple of 4, is:"
						+ packetLength);
			}
*/
			final byte[] packetBytes = new byte[packetLength];
			System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), packetBytes, 0, packetLength);
			rawInput.addToStreamPosition(packetLength);

			//decide which converter to use. messages inside blobs can be packed.
			final ByteArrayToJavaConverter conv;
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
		}
		return bundle;
	}//end convertBundle()

	//
	public List<Object> convertArguments(final Input rawInput, final CharSequence types)
	{
		final List<Object> args=new ArrayList<Object>();

		for (int ti = 0; ti < types.length(); ++ti) {
			if ('[' == types.charAt(ti)) {
				// we're looking at an array -- read it in
				args.add(readArray(rawInput, types, ++ti));
				// then increment i to the end of the array
				while (types.charAt(ti) != ']') {
					ti++;
				}
			} else {
				args.add(readArgument(rawInput, types.charAt(ti)));
			}
		}
		return args;
	}

	//wrapper
	public List<Object> convertArguments(final byte[] bytes, final String types)
	{
		final Input rawInput = new Input(bytes, bytes.length);
		return convertArguments(rawInput,types);
	}

	/**
	 * Converts the byte array to a simple message.
	 * Assumes that the byte array is a message.
	 * @return a message containing the data specified in the byte stream
	 */
	private OSCMessage convertMessage(final Input rawInput) {
		final OSCMessage message = new OSCMessage();
		message.setAddress(readString(rawInput));
		message.setRemoteHost(remoteHost);
		message.setRemotePort(remotePort);
		final CharSequence types = readTypes(rawInput);
		message.setTypetagString(""+types);
		message.addArguments(convertArguments(rawInput,types));
		return message;
	}

	/**
	 * Reads a string from the byte stream.
	 * @return the next string in the byte stream
	 */
	private String readString(final Input rawInput) {
		final int strLen = lengthOfCurrentString(rawInput);
		final String res = new String(rawInput.getBytes(), rawInput.getStreamPosition(), strLen, charset);
		rawInput.addToStreamPosition(strLen+1); ////skip zero termination
		moveToFourByteBoundry(rawInput);
		return res;
	}

	/**
	 * Reads a binary blob from the byte stream.
	 * @return the next blob in the byte stream
	 */
	private byte[] readBlob(final Input rawInput) {
		final int blobLen = readInteger(rawInput);
		final byte[] res = new byte[blobLen];
		System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), res, 0, blobLen);
		rawInput.addToStreamPosition(blobLen);
		//blob:
		//"An int32 size count, followed by that many 8-bit bytes of arbitrary binary data, 
		//followed by 0-3 additional zero bytes to make the total number of bits a multiple of 32."
		moveToFourByteBoundry(rawInput);
		return res;
	}

	/**
	 * Reads the types of the arguments from the byte stream.
	 * @return a char array with the types of the arguments,
	 *   or <code>null</code>, in case of no arguments
	 */
	private CharSequence readTypes(final Input rawInput) {
		final String typesStr;

		// The next byte should be a ',', but some legacy code may omit it
		// in case of no arguments, refering to "OSC Messages" in:
		// http://opensoundcontrol.org/spec-1_0
		if (rawInput.getBytes().length <= rawInput.getStreamPosition()) {
			typesStr = NO_ARGUMENT_TYPES;
		} else if (rawInput.getBytes()[rawInput.getStreamPosition()] != ',') {
			// XXX should we not rather fail-fast -> throw exception?
			typesStr = NO_ARGUMENT_TYPES;
		} else {
			rawInput.getAndIncreaseStreamPositionByOne();
			typesStr = readString(rawInput);
		}

		return typesStr;
	}

	/**
	 * Reads an object of the type specified by the type char.
	 * @param type type of the argument to read
	 * @return a Java representation of the argument
	 */
	private Object readArgument(final Input rawInput, final char type) {
		switch (type) {
			case 'u' :
				return readUnsignedInteger(rawInput);
			case 'i' :
				return readInteger(rawInput);
			case 'h' :
				return readLong(rawInput);
			case 'f' :
				return readFloat(rawInput);
			case 'd' :
				return readDouble(rawInput);
			case 's' :
				return readString(rawInput);
			case 'b' :
				return readBlob(rawInput);
			case 'c' :
				return readChar(rawInput);
			case 'N' :
				return null;
			case 'T' :
				return Boolean.TRUE;
			case 'F' :
				return Boolean.FALSE;
			case 'I' :
				return OSCImpulse.INSTANCE;
			case 'm' :
				return readMidi(rawInput);
			case 't' :
				return readTimeTag(rawInput);
			case 'B':
				return readTypedBlob(rawInput);
			default:
				// XXX Maybe we should let the user choose what to do in this
				//   case (we encountered an unknown argument type in an
				//   incomming message):
				//   just ignore (return null), or throw an exception?
//				throw new UnsupportedOperationException(
//						"Invalid or not yet supported OSC type: '" + type + "'");
				return null;
		}
	}

	/**
	 * Reads a char (enclosed in 32 bits) from the byte stream.
	 * @return a {@link Character}
	 */
	private Character readChar(final Input rawInput) {
		rawInput.addToStreamPosition(3);
		return (char) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()];
		/*
		return (char) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()];

		just reading one char isn't enough, it would either read 00 and/or leave padded bytes
		note: type 'c': an ascii character, sent as 32 bits
		-liblo: 'a': 00 00 00 61
		vs      'a'  61 00 00 00
                */
	}

	private BigInteger readBigInteger(final Input rawInput, final int numBytes) {
		final byte[] myBytes = new byte[numBytes];
		System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), myBytes, 0, numBytes);
		rawInput.addToStreamPosition(numBytes);
		return new BigInteger(myBytes);
	}

	/**
	 * Reads a double from the byte stream.
	 * @return a 64bit precision floating point value
	 */
	private Object readDouble(final Input rawInput) {
		final BigInteger doubleBits = readBigInteger(rawInput, 8);
		return Double.longBitsToDouble(doubleBits.longValue());
	}

	/**
	 * Reads a float from the byte stream.
	 * @return a 32bit precision floating point value
	 */
	private Float readFloat(final Input rawInput) {
		final BigInteger floatBits = readBigInteger(rawInput, 4);
		return Float.intBitsToFloat(floatBits.intValue());
	}

	/**
	 * Reads a double precision integer (64 bit integer) from the byte stream.
	 * @return double precision integer (64 bit)
	 */
	private Long readLong(final Input rawInput) {
		final BigInteger longintBytes = readBigInteger(rawInput, 8);
		return longintBytes.longValue();
	}

	/**
	 * Reads an Integer (32 bit integer) from the byte stream.
	 * @return an {@link Integer}
	 */
	private Integer readInteger(final Input rawInput) {
		final BigInteger intBits = readBigInteger(rawInput, 4);
		return intBits.intValue();
	}

	/**
	 * Reads an unsigned integer (32 bit) from the byte stream.
	 * This code is copied from http://darksleep.com/player/JavaAndUnsignedTypes.html,
	 * which is licensed under the Public Domain.
	 * @return single precision, unsigned integer (32 bit) wrapped in a 64 bit integer (long)
	 */
	private Long readUnsignedInteger(final Input rawInput) {

		final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
		final int secondByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
		final int thirdByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
		final int fourthByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
		return ((long) (firstByte << 24
				| secondByte << 16
				| thirdByte << 8
				| fourthByte))
				& 0xFFFFFFFFL;
	}

	/**
	 * Reads the time tag and convert it to a Java Date object.
	 * A timestamp is a 64 bit number representing the time in NTP format.
	 * The first 32 bits are seconds since 1900, the second 32 bits are
	 * fractions of a second.
	 * @return a {@link Date}
	 */
	private Date readTimeTag(final Input rawInput) {
		long time=readLong(rawInput);
		return NTPTime.readTimeTag(time);
	}

	//
	private ShortMessage readMidi(final Input rawInput) {
		//first byte of OSC type 'm' message is midi port.
		//skip it for now
		rawInput.getAndIncreaseStreamPositionByOne();
		byte b1=rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()];
		byte b2=rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()];
		byte b3=rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()];
		int status=(int) (b1 & 0xff);
		int data1=(int) (b2 & 0xff);
		int data2=(int) (b3 & 0xff);

//		System.err.println("status "+status+" data1 "+data1+" data2 "+data2);
		try {
			return new ShortMessage(status,data1,data2);
		}catch(Exception e){throw new IllegalArgumentException("could not create MIDI message.",e);}
	}

	private OSCTypedBlob readTypedBlob(final Input rawInput) {

		//these two make the blob typed
		final char type = readChar(rawInput);
		final int count = readInteger(rawInput);
		final int blobLen = readInteger(rawInput);
		final byte[] res = new byte[blobLen];

		//blob payload data starts now (single typed array)
		System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), res, 0, blobLen);
		rawInput.addToStreamPosition(blobLen);
		moveToFourByteBoundry(rawInput);
		return new OSCTypedBlob(type,count,res);
	}

	/**
	 * Reads an array from the byte stream.
	 * @param types
	 * @param pos at which position to start reading
	 * @return the array that was read
	 */
	private List<Object> readArray(final Input rawInput, final CharSequence types, int pos) {
		int arrayLen = 0;
		while (types.charAt(pos + arrayLen) != ']') {
			arrayLen++;
		}
		final List<Object> array = new ArrayList<Object>(arrayLen);
		for (int ai = 0; ai < arrayLen; ai++) {
			array.add(readArgument(rawInput, types.charAt(pos + ai)));
		}
		return array;
	}

	/**
	 * Get the length of the string currently in the byte stream.
	 */
	private int lengthOfCurrentString(final Input rawInput) {
		int len = 0;
		while (rawInput.getBytes()[rawInput.getStreamPosition() + len] != 0) {
			len++;
		}
		return len;
	}

	/**
	 * Move to the next byte with an index in the byte array
	 * which is dividable by four.
	 */
	private void moveToFourByteBoundry(final Input rawInput) {
		final int mod = rawInput.getStreamPosition() % 4;
		//rawInput.addToStreamPosition(4 - mod);

		//don't move if already on 4-byte boundary
		//for null-terminated strings: add to position, then call moveToFourByteBoundry
		rawInput.addToStreamPosition( (4 - mod) % 4 );
	}
}//end class OSCByteArrayToJavaConverter
//EOF
