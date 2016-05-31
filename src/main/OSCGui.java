import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.event.*;

import java.net.*;
import java.io.*;

import com.illposed.osc.*;
import java.util.*;
import java.text.*;

//tb/160312
//=============================================================================
//=============================================================================
public class OSCGui
{
	//this file is loaded if found in current directory
	private String propertiesFileUri="OSCGui.properties";

	//===configurable parameters (here: default values)
	public int 	local_osc_port 			=3090;

	public String	remote_osc_host 		="127.0.0.1";
	public int 	remote_osc_port			=3091;

	public int 	poll_thread_sleep_count 	=1000; //ms
	public boolean 	poll_forever 			=false; //false: once

	public String 	message_string			="/ping;fis;0.123;4;five six seven"; //messages MUST start with '/'

	public boolean 	send_after_program_start 	=false;

	public String 	message_filter_path		="//*"; //match every message starting with '/'

	public boolean 	log_to_file 			=false;
	//directory will be created if not existing and loggin enabled
	public String 	log_file_base_path		="./logs";
	//prefix of log filenames
	public String 	log_file_prefix			="osc_log_";
	//date pattern to use for filenames (after prefix, before postfix)
	public String 	date_format_string		="yyyy-MM-dd_HH-mm-ss";
	//postfix for log filenames
	public String 	log_file_postfix		=".csv";
	public String 	csv_separator			=";";

	public String	main_window_title		="OSCGui";
	public boolean 	start_iconified			=false;
	public boolean	always_on_top			=false;
	public boolean 	hide_window_decoration		=false;
	public int	initial_width			=600;
	public int	initial_height			=300;
	public int	initial_placement_x		=0;
	public int	initial_placement_y		=0;

	//===end configurable parameters

	//will be generated
	private String log_file_uri="";
	private PrintWriter printwriter=null;
	private SimpleDateFormat date_format;
	private String timezone_id="UTC"; //fixed

	private int thread_do_exit=0;

	private OSCPortOut portOut; //send
	private OSCPortIn portIn; //receive

	private OSCMessage msg_send;

	private long last_status_set_millis=0;

	//gui objects
	private JFrame main_frame;

	private JPanel panel_south;

	private JLabel label_status;
	private JTextField tf_last_received_message;

	private JPanel panel_form_outter;
	private JPanel panel_form;
	private JPanel panel_form_long;

	private JLabel label_local_port;
	private JLabel label_remote_host;
	private JLabel label_remote_port;
	private JLabel label_repeat;
	private JLabel label_interval_ms;
	private JLabel label_message_string;
	private JLabel label_message_filter_path;
	private JLabel label_log_to_file;
	private JLabel label_send_after_program_start;
	private JLabel label_log_file_uri;

	private JTextField tf_local_port;
	private JTextField tf_remote_host;
	private JTextField tf_remote_port;
	private JCheckBox cb_repeat;
	private JTextField tf_interval_ms;
	private JTextField tf_message_string;
	private JTextField tf_message_filter_path;
	private JCheckBox cb_log_to_file;
	private JCheckBox cb_send_after_program_start;

	private JTextField tf_log_file_uri;

	private JPanel panel_buttons;
	private JButton button_send;

	private JPanel panel_comm;
	private JPanel panel_indicate_send;
	private JPanel panel_indicate_receive;

	private int indicate_send_toggle_state=0;
	private int indicate_receive_toggle_state=0;

