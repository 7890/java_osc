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
	public static Date readTimeTag(long t) {
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
			seconds |= 0x80000000L; // set high-order bit if msb1baseTime 1900 used
		}

		final long ntpTime = seconds << 32 | fraction;

		return ntpTime;
	}
} //end class NTPTime
