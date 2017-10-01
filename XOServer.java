import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.ServerSocket;

public class XOServer {
	//Порт за връзка със сървъра
	  static int port = 1234;
	  @SuppressWarnings("resource")
	public static void main(String [] args){

		// Стартираме сървъра
	    ServerSocket servSock;
	    try{
	      servSock = new ServerSocket(port);
	    }
	    catch(IOException e){
	      System.err.println("Can't start server");
	      return;
	    }
	    System.out.println("Server started");

	    
	    int lastGameID = 1;

	    while(true){
	      try{
	    	// Приемаме двама нови клиенти
	        Socket XUser = servSock.accept();
	        System.out.println("User X connected");
	        Socket OUser = servSock.accept();
	        System.out.println("User O connected");
	        // Стартираме игра за тях
	        Thread t = new Thread(new XOGame(lastGameID, XUser, OUser));
	        System.out.println("Game id "+lastGameID+" started");
	        t.start();
	        lastGameID++;
	      }
	      catch(IOException e){
	        System.err.println("ERR: Lost connection with user: "+e.getMessage());
	      }
	    }
	  }
	
}

interface XOInterface{
	  // проверява дали играч 1 е победил
	  public boolean hasXWon();
	  // проверява дали играч 2 е победил
	  public boolean hasOWon();
	  //добавя X на позиция i,j в масива. Връща false при невъзможно поставяне
	  public Boolean addX(int i, int j);
	  // добавя O на позиция i,j в масива. Връща false при невъзможно поставяне
	  public Boolean addO(int i, int j);
	}


class XOGame implements Runnable, XOInterface{
	  int id;                // id на играта
	  DataInputStream Xin;   // за получаване от X
	  DataInputStream Oin;   // за получаване от O
	  DataOutputStream Xout; // за изпращане до X
	  DataOutputStream Oout; // за изпращане до O
	  char[][] board;        // XO таблото
	  byte Xfailures;        // брой грешки на X
	  byte Ofailures;        // брой грешки на O
	  
	  
	  public XOGame(int id, Socket X, Socket O) throws IOException{
		    this.id = id;
		    this.Xin = new DataInputStream(X.getInputStream());
		    this.Oin = new DataInputStream(O.getInputStream());
		    this.Xout = new DataOutputStream(X.getOutputStream());
		    this.Oout = new DataOutputStream(O.getOutputStream());
		    this.board = new char[3][3];
		    Xfailures = 0;
		    Ofailures = 0;
		  }

		  public void run(){
		   
		    try{
		      // Известяваме играчите кой с какво играе
		      Xout.writeChar('X');
		      Oout.writeChar('O');
		      // Променлива обозначаваща край на играта
		      boolean hasWon = false;
		   // В XO има максимум 9 хода
		      for(int i=0; i<9; i++){
		    	// при четен ход играе X
		        if(i%2 == 0){
		        	// казваме на O да чака
		          this.sendTo('O', "SRV: Please wait until X makes his move");
		          // изпращаме текущото табло на X
		          this.sendGameStatusTo('X');
		          // взимаме хода на Х
		          this.getXMove();
		          // проверяваме дали X печели
		          hasWon = this.hasXWon();
		          if(hasWon){
		        	// уведомяваме играчите, че има победител
		            // и прекратяваме играта
		            notifyWin('X');
		            System.out.println("Game "+this.id+" ended");
		            return;
		          }
		       // отново изпращаме статуса на играта на Х
		          this.sendGameStatusTo('X');
		        }      
		     // при нечетен ход играе O - повтаря операциите от горе
		        else{
		          this.sendTo('X', "SRV: Please wait until O makes his move");
		          this.sendGameStatusTo('O');
		          this.getOMove();
		          hasWon = this.hasOWon();
		          if(hasWon){
		            notifyWin('O');
		            System.out.println("Game "+this.id+" ended");
		            return;
		          }
		          this.sendGameStatusTo('O');
		        }
		      }
		   // Направени са 9 хода и няма победител
		      sendTo('X', "Draw game");
		      sendTo('O', "Draw game");
		    }
		    // В този catch ще се влезе при непредвидена грешка
		    // например connection reset by peer и подобни
		    catch(IOException userConnectionLost){
		      try{
		        sendTo('X', "SRV: Your opponent connection is lost");
		      }
		      catch(IOException e){}
		      try{
		        sendTo('O', "SRV: Your opponent connection is lost");
		      }
		      catch(IOException e){}
		    }
		    // Тук ще се влезе при предвидена грешка
		    catch(Exception otherError){
		      try{
		        sendTo('X', "SRV: " +otherError.getMessage());
		      }
		      catch(IOException e){}
		      try{
		        sendTo('O', "SRV: " +otherError.getMessage());
		      }
		      catch(IOException e){}
		    }
		   // Ако има незатворена връзка, затваряме я
		    finally{
		      try{
		        if(Xin!=null) Xin.close();
		        if(Xout!=null) Xout.close();
		        if(Oin!=null) Oin.close();
		        if(Oout!=null) Oout.close();
		      }
		      catch(IOException e2){}
		    }
		    System.out.println("Game "+this.id+" ended");
		  }