	private Color indicate_color_low=new Color(10,10,10);
	private Color indicate_color_high=new Color(10,90,10);
	private Color default_button_color_disabled=new JButton().getBackground();
	private Color default_button_color_enabled=new Color(0,255,0);

//========================================================================
	public static void main(String[] args)
	{
		new OSCGui(args);
	}

//========================================================================
	public String createLogFileUri() throws Exception
	{
		File log_dir=new File(log_file_base_path);
		if(!log_dir.exists())
		{
			System.err.println("Creating log dir: "+log_file_base_path);
			log_dir.mkdirs();
		}
		if(!log_dir.exists() || !log_dir.isDirectory() || !log_dir.canWrite())
		{
			throw new Exception("Invalid log_file_base_path: "+log_file_base_path);
		}

		date_format=new SimpleDateFormat(date_format_string);
		date_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
		String log_file_name=log_file_prefix+date_format.format(new Date())
			+"_"+timezone_id+log_file_postfix;
		String log_file_uri=log_file_base_path+File.separator+log_file_name;
		return log_file_uri;
	}

//========================================================================
	public void createLogWriter(String log_file_uri) throws Exception
	{
		printwriter=new PrintWriter(log_file_uri, "UTF-8");
		e("Logging to file "+log_file_uri);
	}

//========================================================================
	public OSCGui(String[] args)
	{
		DTime.setTimeZoneUTC();

		if(args.length==1
			&& (args[0].equals("-h") || args[0].equals("--help"))
			|| args.length>2
		)
		{
			System.out.println("This jar file can be used as a library (in the Java classpath) that provides com.illposed.osc.*.\n");
			System.out.println("However it also contains classes with a main method");
			System.out.println("(such as this help text, used as default main class when started with java -jar).\n");
			System.out.println("Use oscsend: java -cp <path to this jar> oscsend <oscsend args> ...\n");
			System.out.println("Use oscdump: java -cp <path to this jar> oscdump <oscdump args> ...\n");
			System.out.println("Use OSCGui: java -jar <path to this jar>");
			System.err.println("Syntax: -c (config file)");
			System.err.println("Example: -c my.properties\n");
			System.err.println("Default properties file: ./"+propertiesFileUri);
			System.err.println("If no parameters provided, default values will be used.\n");
			System.exit(0);
		}

		if(args.length==2 && (args[0].equals("-c") || args[0].equals("--config")))
		{
			if(!loadProps(args[1]))
			{
				System.err.println("Could not load properties "+args[1]);
				System.exit(1);
			}
		}
		else
		{
			if(!loadProps(propertiesFileUri))
			{
				e("Could not load default properties "+propertiesFileUri);
			}
		}

		addShutdownHook();

		if(log_to_file)
		{
			try
			{
				log_file_uri=createLogFileUri();
				createLogWriter(log_file_uri);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}

		setup_gui();
		create_action_listeners();

		main_frame.setLocation(initial_placement_x,initial_placement_y);

		if(always_on_top)
		{
			main_frame.setAlwaysOnTop(true); 
		}
		if(hide_window_decoration)
		{
			main_frame.setUndecorated(true); 
			main_frame.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
		}

		if(start_iconified)
		{
			main_frame.setState(JFrame.ICONIFIED);
		}

		main_frame.setVisible(true);
		set_status("Starting up...");

		try
		{
			init_osc_server(local_osc_port,remote_osc_host,remote_osc_port);
			start_poll_thread();
			set_status("OSC server started");

			//send to self
			OSCMessage msg_startup=new OSCMessage("/OSCGui/startup");
			portOut.setTarget(InetAddress.getByName("127.0.0.1"),local_osc_port);
			portOut.send(msg_startup);
		}
		catch(Exception e)
		{
			e("Error: "+e);
			set_status("Error: could not start OSC server on port "+local_osc_port);
			try{Thread.sleep(3000);}catch(Exception e1){}
			System.exit(1);
		}

		start_status_clear_timeout_thread();

		if(send_after_program_start)
		{
			action_send();
		}
	}//end OSCGui constructor

//========================================================================
	public void set_status(String s)
	{
		label_status.setText(s);
		last_status_set_millis=System.currentTimeMillis();
	}

//========================================================================
	public void set_last_received_message(String s)
	{
		tf_last_received_message.setText(s);
	}

//========================================================================
	public void toggle_indication_send()
	{
		if(indicate_send_toggle_state==1)
		{
			indicate_send_toggle_state=0;
			panel_indicate_send.setBackground(indicate_color_low);
		}
		else
		{
			indicate_send_toggle_state=1;
			panel_indicate_send.setBackground(indicate_color_high);
		}
	}

//========================================================================
	public void toggle_indication_receive()
	{
		if(indicate_receive_toggle_state==1)
		{
			indicate_receive_toggle_state=0;
			panel_indicate_receive.setBackground(indicate_color_low);
		}
		else
		{
			indicate_receive_toggle_state=1;
			panel_indicate_receive.setBackground(indicate_color_high);
		}
	}

//========================================================================
	public void setup_gui()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e)
		{}

