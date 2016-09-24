import java.net.*;

import com.illposed.osc.*;
import com.illposed.osc.utility.*;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;

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

	static ArrayList ar=new ArrayList();

	public static void main(String[] args) throws Exception
	{
		TestOSCPack t=new TestOSCPack(args);
	}

	public TestOSCPack(String[] args) throws Exception
	{
		ar.add(33);
		ar.add("last arg");

		OSCMessage o_norm=new OSCMessage();

		o_norm.setAddress(address);
		o_norm.add(s);
		o_norm.add(f);
		o_norm.add(d);
		o_norm.add(l);
		o_norm.add(i);
		o_norm.add(b);
		o_norm.add(null);
		o_norm.add(c);
		o_norm.add(date);
		o_norm.addArguments(ar);

		//binary representation of OSCPackMessage object
		byte[] b0=o_norm.getByteArray();
		//show byte by byte
		System.out.println("OSC (normal) dump:");
		Debug.hexdump(b0);
		//remember for comparison
		int i0=b0.length;

//===================================

		//create new empty message like a regular osc message but using OSCPackMessage class
		OSCMessage op=new OSCPackMessage();
							//index in msgpack
		//! first char indicates oscpack	//0 (cut-off by OSCPortIn)
		op.setAddress(address);			//0 address
							//1 types as one string
		op.add(s);				//2
		op.add(f);				//...
		op.add(d);
		op.add(l);
		op.add(i);
		op.add(b);
		op.add(null);
		op.add(c);
		op.add(date);
		op.addArguments(ar);

		//binary representation of OSCPackMessage object
		byte[] b1=op.getByteArray();
		//show byte by byte
		System.out.println("OSCPack dump:");
		Debug.hexdump(b1);
		//remember for comparison
		int i1=b1.length;

//===================================

		OSCPackByteArrayToJavaConverter conv=new OSCPackByteArrayToJavaConverter();

		//convert (parse) OSCPackMessage binary format to OSCMessage object
		OSCMessage op_o=(OSCMessage)conv.convert(b1, b1.length);

		System.out.println("results from parsing OSCPack bytes to Java object:");

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

//===================================

		//show ratio: OSCPack / OSC -> should usually result in a SMALLER THAN 1 value
		System.out.println("OSC size: " + i0 + " OSCPack size: " + i1 + " ratio OSCPack/OSC (without UDP framing): " + (float)i1/i0);

		try
		{
			//create port with random portnumber, to be used to send message to target host
			DatagramSocket ds=new DatagramSocket();//local_port);
			OSCPortOut portOut=new OSCPortOut(InetAddress.getByName("localhost"), 7890, ds);

			//send the message
			portOut.send(o_norm);	//OSCMessage
			portOut.send(op_o);	//OSCPackMessage converted from OSCPack bytes
			portOut.send(op);	//OSCPackMesasge (regular osc libraries will fail)

			System.err.println("3 messages sent to osc.udp://localhost:7890\nsome messages only work with OSCPack enabled libraries.");
			System.err.println("done!");

			portOut.close();
		}
		catch(Exception e){e.printStackTrace();}
	}// end TestOSCPack constructor (run test)
}//end class TestOSCPack
//EOF