		// Метод за изпращане на текстови низ до играч
		  private void sendTo(char c, String msg) throws IOException{
		    if(c=='X' && this.Xout!=null){
		      this.Xout.writeUTF(msg);
		    }
		    else if(this.Oout!=null){
		      this.Oout.writeUTF(msg);
		    }
		  }

		// Метод за уведомяване на играчите кой е победителя
		  private void notifyWin(char c) throws IOException{
		      sendTo('X', "SRV: "+c+" wins");
		      sendTo('O', "SRV: "+c+" wins");
		  }

		// Взима ход от Х
		  private void getXMove() throws Exception{
		    getMove('X', this.Xin, this.Xout);
		  }

		// Взима ход от O
		  private void getOMove() throws Exception{
		    getMove('O', this.Oin, this.Oout);
		  }

		  // Да взимане на ход от играч
		  // "c" е със стойности X или O и указва за кой играч се взима хода
		  private void getMove(char c, DataInputStream in, DataOutputStream out) 
		    throws Exception{
		    
		    try{
		      
		      boolean correctMove = false;
		      do{
		    	// Подканваме играча да изпрати хода си
		        sendTo(c, "SRV: Send your row and column for move");

		        // Прочитаме неговия ход
		        int i = in.readInt();
		        int j = in.readInt();

		        // Опитваме се да добавим знака на съответната позиция
		        // Ако хода е невалинен, add функцията ще върне false
		        if(c == 'X') correctMove = this.addX(i,j);
		        else correctMove = this.addO(i,j);

		        // Ако хода не е коректен, правим проверка за брой грешки
		        if(!correctMove){
		        	// Ако грешките са прекалено много, гоним играча
		          if((c=='X' && this.Xfailures == 2) || 
		             (c=='O' && this.Ofailures == 2)){
		            sendTo(c, "SRV: Sorry, too much wrong moves, you lose");
		            if(c == 'X'){
		              sendTo('O', "SRV: Your opponent made too many mistakes");
		              throw new Exception("O wins");
		            }
		            else{
		              sendTo('X', "SRV: Your opponent made too many mistakes");
		              throw new Exception("X wins");
		            }
		          }
		          
		          else{
		            sendTo(c, "SRV: Illegal move");
		            if(c=='X') this.Xfailures++;
		            else this.Ofailures++;
		          }
		        }
		      }
		      while(!correctMove);
		    }
		    
		    catch(IOException e){
		      if(c=='X'){
		        sendTo('O', "SRV: Your opponent is gone");
		        throw new Exception("O wins");
		      }
		      else{
		        sendTo('X', "SRV: Your opponent is gone");
		        throw new Exception("X wins");
		      }        
		    }
		  }

		// Проверява дали X печели
		  public boolean hasXWon(){
		    return hasWon('X');
		  }

		// Проверява дали O печели
		  public boolean hasOWon(){
		    return hasWon('O');
		  }

		  // Методът за проверка дали играч "c" печели
		  private Boolean hasWon(char c){
		    for(int i=0; i<3; i++){
		      byte elements = 0;
		      for(int j=0; j<3; j++){
		        if(this.board[i][j]==c) elements++;
		      }
		      if(elements==3) return true;
		    }

		    for(int j=0; j<3; j++){
		      byte elements = 0;
		      for(int i=0; i<3; i++){
		        if(this.board[i][j]==c) elements++;
		      }
		      if(elements==3) return true;
		    }

		    if(this.board[1][1]==c){
		      if((this.board[0][0]==c && this.board[2][2]==c)
		           ||
		         (this.board[0][2]==c && this.board[2][0]==c)){
		        return true;
		      }
		    }
		 // ако се стигне до тук, значи няма печеливша позиция
		    return false;
		  }

		// за добавяне на X на позиция (i,j)
		  public Boolean addX(int i, int j){
		    return this.add(i, j, 'X');
		  }

		// за добавяне на O на позиция (i,j)
		  public Boolean addO(int i, int j){
		    return this.add(i, j, 'O');
		  }

		// за добавяне на елемент "c" на позиция (i,j)
		  private Boolean add(int i, int j, char c){
		    if(i<0 || i>2 || j<0 || j>2){
		      return false;
		    }
		    else if(this.board[i][j] == 'X' || this.board[i][j] == 'O'){
		      return false;
		    }
		    else{
		      board[i][j] = c;
		      return true;
		    }
		  }

		  
		  private void sendGameStatusTo(char c) throws IOException{
		    StringBuilder strb = new StringBuilder();
		    for(int i=0; i<3; i++){
		      for(int j=0; j<3; j++){
		        strb.append("| "+this.board[i][j]+" | ");
		      }
		      strb.append("\n");
		    }
		    sendTo(c, strb.toString());    
		  }
		}