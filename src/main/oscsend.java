import java.net.*;
import com.illposed.osc.*;
import java.util.*;
import java.io.RandomAccessFile;
import javax.sound.midi.ShortMessage;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;
//tb/160301
//mimic oscsend from liblo

class oscsend
{
	static int absolute_arg_index=0;

	public static OSCMessage createMessageFromArgs(String[] args) throws Exception
	{
		String path=args[absolute_arg_index];
		absolute_arg_index++;

		String typetags="";
		Vector msg_args=new Vector();

		boolean pack=false;

		if(path.charAt(0)=='!')
		{
			path=path.substring(1,path.length());
			pack=true;
		}

		if(args.length>absolute_arg_index)
		{
			typetags=args[absolute_arg_index].trim();

			//if what could be the typetag looks like an address, return here
			if(typetags.charAt(0)=='/')
			{
				//"path-only" message
				if(pack)
				{
					return new OSCPackMessage(path);
				}
				else
				{
					return new OSCMessage(path);
				}
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
				else if(type.equals("b"))
				{
					String hex_or_filename=args[absolute_arg_index];
					if(hex_or_filename.startsWith("0x")) //read as string
					{
						//cut leading 0x for parsing
						hex_or_filename=hex_or_filename.substring(2,hex_or_filename.length());
						//make sure it's an even number of digits (two digits for each byte)
						if(hex_or_filename.length() % 2 != 0){hex_or_filename+="0";}
						final byte[] b=DatatypeConverter.parseHexBinary(hex_or_filename);
						msg_args.add(b);
					}
					else //read from file
					{
						String filename=args[absolute_arg_index];
						RandomAccessFile f = new RandomAccessFile(filename, "r");
						final byte[] b = new byte[(int)f.length()];
						f.readFully(b);
						msg_args.add(b);
					}
					absolute_arg_index++;
				}
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
				else if(type.equals("m"))
				{
					String hex=args[absolute_arg_index];
					if(!hex.startsWith("0x"))
					{
						System.err.println("error: invalid hex string.");

						System.exit(1);
					}
					//cut leading 0x for parsing
					hex=hex.substring(2,hex.length());
					//make sure it's an even number of digits (two digits for each byte)
					if(hex.length() % 2 != 0){hex+="0";}

					final byte[] b=DatatypeConverter.parseHexBinary(hex);
					int len=b.length;
					if(len==1)      { msg_args.add(new ShortMessage( (int)(b[0] & 0xff), 0,                  0 )); }
					else if(len==2) { msg_args.add(new ShortMessage( (int)(b[0] & 0xff), (int)(b[1] & 0xff), 0 )); }
					else if(len>2) { msg_args.add(new ShortMessage( (int)(b[0] & 0xff), (int)(b[1] & 0xff), (int)(b[2] & 0xff)));}
					absolute_arg_index++;
				}
				else if(type.equals("[") || type.equals("]"))
				{///
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
			if(pack)
			{
				msg_out=new OSCPackMessage(path,msg_args);
			}
			else
			{
				msg_out=new OSCMessage(path,msg_args);
			}
		}
		else
		{
			if(pack)
			{
				msg_out=new OSCPackMessage(path);
			}
			else
			{
				msg_out=new OSCMessage(path);
			}
		}
		return msg_out;
	}

	public static void main(String[] args)
	{
//		System.out.println("oscsend");
		if(args.length<3)
		{
			System.err.println("oscsend - send OSC (Open Sound Control) messages\n");

			System.err.println("usage: oscsend <hostname> <port> <address> (<types> (<values> ...))\n");

			System.err.println("description");
			System.err.println("hostname: the remote hostname or IP address");
			System.err.println("port    : the remote UDP port to connect to\n");

			System.err.println("address : the OSC address (path) for the message");
			System.err.println("types   : a string setting the types of the values that follow\n");

			System.err.println("    i - 32bit integer");
			System.err.println("    h - 64bit integer");
			System.err.println("    f - 32bit floating point number");
			System.err.println("    d - 64bit (double) floating point number");
			System.err.println("    s - string");
			System.err.println("    b - blob (binary / raw bytes)");
			System.err.println("        hex notation 0x..(...), i.e. 0xffff");
			System.err.println("        OR path to file, used as blob content");
			System.err.println("    c - char");
			System.err.println("        single character or hex byte format '0x00'");
			System.err.println("    N - NIL           (no value required)");
			System.err.println("    T - TRUE          (no value required)");
			System.err.println("    F - FALSE         (no value required)");
			System.err.println("    I - INFINITUM     (no value required)");
			System.err.println("    m - MIDI 1-3 bytes forming a valid message");
			System.err.println("        hex notation 0x..(...), i.e. 0xfa or 0xae7f02");
			System.err.println("        missing bytes will be compensated with 00");
			System.err.println("        superfluous bytes are ignored.");
			System.err.println("    t - timestamp");
			System.err.println("        literal 'NOW' or long (type 'h', unix time millis)\n");

			System.err.println("-the minimal valid message must only have an address set.");
			System.err.println("-a message can contain a string (typetags) defining the types to transmit.");
			System.err.println("-the typetags are followed by 0 or more values corresponding to the typetags.\n");

			System.err.println("to send a bundle, add the next message starting with the address right after the last message.\n");

			System.err.println("to pack a message (using MessagePack library), add a '!' in front of the address.\n");

			System.err.println("examples");
			System.err.println("  send simple message: oscsend localhost 7890 /hi");
			System.err.println("  send bundle: oscsend localhost 7890 /hi /hutsefluts h 42 /foo ifs 1 .2 \"bar last\"");
			System.err.println("  send timestamp: oscsend localhost 7890 /x t now");
			System.err.println("  send blob: oscsend localhost 7890 /y b /etc/lsb-release");
			System.err.println("  send packed message (experimental, non-OSC): oscsend localhost 7890 '!/foo' iii 0 1 2\n");

			System.err.println("oscsend supports UDP only.");
			System.err.println("the maximum UDP payload size (serialized OSC message byte array) can not exceed "+OSCPortIn.BUFFER_SIZE+" bytes.");
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
