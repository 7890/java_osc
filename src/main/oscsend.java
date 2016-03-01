import java.net.*;
import com.illposed.osc.*;
import java.util.*;

//tb/160301
//mimic oscsend from liblo
//bare minimum

class oscsend
{
	public static void main(String[] args)
	{
//		System.out.println("oscsend");

		//parse args: host, port, message, typetags, args...
		//minimum #args: 3
		//if typetag contained: min #args 5
		if(args.length<3 || args.length==4)
		{
			System.err.println("syntax: <host> <port> <path> (<typetags> <args> ...)");
			System.exit(1);
		}

		try
		{
			String remote_host=args[0];
			int remote_port=Integer.parseInt(args[1]);
			String path=args[2];

			String typetags="";
			Vector msg_args=new Vector();
			OSCPortOut portOut=null;
		
			if(args.length>4)
			{
				typetags=args[3].trim();

				if(typetags.length()!=args.length-4)
				{
					System.err.println("error: typetags length does not match # of args");
					System.exit(1);
				}

				for(int i=0;i<(args.length-4);i++)
				{
					String type=typetags.substring(i,i+1);
					if(type.equals("s"))
					{
						msg_args.add(args[(i+4)]);
					}
					else if(type.equals("i"))
					{
						msg_args.add(new Integer( Integer.parseInt( args[(i+4)] ) ));
					}
					else if(type.equals("f"))
					{
						msg_args.add(new Float( Float.parseFloat( args[(i+4)] ) ));
					}
					else
					{
						System.err.println("type '"+type+"' not supported! (just 's', 'i' and 'f' for now).");
					}
				}
			}

			//create message, cast args to types according to typetag
			OSCMessage msg_out=null;
			if(!msg_args.isEmpty())
			{
				msg_out=new OSCMessage(path,msg_args);
			}
			else
			{
				msg_out=new OSCMessage(path);
			}

			//create port, to be used to send message to target host
			DatagramSocket ds=new DatagramSocket();//local_port);
			portOut=new OSCPortOut(InetAddress.getByName(remote_host), remote_port, ds);

			//send the message
			portOut.send(msg_out);

			System.err.println("message sent");

			//quit
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("oscsend terminated abnormally. please check that:");
			System.err.println("a) syntax is valid");
			System.err.println("b) typetags string is matching types of input arguments");

			System.exit(1);
		}
	}
}//end class oscsend
//EOF
