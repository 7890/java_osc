import java.net.*;
import com.illposed.osc.*;
import java.util.*;

//tb/160301
//mimic oscsend from liblo

class oscsend
{
	public static void main(String[] args)
	{
//		System.out.println("oscsend");
		if(args.length<3)
		{
			System.err.println("syntax: <host> <port> <message address/path> (<typetags> (<args> ...))");
			System.err.println("supported types: i, h, f, d, s, c, N, T, F, I, t");
			System.err.println("types without argument: N, T, F, I");
			System.err.println("c: single character or hex number format '0x00'");
			System.err.println("t: 'now' or long (unix time, millis since 1970)");
			System.err.println("oscsend does support UDP only.");
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
		
			if(args.length>3)
			{
				typetags=args[3].trim();

				//index where argument values start in command line args array
				//incremented for every type that takes an argument
				int arg_index=4;

				for(int i=0;i<typetags.length();i++)
				{
					String type=typetags.substring(i,i+1);

					if(type.equals("i"))
					{
						msg_args.add(new Integer( Integer.parseInt( args[arg_index] ) ));
						arg_index++;
					}
					else if(type.equals("h"))
					{
						msg_args.add(new Long( Long.parseLong( args[arg_index] ) ));
						arg_index++;
					}
					else if(type.equals("f"))
					{
						msg_args.add(new Float( Float.parseFloat( args[arg_index] ) ));
						arg_index++;
					}
					else if(type.equals("d"))
					{
						msg_args.add(new Double( Double.parseDouble( args[arg_index] ) ));
						arg_index++;
					}
					else if(type.equals("s"))
					{
						msg_args.add(args[arg_index]);
						arg_index++;
					}
/*
					else if(type.equals("b"))
					{
						//read from file?
					}
*/
					else if(type.equals("c"))
					{
						String cstr=args[arg_index];
						if(cstr.length()==4)
						{
							//try parse hex 0x00 format
							long charno=Long.decode(cstr);
							msg_args.add((char)charno);
						}
						else
						{
							msg_args.add(cstr.charAt(0));
						}
						arg_index++;
					}
					else if(type.equals("N"))
					{
						msg_args.add(null);
					}
					else if(type.equals("T"))
					{
						msg_args.add(true);
					}
					else if(type.equals("F"))
					{
						msg_args.add(false);
					}
					else if(type.equals("I"))
					{
						msg_args.add(OSCImpulse.INSTANCE);
					}
					else if(type.equals("t"))
					{
						String date=args[arg_index];
						if(date.toLowerCase().equals("now"))
						{
							msg_args.add(new Date());
						}
						else//try parse date as long (unix time 1970 epoch)
						{
							msg_args.add( new Date(new Long( Long.parseLong( date ) )));
						}
						///could use string & format, or take values like db93219d.449ba5e3
						arg_index++;
					}
					else
					{
						System.err.println("type '"+type+"' not supported!");
						System.exit(1);
					}
				}
				if(args.length>arg_index)
				{
					System.err.println("error: typetags length does not match # of args");
					System.exit(1);
				}
			}//end args.length >3

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
			portOut.close();
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
