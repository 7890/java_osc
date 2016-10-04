import java.net.*;

import com.illposed.osc.*;
import com.illposed.osc.utility.*;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import javax.sound.midi.ShortMessage;

//javac -cp _build/ Test.java && java -cp .:_build/ Test

public class Test
{
	//these values are used to create both OSCPack and (regular) OSC messages
	public String address="/foo/bar";
	public String s="my string";
	public float f=1.23f;
	public double d=99.99d;
	public long l=100000000000001l; //i.e. change here to something smaller to see larger difference in ratio
	public int i=0;
	public boolean b=true;
	//null
	public char c='a';
	public Date date=new Date();
	public ArrayList ar=new ArrayList();

	//this list will hold all arguments, to be used to create messages conveniently
	public ArrayList msg_args=new ArrayList();

	public OSCShortcutManager osm=OSCShortcutManager.getInstance();

	//holding all messages and bundles to send
	public ArrayList<OSCPacket> packets = new ArrayList<OSCPacket>();

	public static void main(String[] args) throws Exception
	{
		Test t=new Test(args);
	}

	public Test(String[] args) throws Exception
	{
		ar.add(33);
		ar.add("last standard arg");
		ar.add(new ShortMessage(0xa1,0x7a,0x78));

		msg_args.add(s);
		msg_args.add(f);
		msg_args.add(d);
		msg_args.add(l);
		msg_args.add(i);
		msg_args.add(b);
		msg_args.add(null);
		msg_args.add(c);
		msg_args.add(date);
//		msg_args.add(ar); //try to add to OSCMessage object later

//===================================

		int msg_no=0;

		OSCMessage m0=new OSCMessage(address,msg_args);
		m0.addArguments(ar).add(msg_no++);
		//m.add(s).add(f)...;
		addAndDump(m0);

		//add shortcut with id 1233 for messages with this address and typetags
		//OSCShortcut os=
		//osm.add(new OSCShortcut("/foo/bar","sfdhiTNctismi",1233));
		int cnt=osm.load("./osc_shortcuts.txt");
		System.err.println(cnt+" shortcuts loaded from file.");

		OSCMessage m1=new OSCPackMessage(address,msg_args).addArguments(ar).add(msg_no++);
		addAndDump(m1);

		OSCMessage m2=new OSCShortcutMessage(address,msg_args).addArguments(ar).add(msg_no++);
		addAndDump(m2);

		OSCMessage m3=new OSCShortcutPackMessage(address,msg_args).addArguments(ar).add(msg_no++);
		addAndDump(m3);

		addBundleNestingsFor(m0);
		addBundleNestingsFor(m1);
		addBundleNestingsFor(m2);
		addBundleNestingsFor(m3);

		addPackBundleNestingsFor(m0);
		addPackBundleNestingsFor(m1);
		addPackBundleNestingsFor(m2);
		addPackBundleNestingsFor(m3);

		addMixedBundleNestingsFor(m0,m1,m2,m3);

//===================================

		try
		{
			//create port with random portnumber, to be used to send message to target host
			DatagramSocket ds=new DatagramSocket();//local_port);
			OSCPortOut portOut=new OSCPortOut(InetAddress.getByName("localhost"), 7890, ds);
//			portOut.setDebug(true);

			System.err.println("sending "+packets.size()+" packets. expexted message count: "+( 4 + (2*4*4) + 20 ));

			for (OSCPacket packet : packets)
			{
				portOut.send(packet);
			}

			System.err.println("done!");
			portOut.close();
		}
		catch(Exception e){e.printStackTrace();}
	}// end Test constructor (run test)

	public void addAndDump(OSCPacket op)
	{
		Debug.hexdump(op.getByteArray());
		packets.add(op);
	}

	/*
	Nestings:

	*Bundle
		*Message
	*Bundle
		*Bundle
			*...
	*Bundle
		*Message
		*Bundle
			*...
	*/
	public void addBundleNestingsFor(OSCMessage m)
	{
		OSCBundle b0=new OSCBundle().add(m);
		OSCBundle b1=new OSCBundle().add(b0);
		OSCBundle b2=new OSCBundle().add(m).add(b0);
		addAndDump(b0);
		addAndDump(b1);
		addAndDump(b2);
	}

	public void addPackBundleNestingsFor(OSCMessage m)
	{
		OSCBundle b0=new OSCPackBundle().add(m);
		OSCBundle b1=new OSCPackBundle().add(b0);
		OSCBundle b2=new OSCPackBundle().add(m).add(b0);
		addAndDump(b0);
		addAndDump(b1);
		addAndDump(b2);
	}

	public void addMixedBundleNestingsFor(OSCMessage m0,OSCMessage m1,OSCMessage m2,OSCMessage m3)
	{
		OSCBundle b0=new OSCBundle().add(m0).add(m1).add(m2).add(m3); //4
		OSCBundle b1=new OSCPackBundle().add(b0);	//4
		OSCBundle b2=new OSCBundle().add(m0).add(m1).add(m2).add(m3).add(b0).add(b1); //4 + 4 + 4 = 12
		addAndDump(b0);
		addAndDump(b1);
		addAndDump(b2);
		//total 5 * 4 = 20
	}
}//end class Test
//EOF
