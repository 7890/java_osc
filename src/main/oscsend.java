import java.net.*;
import com.illposed.osc.*;
import java.util.*;

//tb/160301
//mimic oscsend from liblo

class oscsend
{
	static int absolute_arg_index=0;

	public static OSCMessage createMessageFromArgs(String[] args)
	{
		String path=args[absolute_arg_index];
		absolute_arg_index++;

		String typetags="";
		Vector msg_args=new Vector();

		if(args.length>absolute_arg_index)
		{
			typetags=args[absolute_arg_index].trim();

			//if what could be the typetag looks like an address, return here
			if(typetags.charAt(0)=='/')
			{
				//"path-only" message
				return new OSCMessage(path);
			}

			absolute_arg_index++;

			for(int i=0;i<typetags.length();i++)
			{
				String type=typetags.substring(i,i+1);

				if(type.equals("i"))
				{
					msg_args.add(new Integer( Integer.parseInt( args[absolute_arg_index] ) ));
					absolute_arg_index++;
				}
				else if(type.equals("h"))
				{
					msg_args.add(new Long( Long.parseLong( args[absolute_arg_index] ) ));
					absolute_arg_index++;
				}
				else if(type.equals("f"))
				{
					msg_args.add(new Float( Float.parseFloat( args[absolute_arg_index] ) ));
					absolute_arg_index++;
				}
				else if(type.equals("d"))
				{
					msg_args.add(new Double( Double.parseDouble( args[absolute_arg_index] ) ));
					absolute_arg_index++;
				}
				else if(type.equals("s"))
				{
					msg_args.add(args[absolute_arg_index]);
					absolute_arg_index++;
				}
/*
				else if(type.equals("b"))
				{
					//read from file?
				}
*/
				else if(type.equals("c"))
				{
					String cstr=args[absolute_arg_index];
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
					absolute_arg_index++;
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
					String date=args[absolute_arg_index];
					if(date.toLowerCase().equals("now"))
					{
						msg_args.add(new Date());
					}
					else//try parse date as long (unix time 1970 epoch)
					{
						msg_args.add( new Date(new Long( Long.parseLong( date ) )));
					}
					///could use string & format, or take values like db93219d.449ba5e3
					absolute_arg_index++;
				}
				else
				{
					System.err.println("type '"+type+"' not supported!");
					System.exit(1);
				}
			}//end for typetags.length
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
		return msg_out;
	}

	public static void main(String[] args)
	{
//		System.out.println("oscsend");
		if(args.length<3)
		{
			System.err.println("syntax: oscsend <host> <port> <message address/path> (<typetags> (<args> ...))");
			System.err.println("supported types: i, h, f, d, s, c, N, T, F, I, t");
			System.err.println("types without argument: N, T, F, I");
			System.err.println("c: single character or hex number format '0x00'");
			System.err.println("t: 'now' or long (unix time, millis since 1970)");
			System.err.println("-a message must at least have an address (with no typetags and args)");
			System.err.println("-a message can have a typetag");
			System.err.println("-a typetag is followed by 0 or more arguments");
			System.err.println("to send a bundle, add the next message starting with the address right after the last message.");
			System.err.println("examples");
			System.err.println("  send simple message: oscsend localhost 7890 /hi");
			System.err.println("  send bundle: oscsend localhost 7890 /hi /hutsefluts h 42 /foo ifs 1 .2 \"bar last\"");
			System.err.println("oscsend does support UDP only.");
			System.exit(1);
		}

		try
		{
			String remote_host=args[0];
			int remote_port=Integer.parseInt(args[1]);
			absolute_arg_index+=2;

			OSCMessage message=null;
			OSCBundle bundle=new OSCBundle();
			int message_count=0;
			while(absolute_arg_index<args.length)
			{
				message=createMessageFromArgs(args);
				bundle.add(message);
				message_count++;
			}

			//create port, to be used to send message to target host
			DatagramSocket ds=new DatagramSocket();//local_port);
			OSCPortOut portOut=new OSCPortOut(InetAddress.getByName(remote_host), remote_port, ds);

			//send the bundle or message
			if(message_count>1 && bundle!=null)
			{
				portOut.send(bundle);
			}
			else if(message!= null)
			{
				portOut.send(message);
			}
			else
			{
				System.err.println("no bundle or message to send!");
				portOut.close();
				System.exit(1);
			}

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
	}//end main()
}//end class oscsend
//EOF
