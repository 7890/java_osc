/*
 * Copyright (C) 2016, T. Brand <tom@trellis.ch>
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

public class Debug {
	//helper method to dump binary contents of OSCMessage and OSCPackMessage payloads to stdout
	public static void hexdump(byte[] bytes)
	{
		int bytesPerLine = 16;
		int i;

		for (i = 0; i<bytes.length; i++)
		{
			if (i % bytesPerLine == 0)
			{
				if(i!=0)
				{
					System.err.print(" |");
					for(int k=i-bytesPerLine;k<i;k++)
					{
						if(bytes[k]<32) //if non-printable (first is space)
						{
							System.err.print(".");
						}
						else
						{
							try
							{
								System.err.printf("%c",(byte)bytes[k]);
							}catch(Exception e){System.err.print(".");}
						}
					}
					System.err.printf("|\n");
				}
				System.err.printf("%08x  ",i);
			}
			else if (i % (bytesPerLine/2) == 0 && i!=0) {System.err.print(" ");}
			System.err.printf("%02x ", bytes[i]);// & 0xff);
		}
		System.err.println();
	}
}//end class Debug
//EOF
