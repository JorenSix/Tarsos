package be.tarsos;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.LinkedList;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

public class OSCTest {
	
	private static String ip = "127.0.0.1";
	private static int port = 3000;
	
	private static Runnable oscWriter = new Runnable(){

		@Override
		public void run() {
			while(true){
			 try {
				InetAddress address = InetAddress.getByName(ip);
				OSCPortOut sender = new OSCPortOut(address,port );
				Collection<Object> args = new LinkedList<Object>();
				args.add(new Integer(2));
				args.add(new Integer(2));
				args.add(new Integer(4));
				args.add(new Integer(8));
				OSCMessage msg = new OSCMessage("/sayhello", args);
				sender.send(msg);
				Thread.sleep(300);
			 } catch (Exception e) {
				 e.printStackTrace();
			 }
			}
		}};
	
	public static void main(String... args) throws SocketException, InterruptedException{
		new Thread(oscWriter,"OSC writer").start();;
		//read osc messages;
		
		OSCPortIn receiver = new OSCPortIn(port);
		OSCListener listener = new OSCListener() {
			public void acceptMessage(java.util.Date time, OSCMessage message) {
				System.out.println("/sayhello");
				Collection<Object> args = message.getArguments();
				for(Object arg : args){
					System.out.println("\t" + arg.toString());
				}
				System.out.println();
			}
		};
		receiver.addListener("/sayhello", listener);
		receiver.startListening();
	}

}