		main_frame=new JFrame();
		java.awt.Image icon=createImageFromJar("/resources/images/app_icon.png");
		if(icon!=null)
		{
			main_frame.setIconImage(icon);
		}
 		
		//make sure the program exits when the frame closes
		main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		main_frame.setTitle(main_window_title);
		main_frame.setSize(initial_width,initial_height);

		//This will center the JFrame in the middle of the screen
		main_frame.setLocationRelativeTo(null);

		main_frame.setLayout(new BorderLayout());

		label_local_port=new JLabel("Local Port (UDP):");
		label_remote_host=new JLabel("Remote Host:");
		label_remote_port=new JLabel("Remote Port (UDP):");
		label_repeat=new JLabel("Repeat Message:");
		label_interval_ms=new JLabel("Interval [ms]:");
		label_message_filter_path=new JLabel("Path Filter (In):");
		//label_message_filter_typetag=new JLabel("Typetag Filter:"); 
		label_log_to_file=new JLabel("Log to File:");
		label_send_after_program_start=new JLabel("Send After Start:");


		tf_local_port=new JTextField(""+local_osc_port);
		tf_local_port.setEditable(false);
		tf_remote_host=new JTextField(""+remote_osc_host);
		tf_remote_port=new JTextField(""+remote_osc_port);
		cb_repeat=new JCheckBox("");
		cb_repeat.setSelected(poll_forever);
		tf_interval_ms=new JTextField(""+poll_thread_sleep_count);
		tf_message_filter_path=new JTextField(""+message_filter_path);
		tf_message_filter_path.setEditable(false);
		JCheckBox cb_log_to_file=new JCheckBox("");
		cb_log_to_file.setSelected(log_to_file);
		cb_log_to_file.setEnabled(false);
		JCheckBox cb_send_after_program_start=new JCheckBox("");
		cb_send_after_program_start.setEnabled(false);
		cb_send_after_program_start.setSelected(send_after_program_start);
		cb_send_after_program_start.setEnabled(false);

		panel_form_outter=new JPanel();
		panel_form_outter.setLayout(new BorderLayout());

		panel_form=new JPanel();
		panel_form.setLayout(new GridLayout(4,4)); //rows, columns

		panel_form.add(label_local_port);
		panel_form.add(tf_local_port);
			panel_form.add(label_message_filter_path);
			panel_form.add(tf_message_filter_path);

		panel_form.add(label_remote_host);
		panel_form.add(tf_remote_host);
			panel_form.add(label_log_to_file);
			panel_form.add(cb_log_to_file);

		panel_form.add(label_remote_port);
		panel_form.add(tf_remote_port);
			panel_form.add(label_send_after_program_start);
			panel_form.add(cb_send_after_program_start);

		panel_form.add(label_repeat);
		panel_form.add(cb_repeat);
			panel_form.add(label_interval_ms);
			panel_form.add(tf_interval_ms);

		panel_form_long=new JPanel();
		panel_form_long.setLayout(new GridLayout(4,1)); //rows, columns

		label_log_file_uri=new JLabel("Logfile URI:");
		label_message_string=new JLabel("Message String:");

		JTextField tf_log_file_uri=new JTextField(log_file_uri);
		tf_log_file_uri.setEditable(false);
		tf_message_string=new JTextField(""+message_string);

		panel_form_long.add(label_log_file_uri);
		panel_form_long.add(tf_log_file_uri);

		panel_form_long.add(label_message_string);
		panel_form_long.add(tf_message_string);

		panel_form_outter.add(panel_form,BorderLayout.CENTER);
		panel_form_outter.add(panel_form_long,BorderLayout.SOUTH);

		panel_buttons=new JPanel();
		panel_buttons.setLayout(new GridLayout(1,1)); //rows, columns

		button_send=new JButton("Send");

		panel_buttons.add(button_send);

		panel_comm=new JPanel();
		panel_comm.setLayout(new GridLayout(1,2)); //rows, columns

		panel_indicate_send=new JPanel();
		panel_indicate_receive=new JPanel();
		panel_indicate_send.setBackground(indicate_color_low);
		panel_indicate_receive.setBackground(indicate_color_low);

		panel_comm.add(panel_indicate_send);
		panel_comm.add(panel_indicate_receive);

		panel_south=new JPanel();
		panel_south.setLayout(new GridLayout(4,1)); //rows, columns
	
