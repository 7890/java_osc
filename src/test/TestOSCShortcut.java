import java.net.*;

import com.illposed.osc.*;
import com.illposed.osc.utility.*;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;

//javac -cp _build/ TestOSCShortcut.java && java -cp .:_build/ TestOSCShortcut

public class TestOSCShortcut
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
		TestOSCShortcut t=new TestOSCShortcut(args);
	}

	public TestOSCShortcut(String[] args) throws Exception
	{
		ar.add(33);
		ar.add("last arg");

		//test fill shortcuts
		OSCShortcutManager osm=OSCShortcutManager.getInstance();
		OSCShortcut os=osm.add(new OSCShortcut("/foo/bar","sfdhiTNctis",1234));
//		OSCShortcut os=osm.add(new OSCShortcut("/a","",1235));
		System.out.println("OSCShortcut id: " + os.getID());
		System.out.println("OSCShortcut symbol: " + os.getSymbol());
		System.out.println("total count of OSCShortcuts: " + osm.size());

//===================================

		//shortcut
		OSCMessage sc=new OSCShortcutMessage(address);
		sc.add(s).add(f);
//		sc.add(f);
		sc.add(d);
		sc.add(l);
		sc.add(i);
		sc.add(b);
		sc.add(null);
		sc.add(c);
		sc.add(date);
		sc.addArguments(ar);
		Debug.hexdump(sc.getByteArray());

		OSCBundle b=new OSCBundle();
		b.add(sc).add(sc);

//===================================

		try
		{
			//create port with random portnumber, to be used to send message to target host
			DatagramSocket ds=new DatagramSocket();//local_port);
			OSCPortOut portOut=new OSCPortOut(InetAddress.getByName("localhost"), 7890, ds);
			portOut.setDebug(true);

			portOut.send(sc);
			portOut.send(b);

			System.err.println("done!");
			portOut.close();
		}
		catch(Exception e){e.printStackTrace();}
	}// end TestOSCShortcut constructor (run test)
}//end class TestOSCShortcut
//EOF
