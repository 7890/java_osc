/*
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import java.util.HashMap;
import java.util.List;

public class OSCShortcutManager {

	private static HashMap<Integer,OSCShortcut> shortcut_ids=new HashMap<Integer,OSCShortcut>(100); //initial capacity
	private static HashMap<String,OSCShortcut> shortcut_symbols=new HashMap<String,OSCShortcut>(100);

	//for every addition or insert operation: check if an object with the same signature already exists
	//if true, the hashmap will only contain unique symbols (which in most cases makes sense)
	private static boolean insert_unique=true;

	//make it a singleton
	private static OSCShortcutManager instance = null;

	private OSCShortcutManager() {
		//exists only to disallow direct instantiation
	}

	public static OSCShortcutManager getInstance() {
		if (instance == null) {
			instance = new OSCShortcutManager();
		}
		return instance;
	}

	public static int size()
	{
		int total = 0;
		for(OSCShortcut os : shortcut_ids.values()) {
			total++;
		}
		return total;
	}

	public static boolean exists(OSCShortcut o)
	{
		//assuming addition was done for both data structures it's enough to check one of them
		if(shortcut_ids.get(o.getID())==null) {return false;}
		return true;
	}

	public static boolean exists(String symbol)
	{
		if(shortcut_symbols.get(symbol)==null) {return false;}
		return true;
	}

	public static boolean exists(int id)
	{
		if(shortcut_ids.get(id)==null) {return false;}
		return true;
	}

	public static OSCShortcut get(String symbol)
	{
		return shortcut_symbols.get(symbol); //can be null
	}

	public static OSCShortcut get(int id)
	{
		return shortcut_ids.get(id); //can be null
	}

	public static OSCShortcut ins(OSCShortcut o, int i)
	{
		if(insert_unique && exists(o)){return null;}
		shortcut_ids.put(o.getID(),o);
		shortcut_symbols.put(o.getSymbol(),o);
		return o;
	}

	public static OSCShortcut add(OSCShortcut o)
	{
		if(insert_unique && exists(o)){return null;}
		shortcut_ids.put(o.getID(),o);
		shortcut_symbols.put(o.getSymbol(),o);
		return o;
	}

	public static boolean rm(OSCShortcut o)
	{
		if(!exists(o)){return false;}
		shortcut_ids.remove(o.getID());
		shortcut_symbols.remove(o.getSymbol());
		return true;
	}

	public static boolean rm(int i)
	{
		if(i < 0 || i > shortcut_ids.size()-1){return false;}
		String s = shortcut_ids.get(i).getSymbol();
		shortcut_ids.remove(i);
		shortcut_symbols.remove(s);
		return true;
	}
}//end class OSCShortcutManager
//EOF