		panel_south.add(panel_buttons);
		panel_south.add(panel_comm);

		label_status=new JLabel("");
		tf_last_received_message=new JTextField("");
		tf_last_received_message.setEditable(false);

		panel_south.add(tf_last_received_message);
		panel_south.add(label_status);

		main_frame.add(panel_form_outter,BorderLayout.CENTER);
		main_frame.add(panel_south,BorderLayout.SOUTH);

		//hitting enter triggers send button
		main_frame.getRootPane().setDefaultButton(button_send);
	}//end setup_gui()

//========================================================================
	public void action_send()
	{
		try
		{
			portOut.setTarget(InetAddress.getByName(
				tf_remote_host.getText().trim())
				,Integer.parseInt(tf_remote_port.getText().trim())
			);

			//parse to osc message from string
			msg_send=parseOSCMessageFromCSVString(tf_message_string.getText().trim());

			poll_forever=cb_repeat.isSelected();
			if(poll_forever) //if send in loop
			{
				//set interval used in poll thread
				poll_thread_sleep_count=Integer.parseInt(tf_interval_ms.getText());
			}
			else //send message once
			{
				portOut.send(msg_send);
				set_status("Message sent");
				toggle_indication_send();
			}
		}
		catch(Exception e)
		{
			e("Error: "+e);
			set_status("Error: could not send message");
		}
	}

//========================================================================
	public void create_action_listeners()
	{
		button_send.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				action_send();
			}
		});
	}

//========================================================================
	public void init_osc_server(int local_port, String remote_host, int remote_port) throws Exception
	{
		DatagramSocket ds=new DatagramSocket(local_port);
		portIn=new OSCPortIn(ds);

		portOut=new OSCPortOut(InetAddress.getByName(remote_host), remote_port, ds);

		//match any message
		portIn.addListener(message_filter_path, new IncomingMessageListener());
		portIn.startListening();

		p("OSC server started on local port "+local_port);
		p("Filtering incoming messages for pattern '"+message_filter_path+"'");
	}

//=============================================================================
	public void start_poll_thread()
	{
		thread_do_exit=0;
		try
		{
			Thread t=new Thread()
			{
				public void run()
				{
					while(thread_do_exit!=1)
					{
						try
						{
							if(portOut!=null && msg_send!=null && poll_forever)
							{
								portOut.send(msg_send);
								set_status("Message sent");
								toggle_indication_send();
								try{Thread.sleep(poll_thread_sleep_count);}catch(Exception e){}
								continue;
							}
						}
						catch(Exception e)
						{
							e("Error: "+e);

						}
						try{Thread.sleep(10);}catch(Exception e2){}
					}
				}
			};
			t.start();
		}
		catch(Exception e)
		{
			e("Error: "+e);
		}
	}//end start_poll_thread()

//=============================================================================
	public void start_status_clear_timeout_thread()
	{
		thread_do_exit=0;
		try
		{
			Thread t=new Thread()
			{
				public void run()
				{
					while(thread_do_exit!=1)
					{
						try
						{
							//set back status after 1 sec of inactivity to ready
							if(System.currentTimeMillis()
								-last_status_set_millis > 1000)
							{
								set_status("Ready");
							}
							Thread.sleep(200);
						}
						catch(Exception e)
						{
							e("Error: "+e);
						}
					}
				}
			};
			t.start();
		}
		catch(Exception e)
		{
			e("Error: "+e);
		}
	}//end start_status_clear_timeout_thread()

//========================================================================
	public Image createImageFromJar(String imageUriInJar)
	{
		InputStream is;
		Image ii;
		try
		{
			is=OSCGui.class.getResourceAsStream(imageUriInJar);
			ii=ImageIO.read(is);
			is.close();
		}
		catch(Exception e)
		{
			e("Could not load built-in image. "+e.getMessage());
			return null;
		}
		return ii;
	}//end createImageIconFromJar

//========================================================================
	public boolean loadProps(String configfile_uri)
	{
		propertiesFileUri=configfile_uri;
		return LProps.load(propertiesFileUri,this);
	}
//========================================================================
	public void p(String s)
	{
		System.out.println(s);
	}

//========================================================================
	public void e(String s)
	{
		System.err.println(s);
	}

