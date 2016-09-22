import java.net.*;

import com.illposed.osc.*;
import com.illposed.osc.utility.*;

import java.util.List;
import java.util.Date;

//javac -cp _build/ TestOSCPack.java && java -cp .:_build/ TestOSCPack

public class TestOSCPack
{
	//these values are used to create both OSCPack and (regular) OSC messages
	static String address="/foo/bar";
	static String s="my string";
	static float f=1.23f;
	static double d=99.99d;
	static long l=100000000000001l; //i.e. change here to something smaller to see larger difference in ratio
	static int i=0;
	static boolean b=true;
	//null
	static char c='a';
	static Date date=new Date();

	public static void main(String[] args) throws Exception
	{
		TestOSCPack t=new TestOSCPack(args);
	}

	public TestOSCPack(String[] args) throws Exception
	{
		//create new empty message like a regular osc message but using OSCPackMessage class
		OSCMessage op=new OSCPackMessage();
							//index in msgpack
		//! first char indicates oscpack	//0 (cut-off by OSCPortIn)
		op.setAddress(address);			//0 address
							//1 types as one string
		op.addArgument(s);			//2
		op.addArgument(f);			//...
		op.addArgument(d);
		op.addArgument(l);
		op.addArgument(i);
		op.addArgument(b);
		op.addArgument(null);
		op.addArgument(c);
		op.addArgument(date);

		//binary representation of OSCPackMessage object
		byte[] b1=op.getByteArray();
		//show byte by byte
		System.out.println("OSCPack dump:");
		hexdump(b1);
		//remember for comparison
		int i1=b1.length;

		OSCPackByteArrayToJavaConverter conv=new OSCPackByteArrayToJavaConverter();

		//create regular OSCMessage object from OSCPackMessage binary format
		OSCMessage op_o=(OSCMessage)conv.convert(b1, b1.length);

		//get header props
		System.out.println("address: "+op_o.getAddress());
		System.out.println("typetags: "+op_o.getTypetagString());

		//show args
		List<Object> msgargs=op_o.getArguments();
		System.out.println("arg count: "+msgargs.size());

		for(int i=0;i<msgargs.size();i++)
		{
			System.out.println("arg value["+i+"]: "+msgargs.get(i));
		}

		System.out.println("===");

		//create the same message as regular OSCMessage object
		OSCMessage o=new OSCMessage();
		o.setAddress(address);
		o.addArgument(s);
		o.addArgument(f);
		o.addArgument(d);
		o.addArgument(l);
		o.addArgument(i);
		o.addArgument(b);
		o.addArgument(null);
		o.addArgument(c);
		o.addArgument(date);

		byte[] b2=o.getByteArray();
		System.out.println("OSC dump:");
		hexdump(b2);
		int i2=b2.length;

		System.out.println("===");
		//show ratio: OSCPack / OSC -> should usually result in a SMALLER THAN 1 value
		System.out.println("OSCPack size: " + i1 + " OSC size: " + i2 + " ratio OSCPack/OSC (without UDP framing): " + (float)i1/i2);

		try
		{
			//create port with random portnumber, to be used to send message to target host
			DatagramSocket ds=new DatagramSocket();//local_port);
			OSCPortOut portOut=new OSCPortOut(InetAddress.getByName("localhost"), 7890, ds);

			//send the message
			portOut.send(op_o);	//OSCPackMessage converted to OSCMessage
			portOut.send(o);	//OSCMessage
			portOut.send(op);	//OSCPackMesasge (regular osc libraries will fail)

			System.err.println("3 messages sent to osc.udp://localhost:7890\nthe last message only works with OSCPack enabled libraries.");
			System.err.println("done!");

			portOut.close();
		}
		catch(Exception e){e.printStackTrace();}
	}// end TestOSCPack constructor (run test)

	//helper method to dump binary contents of OSCMessage and OSCPackMessage payloads
	public void hexdump(byte[] bytes)
	{
		int bytesPerLine = 16;
		int i;

		for (i = 0; i<bytes.length; i++)
		{
			if (i % bytesPerLine == 0 && i!=0) {System.out.println();}
			else if (i % (bytesPerLine/2) == 0 && i!=0) {System.out.print(" ");}
			System.out.printf("%02x ", bytes[i]);// & 0xff);
		}
		System.out.println();
	}
}//end class TestOSCPack
//EOF
