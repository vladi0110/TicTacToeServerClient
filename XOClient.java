import java.util.Scanner;
import java.io.IOException;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;

public class XOClient {

	  final static String host = "localhost";
	  final static int port = 1234;
	  public static void main(String[] args){
	    System.out.println("Connecting to server... ");
	    Socket s;
	    DataInputStream in;
	    DataOutputStream out;
	    @SuppressWarnings("resource")
		Scanner keybIn = new Scanner(System.in);
	    try{
	      s = new Socket(host, port);
	      in = new DataInputStream(s.getInputStream());
	      out = new DataOutputStream(s.getOutputStream());
	    }
	    catch(IOException e){
	      System.out.println("Cannot connect");
	      return;
	    }
	    System.out.println("done! Waiting for opponent...");

	    // Ще записваме съобщенията от сървъра тук
	    String msgFromServer;
	    
	    int indexToSend;

	    try{
	      
	      System.out.println("I play with: "+in.readChar());

	      while(true){
	        // Чета съобщението на сървъра
	        msgFromServer = in.readUTF();
	        // Отпечатвам го на екрана
	        System.out.println(msgFromServer);

	        if(msgFromServer.equals("SRV: X wins") || 
	           msgFromServer.equals("SRV: O wins") ||
	           msgFromServer.equals("SRV: Draw game")){
	          break;
	        }
	        else if(msgFromServer.equals("SRV: Send your row and column for move")){
	          System.out.print("Enter row: ");
	          indexToSend = keybIn.nextInt();
	          out.writeInt(indexToSend);
	          System.out.print("Enter column: ");
	          indexToSend = keybIn.nextInt();
	          out.writeInt(indexToSend);
	          System.out.println("I will wait for my opponent to move");
	        }
	      }
	    }
	    catch(IOException e){
	      System.out.println("Connection lost");
	    }
	    finally{
	      try{
	        if(in!=null) in.close();
	        if(out!=null) out.close();
	        if(s!=null) s.close();
	      }
	      catch(IOException e2){}
	    } 
	  }
	}