//========================================================================
	public OSCMessage parseOSCMessageFromCSVString(String oscstring) throws Exception
	{
		// !!! csv_separator / ';' can't be part of string content
		String[] args=oscstring.split(csv_separator);

		//parse args: path, typetags, args...
		//minimum #args: 1
		//if typetag contained: min #args 3
		if(args.length<1 || args.length==2)
		{
			throw new Exception("Error: syntax: <path>;(<typetags>;<args>;...)");
		}

		try
		{
			String path=args[0];

			String typetags="";
			Vector msg_args=new Vector();
		
			if(args.length>2)
			{
				typetags=args[1].trim();

				if(typetags.length()!=args.length-2)
				{
					throw new Exception("Error: typetags length does not match # of args");
				}

				for(int i=0;i<(args.length-2);i++)
				{
					String type=typetags.substring(i,i+1);
					if(type.equals("s"))
					{
						msg_args.add(args[(i+2)]);
					}
					else if(type.equals("i"))
					{
						msg_args.add(new Integer( Integer.parseInt( args[(i+2)] ) ));
					}
					else if(type.equals("f"))
					{
						msg_args.add(new Float( Float.parseFloat( args[(i+2)] ) ));
					}
					else
					{
						e("Type '"+type+"' not supported! (just 's', 'i' and 'f' for now).");
					}
				}
			}//end if args length >2

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
		catch(Exception e)
		{
			throw new Exception("Something went wrong parsing the message\n"+e.getMessage());
		}
	}//end parseOSCMessageFromCSVString()

//========================================================================
	public void addShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				e("Terminate signal received");
				//send to self
				OSCMessage msg_shutdown=new OSCMessage("/OSCGui/shutdown");
				try
				{
					portOut.setTarget(InetAddress.getByName("127.0.0.1"),local_osc_port);
					portOut.send(msg_shutdown);
					Thread.sleep(50);
				}catch(Exception e){}
				if(portIn!=null)
				{
					portIn.stopListening();
					portIn.close();
				}
				if(portOut!=null)
				{
					portOut.close();
				}
				if(printwriter!=null)
				{
					printwriter.flush();
					printwriter.close();
				}
				e("Bye");
			}
		});
	}

//inner class
//========================================================================
//========================================================================
class IncomingMessageListener implements OSCListener
{
//========================================================================
	public void accept(OSCMessage msg)
	{
		long millis_now=DTime.nowMillis();
		String path=msg.getAddress();
		java.util.List<Object> args=msg.getArguments();
		int argsSize=args.size();
		StringBuffer pretty=new StringBuffer();

		try
		{
			//  /127.0.0.1:47703
			String rhost=msg.getRemoteHost();
			if(rhost.startsWith("/"))
			{
				rhost=rhost.substring(1,rhost.length());
			}
			pretty.append(rhost+":"+msg.getRemotePort());
			pretty.append(csv_separator+path);
			if(argsSize>0)
			{
				//pretty.append(" ("+argsSize+" args)");
				pretty.append(csv_separator+msg.getTypetagString());
				for(int i=0;i<argsSize;i++)
				{
/*
					if(msg.getTypetagString().substring(i,i+1).equals("s"))
					{
						pretty.append(" \""+args.get(i)+"\"");
					}
					else
					{
						pretty.append(" "+args.get(i));
					}
*/
					pretty.append(csv_separator+args.get(i));
				}
			}
			//p(pretty);

			set_status("Message received");
			set_last_received_message(pretty.toString());

			if(log_to_file && printwriter!=null)
			{
				printwriter.println(millis_now+csv_separator
					+DTime.dateTimeFromMillis(millis_now)+csv_separator
					+pretty.toString());
				printwriter.flush();
			}

			toggle_indication_receive();

			//reply to requester
			//portOut.setTarget( InetAddress.getByName(msg.getRemoteHost()), msg.getRemotePort() );
		}//end try
		catch(Exception e)
		{
			e.printStackTrace();
		}

		if(path.equals("/oscgui/quit") && argsSize==0)
		{
			System.exit(0);
		}
	}//end accpept()

//========================================================================
	public void acceptMessage(Date time,OSCMessage msg) 
	{
		accept(msg);
	}
}//end inner class IncomingMessageListener
}//end class OSCGui
//EOF
