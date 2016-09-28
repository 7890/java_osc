/*
 * Copyright (C) 2003-2014, C. Ramakrishnan / Illposed Software.
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

import com.illposed.osc.OSCImpulse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.sound.midi.ShortMessage;

public class Tagger {

	public Tagger() {}

	public static void addTypeToStringBuffer(Class typeClass, StringBuffer sb) {

		if (Integer.class.equals(typeClass)) {
			sb.append("i");
		} else if (Long.class.equals(typeClass)) {
			sb.append("h");
		} else if (Date.class.equals(typeClass)) {
			sb.append("t");
		} else if (Float.class.equals(typeClass)) {
			sb.append("f");
		} else if (Double.class.equals(typeClass)) {
			sb.append("d");
		} else if (String.class.equals(typeClass)) {
			sb.append("s");
		} else if (byte[].class.equals(typeClass)) {
			sb.append("b");
		} else if (Character.class.equals(typeClass)) {
			sb.append("c");
		} else if (OSCImpulse.class.equals(typeClass)) {
			sb.append("I");
		} else if (ShortMessage.class.equals(typeClass)) {
			sb.append("m");
		} else {
			throw new UnsupportedOperationException("Do not know the OSC type for the java class: "
					+ typeClass);
		}
	}

	public static String getTypesArray(Collection<Object> arguments) {

		StringBuffer sb=new StringBuffer();
		for (final Object argument : arguments) {
			if (null == argument) {
				sb.append("N");
			} else if (argument instanceof Collection) {
				// If the array at i is a type of array, write a '['.
				// This is used for nested arguments.
				sb.append("[");
				// fill the [] with the SuperCollider types corresponding to
				// the object (e.g., Object of type String needs -s).
				// XXX Why not call this function, recursively? 
				// The only reason would be, to not allow nested arrays, but the 
				// specification does not say anythign about them not being allowed.
				sb.append(getTypesArray((Collection<Object>) argument));
				// close the array
				sb.append("]");
			} else if (Boolean.TRUE.equals(argument)) {
				sb.append("T");
			} else if (Boolean.FALSE.equals(argument)) {
				sb.append("F");
			} else {
				// go through the array and write the superCollider types as shown
				// in the above method.
				// The classes derived here are used as the arg to the above method.
				addTypeToStringBuffer(argument.getClass(),sb);
			}
		}
		return sb.toString();
	}
}//end class Tagger
//EOF
