class Main
{
	public static void main(String[] args)
	{
		System.out.println("This jar file can be used as a library (in the Java classpath) that provides com.illposed.osc.*.\n");
		System.out.println("However it also contains classes with a main method");
		System.out.println("(such as this help text, used as default main class when started with java -jar).\n");
		System.out.println("Use oscsend: java -cp <path to this jar> oscsend <oscsend args> ...\n");
		System.out.println("Use oscdump: java -cp <path to this jar> oscdump <oscdump args> ...\n");

		//default to start
		new OSCGui(args);
	}
}
