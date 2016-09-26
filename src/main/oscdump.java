import java.net.*;
import com.illposed.osc.*;
import com.illposed.osc.utility.Debug;
import java.util.*;

//tb/160301
//mimic oscdump from liblo
//bare minimum

class oscdump
{
	static OSCPortIn portIn=null;
	static boolean shutdown_requested=false;

	static boolean debug=true;
	static boolean debug_port_in_dump	=false;
	static boolean debug_msg_in_dump	=false;

	public static void main(String[] args)
	{
//		System.out.println("oscdump");

		//parse arg: port
		//minimum #args: 1
		if(args.length<1 || args.length>2)
		{
			System.err.println("syntax: <port> (<filter string>)");
			System.err.println("default filter string: '//*'");
			System.exit(1);
		}

		try
		{
			int local_port=Integer.parseInt(args[0]);
		
			//create port, to be used to send message to target host
			DatagramSocket ds=new DatagramSocket(local_port);
			//OSCPortIn 
			portIn=new OSCPortIn(ds);

			String filter="//*";
			if(args.length>1)
			{
				filter=args[1];
			}

			if(debug && debug_port_in_dump)
			{
				portIn.setDebug(true);
			}

			// /!\  while /* matches every path with a SINGLE component,
			//      //* will match any message (with more than one part, separated by '/')
			portIn.addListener(filter, new GenericOSCListener());
			System.err.println("listening on UDP port "+local_port+", filtering for messages matching '"+filter+"'" );
			portIn.startListening();
			while(1==1)
			{
				try{Thread.sleep(1000);}catch(Exception e){}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("oscdump terminated abnormally. please check that:");
			System.err.println("a) syntax is valid");
			System.err.println("b) given (local) UDP port is not already bound by another program");
			System.err.println("c) the executing user has permissions to bind the port");
			System.err.println("please note: oscdump currently does not support all possible types.");

			System.exit(1);
		}
	}//end main()

//========================================================================
	void addShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				shutdown_requested=true;
				//prevent second shutdown if shutdown() called directly
				if(!shutdown_requested)
				{
					portIn.close();
					//quit
					System.exit(0);
				}
			}
		});
	}

//inner class
//========================================================================
//========================================================================
static class GenericOSCListener implements OSCListener
{
//========================================================================
	public void accept(OSCMessage msg)
	{
		String path=msg.getAddress();
		List<Object> args=msg.getArguments();
		int argsSize=args.size();
		//println("osc msg received: "+path+" ("+argsSize+" args)");

		try
		{
			System.out.print(portIn.getSuccessfullyProcessedCount()+") "
				+InetAddress.getByName(msg.getRemoteHost()).getHostName()+":"+msg.getRemotePort()+" ");
			System.out.print(path);
			if(argsSize>0)
			{
				//System.out.print(" ("+argsSize+" args)");
				System.out.print(" "+msg.getTypetagString());
				for(int i=0;i<argsSize;i++)
				{
					if(args.get(i) instanceof byte[])
					{
						System.out.print(" ["+((byte[])args.get(i)).length+" byte blob]");
					}
					else if(args.get(i) instanceof String)
					{
						System.out.print(" \""+(String)args.get(i)+"\"");
					}
					else if(args.get(i) instanceof Date)
					{
						System.out.print(" ("+(Date)args.get(i)+")");
					}
					else if(args.get(i) instanceof Character)
					{
						System.out.print(" '"+args.get(i)+"'");
					}
					else
					{
						System.out.print(" "+args.get(i));
					}
				}
			}
			System.out.println("");

			if(debug && debug_msg_in_dump)
			{
				Debug.hexdump(msg.getByteArray());
			}

			//reply to requester
			//portOut.setTarget( InetAddress.getByName(msg.getRemoteHost()), msg.getRemotePort() );
		}//end try
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}//end accept()

//========================================================================
	public void acceptMessage(Date time,OSCMessage msg) 
	{
		accept(msg);
	}
}//end inner class GenericOSCListener
}//end class oscdump
//EOF
