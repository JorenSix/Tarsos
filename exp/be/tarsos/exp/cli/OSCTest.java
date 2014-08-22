package be.tarsos.exp.cli;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

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
			int counter = 0;
			Random r = new Random();
			while(true){
			 try {
				InetAddress address = InetAddress.getByName(ip);
				OSCPortOut sender = new OSCPortOut(address,port );
				
				Collection<Object> args = new LinkedList<Object>();
				for(int i = 0 ; i<r.nextInt(8) ; i ++ ){
					args.add(new Integer(counter++));
				}
				OSCMessage msg = new OSCMessage("/sayhello", args);
				
				System.out.println("Sending to /sayhello");
				for(Object arg : args){
					System.out.println("\t" + arg.toString());
				}
				System.out.println();
				sender.send(msg);
				Thread.sleep(1000);
			 } catch (Exception e) {
				 e.printStackTrace();
			 }
			}
		}};
	
	public static void main(String... args) throws SocketException, InterruptedException{
		new Thread(oscWriter,"OSC writer").run();;
		//read osc messages;
		
		OSCPortIn receiver = new OSCPortIn(port);
		OSCListener listener = new OSCListener() {
			public void acceptMessage(java.util.Date time, OSCMessage message) {
				System.out.println("Receiving on /sayhello");
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
