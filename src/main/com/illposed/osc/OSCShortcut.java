/*
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

public class OSCShortcut {

	private String path;
	private String typetags;
	private int id;

	public OSCShortcut(String path, String typetags, int id)
	{
		this.path=path;
		this.typetags=typetags;
		this.id=id;
	}

	public int getID()
	{
		return id;
	}

	public String getPath()
	{
		return path;
	}

	public String getTypetags()
	{
		return typetags;
	}

	public String getSymbol()
	{
		return path+" "+typetags;
	}
}//end class OSCShortcut
//EOF
