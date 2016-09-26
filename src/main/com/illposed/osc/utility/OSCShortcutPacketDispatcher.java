/*
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

import com.illposed.osc.OSCShortcutManager;
import com.illposed.osc.OSCShortcut;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPackMessage;
import com.illposed.osc.OSCShortcutMessage;
import com.illposed.osc.AddressSelector;
import com.illposed.osc.OSCListener;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class OSCShortcutPacketDispatcher extends OSCPacketDispatcher {

	protected OSCShortcutManager osm=OSCShortcutManager.getInstance();

	public OSCShortcutPacketDispatcher() {
		super();
	}

	//
	protected boolean isOSCShortcutMessage(OSCMessage m)
	{
		return (m.getAddress().equals("/@") && m.getTypetagString().equals("ib"));
	}

	//
	protected OSCMessage unfoldOSCShortcutMessage(OSCMessage m)
	{
		if(!isOSCShortcutMessage(m))
		{
			return m;
		}

		final List<Object> args=m.getArguments();
		int id=(Integer)args.get(0);
//		System.err.println("id: "+id);

		OSCShortcut sc=osm.get(id);

		if( sc!=null )
		{
//			System.err.println("found shortcut "+sc.getPath()+" "+sc.getTypetags() );
			Debug.hexdump(m.getByteArray());

			byte[] b=(byte[])args.get(1);

			Debug.hexdump(b);

			return new OSCMessage(sc.getPath(),sc.getTypetags(),b);
		}

		//maybe throw error?
		//for now, return unchanged message (/@ ib)
		return m;
	}

	@Override
	protected void dispatchMessage(OSCMessage message, Date time) {

		message=unfoldOSCShortcutMessage(message);
		for (final Entry<AddressSelector, OSCListener> addrList : selectorToListener.entrySet()) {
			if (addrList.getKey().matches(message.getAddress())) {
				addrList.getValue().acceptMessage(time, message);
			}
		}
	}
}//end class OSCShortcutPacketDispatcher
//EOF
