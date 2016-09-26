import java.net.*;
import com.illposed.osc.*;
import com.illposed.osc.utility.*;

//javac -cp _build/ TestLarge.java && java -cp .:_build/ TestLarge

public class TestLarge
{
	public static void main(String[] args) throws Exception
	{
		TestLarge t=new TestLarge(args);
	}

	public TestLarge(String[] args) throws Exception
	{
		OSCMessage mx=new OSCMessage("/hi");
		byte[] payload=new byte[OSCPortIn.BUFFER_SIZE-16];
		payload[0]='a';
		payload[OSCPortIn.BUFFER_SIZE-17]='z';
		mx.add(payload);

		try
		{
			//create port with random portnumber, to be used to send message to target host
			DatagramSocket ds=new DatagramSocket();//local_port);
			OSCPortOut portOut=new OSCPortOut(InetAddress.getByName("localhost"), 7890, ds);
			portOut.setDebug(true);
			//send the message
			portOut.send(mx);
			System.err.println("done!");
			portOut.close();
		}
		catch(Exception e){e.printStackTrace();}
	}// end TestLarge constructor (run test)
}//end class TestLarge
//EOF
