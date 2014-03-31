import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.sql.Date;
import java.util.Scanner;



public class RTTMeasure {
	public static void main(String[] args) throws UnknownHostException, IOException {
		System.out.println("Insert name for log file:");
		Scanner terminalInput = new Scanner(System.in);
		String logName = terminalInput.nextLine();
		int failedPings = 0;
		
		InetAddress inet = InetAddress.getByAddress(new byte[] { 8, 8, 8, 8 });
		
		long rtts[];
		long times[];
		//first reads existing log of RTTs and times
		rtts = readRtts(logName);
		times = readTimes(logName);
		
		//the read methods fill not-yet-sampled values with -1
		//so find the first -1 and start from there
		int i = 0;
		while(rtts[i] != -1){
			i++;
		}
		
		long currTime, sendTime, receiveTime, lastPing = 0, lastPrint = 0;
		//now take samples every ten seconds
		//up to i = 9000
		while(i < 9000 && failedPings < 100){
			currTime = System.currentTimeMillis();
			if((currTime - lastPing) > 10000){ //if it has been 10 seconds
				//attempt a ping
				sendTime = System.nanoTime();
				if(inet.isReachable(3000)){
					//if ping is succesful, record time sent and rtt
					receiveTime = System.nanoTime();
					rtts[i] = (receiveTime - sendTime)/1000000; //divide by 1000000 for milliseconds
					times[i] = currTime;
					int hours = (int) ((times[i] - times[0]) / (60 * 60 * 1000));
					int minutes = (int) ((times[i] - times[0]) / (60 * 1000)) % 60;
					int seconds = (int) ((times[i] - times[0])/ 1000) % 60;
					System.out.println("Ping success! i = " + i + ", rtt = " + rtts[i] + " ms , time = " + 
										hours + ":" + minutes + ":" + seconds);
					i++; //also increment i
					lastPing = currTime; //and reset the ping timer
				}else{
					System.out.println("WARNING: Ping failed!");
					failedPings++;
					lastPing = currTime;
				}
			}
			if((currTime - lastPrint) > 30000){ //if it has been 30 seconds
				//print the current data to the terminal
				System.out.println("----------------------------DATA AT " + currTime + "----------------------------");
				for(int j = 0; j < i; j++){
					int hours = (int) ((times[j] - times[0]) / (60 * 60 * 1000));
					int minutes = (int) ((times[j] - times[0]) / (60 * 1000)) % 60;
					int seconds = (int) ((times[j] - times[0])/ 1000) % 60;
					System.out.println(	hours + ":" + minutes + ":" +
										seconds + ": " + rtts[j] + " ms");
				}
				System.out.println("-----------------------------------------------------------------------------");
				//also update the log file
				writeLog(rtts, times, logName);
				lastPrint = currTime;
			}
		}
		
		
	}
	
	
	
	public static long readRtts(String logName)[] throws IOException{
		long rtts[] = new long[9000];
		String currLine = null;
		String parts[] = null;
		int i = 0;
		File file = (new File(logName));
		if(!file.canRead()){
			//if it cannot read the file, then create the file
			//leave i as 0
			
		}else{
			//if it can read the file, then begin inserting values into rtts[i]
			BufferedReader log = new BufferedReader(new FileReader(file));
			currLine = log.readLine();
			while(currLine != null){
				parts = currLine.split(" ");
				rtts[i] = Long.parseLong(parts[0]);
				i++;
				currLine = log.readLine();
			}
			log.close();
		}
		
		while(i < 9000){
			//make the rest -1
			rtts[i] = -1;
			i++;
		}
		return rtts;
	}
	
	public static long readTimes(String logName)[] throws IOException{
		long times[] = new long[9000];
		int i = 0;
		File file = (new File(logName));
		String currLine = null;
		String parts[] = null;
		if(!file.canRead()){
			//if it cannot read the file, then create the file
			//leave i as 0
			file.createNewFile();
		}else{
			//if it can read the file, then begin inserting values into times[i]
			BufferedReader log = new BufferedReader(new FileReader(file));
			currLine = log.readLine();
			while(currLine != null){
				parts = currLine.split(" ");
				times[i] = Long.parseLong(parts[1]); //times is stored as the second number on each line
				//increment i and move to the next line
				i++; 
				currLine = log.readLine();
			}
			log.close();
			
		}
		while(i < 9000){
			//fill the rest with -1
			//i is either 0 or the number of lines already saved in the log
			//if the log was full, i = 9000, and this is never executed
			times[i] = -1;
			i++;
		}
		
		return times;
	}
	
	public static void writeLog(long rtts[], long times[], String logName) throws IOException{
		File file = new File(logName);
		file.delete(); //delete the old log file, to be replaced
		file.createNewFile();
		BufferedWriter log = new BufferedWriter(new FileWriter(file));
		int i = 0;
		while(i < 9000 && rtts[i] != -1.0 && times[i] != -1.0){
			String currLine = rtts[i] + " " + times[i] + "\n";
			log.write(currLine);
			i++;
		}
		log.close();
	}
}
