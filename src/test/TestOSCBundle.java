import java.net.*;

import com.illposed.osc.*;
import com.illposed.osc.utility.*;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;

//javac -cp _build/ TestOSCBundle.java && java -cp .:_build/ TestOSCBundle

public class TestOSCBundle
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
		TestOSCBundle t=new TestOSCBundle(args);
	}

	public TestOSCBundle(String[] args) throws Exception
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
		Debug.hexdump(o_norm.getByteArray());

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
		Debug.hexdump(op.getByteArray());

//===================================

		//mixed
		OSCBundle ob=new OSCBundle();
		ob.addPacket(o_norm);
		ob.addPacket(op);

		//mixed reverse
		OSCBundle ob2=new OSCBundle();
		ob2.addPacket(o_norm);
		ob2.addPacket(op);

		//bundle in bundle
		OSCBundle ob3=new OSCBundle();
		ob3.addPacket(ob);
		ob3.addPacket(ob2);

		//bundle in bundle with mixed
		OSCBundle ob4=new OSCBundle();
		ob4.addPacket(ob);
		ob4.addPacket(ob2);
		ob4.addPacket(o_norm);
		ob4.addPacket(op);

		//PACKED
		//mixed
		OSCBundle ob5=new OSCPackBundle();
		ob5.addPacket(o_norm);
		ob5.addPacket(op);

		//mixed reverse
		OSCBundle ob6=new OSCPackBundle();
		ob6.addPacket(o_norm);
		ob6.addPacket(op);

		//bundle in bundle
		OSCBundle ob7=new OSCPackBundle();
		ob7.addPacket(ob);
		ob7.addPacket(ob2);

		//bundle in bundle with mixed
		OSCBundle ob8=new OSCPackBundle();
		ob8.addPacket(ob); //normal
		ob8.addPacket(ob5); //pack
		ob8.addPacket(o_norm);
		ob8.addPacket(op);

		OSCBundle ob9=new OSCBundle();
		ob9.addPacket(ob); //normal
		ob9.addPacket(ob8); //pack
		ob9.addPacket(o_norm);
		ob9.addPacket(op);

		OSCBundle ob10=new OSCPackBundle();
		ob10.addPacket(ob);
		ob10.addPacket(ob2);
		ob10.addPacket(ob3);
		ob10.addPacket(ob4);
		ob10.addPacket(ob5);
		ob10.addPacket(ob6);
		ob10.addPacket(ob7);
		ob10.addPacket(ob8);
		ob10.addPacket(ob9);

		try
		{
			//create port with random portnumber, to be used to send message to target host
			DatagramSocket ds=new DatagramSocket();//local_port);
			OSCPortOut portOut=new OSCPortOut(InetAddress.getByName("localhost"), 7890, ds);

			portOut.setDebug(true);

			//send the message
//			portOut.send(ob);
//			portOut.send(ob2);
//			portOut.send(ob3);
//			portOut.send(ob4);

//			portOut.send(ob5);
//			portOut.send(ob6);
//			portOut.send(ob7);
//			portOut.send(ob8);
//			portOut.send(ob9);
			portOut.send(ob10);

			System.err.println("done!");

			portOut.close();
		}
		catch(Exception e){e.printStackTrace();}
	}// end TestOSCBundle constructor (run test)
}//end class TestOSCBundle
//EOF
