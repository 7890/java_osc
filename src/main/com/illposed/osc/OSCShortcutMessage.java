/*
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import com.illposed.osc.utility.JavaToByteArrayConverter;
import com.illposed.osc.utility.OSCJavaToByteArrayConverter;
import com.illposed.osc.utility.Tagger;
import com.illposed.osc.utility.Debug;

import java.util.Collection;

public class OSCShortcutMessage extends OSCMessage {

	protected OSCShortcutManager osm=OSCShortcutManager.getInstance();

	public OSCShortcutMessage() {
		super();
	}

	public OSCShortcutMessage(String address) {
		super(address);
	}

	public OSCShortcutMessage(String address, Collection<Object> arguments) {
		super(address,arguments);
	}

	@Override
	protected byte[] computeByteArray(JavaToByteArrayConverter stream) {

		///lookup if available as shortcut
		OSCShortcut sc=osm.get(address+" "+Tagger.getTypesArray(arguments));
		if( sc!=null )
		{
//			System.err.println("found shortcut");

			//set new address, indicating oscshortcut (valid osc address)
			setAddress("/@");

			//create blob from arguments
			JavaToByteArrayConverter stream_all_args=new OSCJavaToByteArrayConverter();
			computePlainArgumentsByteArray(stream_all_args);
			byte[] all_args_in_one_blob=stream_all_args.toByteArray();

//			Debug.hexdump(all_args_in_one_blob);

			//remove original args
			clearArguments();

			//add args for shortened message: id, blob
			addArgument(sc.getID());
			addArgument(all_args_in_one_blob);

			//now after "rewriting" handle like normal..
		}

		computeAddressByteArray(stream);
		computeArgumentsByteArray(stream);

		return stream.toByteArray();
	}//end computeByteArray()
}//end class OSCShortcutMessage
//EOF
