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

import java.math.BigInteger;
import java.util.Date;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NTPTime
{
	protected static final long MSB_0_BASE_TIME = 2085978496000L;
	/**
	 * baseline NTP time if bit-0=1 is 1-Jan-1900 @ 01:00:00 UTC
	 */
	protected static final long MSB_1_BASE_TIME = -2208988800000L;

	//long contains ntp 64bit time
	public static Date readTimeTag(long ntpTime) {
/*
		ByteBuffer buffer = ByteBuffer.allocate(8);//Long.BYTES);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.putLong(ntpTime);
		Debug.hexdump(buffer.array());
*/
		System.err.println("readTimeTag(): "+ntpTime+" "+getSeconds(ntpTime)+" "+getFraction(ntpTime)+" "+toString(ntpTime));

		if(ntpTime==1) //too simple?
		{
			System.err.println("readTimeTag(): IMMEDIATE");
			return OSCBundle.TIMESTAMP_IMMEDIATE;
		}
		return new Date(getTime(ntpTime));
	}

	/**
	 * Converts a Java time-stamp to a 64-bit NTP time representation.
	 * This code was copied in from the "Apache Jakarta Commons - Net" library,
	 * which is licensed under the
	 * <a href="http://www.apache.org/licenses/LICENSE-2.0.html">ASF 2.0 license</a>.
	 * The original source file can be found
	 * <a href="http://svn.apache.org/viewvc/commons/proper/net/trunk/src/main/java/org/apache/commons/net/ntp/TimeStamp.java?view=co">here</a>.
	 * @param javaTime Java time-stamp, as returned by {@link Date#getTime()}
	 * @return NTP time-stamp representation of the Java time value.
	 */
	public static long javaToNtpTimeStamp(long javaTime) {

		final boolean useBase1 = javaTime < MSB_0_BASE_TIME; // time < Feb-2036
		final long baseTime;
		if (useBase1) {
			baseTime = javaTime - MSB_1_BASE_TIME; // dates <= Feb-2036
		} else {
			// if base0 needed for dates >= Feb-2036
			baseTime = javaTime - MSB_0_BASE_TIME;
		}

		long seconds = baseTime / 1000;
		final long fraction = ((baseTime % 1000) * 0x100000000L) / 1000;

		if (useBase1) {
			seconds |= 0x80000000L; // set high-order bit if MSB_1_BASE_TIME 1900 used
		}

		final long ntpTime = seconds << 32 | fraction;

		return ntpTime;
	}

	///========from org.apache.commons.net.ntp.TimeStamp.java
	/***
	 * Convert 64-bit NTP timestamp to Java standard time.
	 *
	 * Note that java time (milliseconds) by definition has less precision
	 * then NTP time (picoseconds) so converting NTP timestamp to java time and back
	 * to NTP timestamp loses precision. For example, Tue, Dec 17 2002 09:07:24.810 EST
	 * is represented by a single Java-based time value of f22cd1fc8a, but its
	 * NTP equivalent are all values ranging from c1a9ae1c.cf5c28f5 to c1a9ae1c.cf9db22c.
	 *
	 * @param ntpTime
	 * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT
	 * represented by this NTP timestamp value.
	 */
	public static long getTime(long ntpTime)
	{
		long seconds = (ntpTime >>> 32) & 0xffffffffL;	 // high-order 32-bits
		long fraction = ntpTime & 0xffffffffL;			 // low-order 32-bits

		// Use round-off on fractional part to preserve going to lower precision
		fraction = Math.round(1000D * fraction / 0x100000000L);

		/*
		 * If the most significant bit (MSB) on the seconds field is set we use
		 * a different time base. The following text is a quote from RFC-2030 (SNTP v4):
		 *
		 *  If bit 0 is set, the UTC time is in the range 1968-2036 and UTC time
		 *  is reckoned from 0h 0m 0s UTC on 1 January 1900. If bit 0 is not set,
		 *  the time is in the range 2036-2104 and UTC time is reckoned from
		 *  6h 28m 16s UTC on 7 February 2036.
		 */
		long msb = seconds & 0x80000000L;
		if (msb == 0) {
			// use base: 7-Feb-2036 @ 06:28:16 UTC
			return MSB_0_BASE_TIME + (seconds * 1000) + fraction;
		} else {
			// use base: 1-Jan-1900 @ 01:00:00 UTC
			return MSB_1_BASE_TIME + (seconds * 1000) + fraction;
		}
	}

	/***
	 * Returns high-order 32-bits representing the seconds of this NTP timestamp.
	 *
	 * @return seconds represented by this NTP timestamp.
	 */
	public static long getSeconds(long ntpTime)
	{
		return (ntpTime >>> 32) & 0xffffffffL;
	}

	/***
	 * Returns low-order 32-bits representing the fractional seconds.
	 *
	 * @return fractional seconds represented by this NTP timestamp.
	 */
	public static long getFraction(long ntpTime)
	{
		return ntpTime & 0xffffffffL;
	}

	/***
	 * Convert NTP timestamp hexstring (e.g. "c1a089bd.fc904f6d") to the NTP
	 * 64-bit unsigned fixed-point number.
	 *
	 * @return NTP 64-bit timestamp value.
	 * @throws NumberFormatException - if the string does not contain a parsable timestamp.
	 */
	public static long decodeNtpHexString(String s)
			throws NumberFormatException
	{
		if (s == null) {
			throw new NumberFormatException("null");
		}
		int ind = s.indexOf('.');
		if (ind == -1) {
			if (s.length() == 0) {
				return 0;
			}
			return Long.parseLong(s, 16) << 32; // no decimal
		}

		return Long.parseLong(s.substring(0, ind), 16) << 32 |
				Long.parseLong(s.substring(ind + 1), 16);
	}

	/***
	 * Converts 64-bit NTP timestamp value to a <code>String</code>.
	 * The NTP timestamp value is represented as hex string with
	 * seconds separated by fractional seconds by a decimal point;
	 * e.g. c1a089bd.fc904f6d <=> Tue, Dec 10 2002 10:41:49.986
	 *
	 * @return NTP timestamp 64-bit long value as hex string with seconds
	 * separated by fractional seconds.
	 */
	public static String toString(long ntpTime)
	{
		StringBuilder buf = new StringBuilder();
		// high-order second bits (32..63) as hexstring
		appendHexString(buf, (ntpTime >>> 32) & 0xffffffffL);

		// low-order fractional seconds bits (0..31) as hexstring
		buf.append('.');
		appendHexString(buf, ntpTime & 0xffffffffL);

		return buf.toString();
	}

	/***
	 * Left-pad 8-character hex string with 0's
	 *
	 * @param buf - StringBuilder which is appended with leading 0's.
	 * @param l - a long.
	 */
	private static void appendHexString(StringBuilder buf, long l)
	{
		String s = Long.toHexString(l);
		for (int i = s.length(); i < 8; i++) {
			buf.append('0');
		}
		buf.append(s);
	}
} //end class NTPTime
