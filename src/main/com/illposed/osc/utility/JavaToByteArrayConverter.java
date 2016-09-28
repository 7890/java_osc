/*
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import javax.sound.midi.ShortMessage;

public interface JavaToByteArrayConverter {
	public Charset getCharset();
	public void setCharset(Charset charset);

	public byte[] toByteArray();
	public void write(byte[] bytes);
	public void write(int anInt);
	public void write(Float aFloat);
	public void write(Double aDouble);
	public void write(Integer anInt);
	public void write(Long aLong);
	public void write(Date timestamp);
	public void write(ShortMessage midievent);
	public void write(String aString);
	public void write(Character aChar);
	public void write(char aChar);
	public void write(Object anObject);
//	public void writeType(Class typeClass);
	public void writeTypes(Collection<Object> arguments);
}
