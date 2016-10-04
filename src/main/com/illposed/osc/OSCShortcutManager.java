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
import java.io.BufferedReader;
import java.io.FileReader;

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

	public static int load(String file_uri)
	{
		//    0          1   2    3         4
		//    /.shortcut iss <id> <address> <typetags>

		int count_success=0;

		try {
			BufferedReader buffered_reader=new BufferedReader(new FileReader(file_uri));

			String line;
			while ((line = buffered_reader.readLine()) != null)
			{
//				System.err.println("input line: '"+line+"'");
				if(!line.startsWith("/.shortcut iss "))
				{
//					System.err.println("/!\\ invalid format for shortcut, line: '"+line+"'");
					continue;
				}
				else
				{
					String[] tokens = line.split(" "); //split space
//					System.err.println("# line tokens: "+tokens.length);
					if(tokens.length != 5)
					{
						System.err.println("/!\\ invalid format for shortcut, line: '"+line+"'");
						continue;
					}
					else
					{
						String tt=tokens[4];
						if(tokens[4].equals("\"\"") || tokens[4].equals("''"))
						{
							tt="";
						}
//						System.err.println( Integer.parseInt(tokens[2])+" "+tokens[3]+" "+tt);
						if(null!=
							add(new OSCShortcut(tokens[3] ,tt, Integer.parseInt(tokens[2]))))
						{
							count_success++;
						}
					}
				}
			}//end while
		}
		catch(Exception e){e.printStackTrace();}
		return count_success;
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
