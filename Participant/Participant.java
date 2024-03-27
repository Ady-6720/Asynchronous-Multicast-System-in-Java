import java.io.*;
import java.net.*;
import java.nio.CharBuffer;
import javax.lang.model.util.ElementScanner6;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.Port;
import java.text.DecimalFormat;
import java.util.Scanner;

class workerThread extends Thread {

        private Socket socket;
        private int uniqueID;
        private DataInputStream input = null;
        private String fileName;
        private DataOutputStream coordinatorOut = null;
        private DataInputStream coordinatorInput = null;
        private String coordinatorIP;
        receiverThread nreceiverThread;


        workerThread(int id, String fname, String address, int port) {
          coordinatorIP = address; //odin or VCF 
          uniqueID = id; //id for each UNIQUE participant
          fileName = fname; //logg
          

          try {
          //connection to the coordinator.
          socket = new Socket(coordinatorIP, port);
          System.out.println("Connection established on port: " + port);
      
          //user input.
          input = new DataInputStream(System.in);
    
          coordinatorInput = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
      
          coordinatorOut = new DataOutputStream(socket.getOutputStream());
            } catch (UnknownHostException e) {
                System.err.println("Failed to connect to the host: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("I/O error occurred: " + e.getMessage());
            }

  }

  enum Status {
    Registered,
    Deregistered,
    Disconnected,
    Reconnected,
  }

  @Override
  public void run() {
    try {
      // Displaying the thread that is running
        String line = "";
        String command = "";
        Status status = Status.Deregistered;
  
        // keep reading until "quit" is input
        while (line != "quit") {
            System.out.print("COMMAND-->");
            line = input.readLine();
            String error = "";
///////////////////////////////////////////////////
        if (line.indexOf("register ") == 0) {
            System.out.println("Attempting to process 'register' command...");
        
            if (line.split(" ").length != 2) {
                error = "register: missing operand";
                System.err.println(error);
            } else {
                System.out.println("Processing registration...");
        
                if (status.equals(Status.Deregistered)) {
                    int listenerPort = Integer.parseInt(line.split(" ")[1]);
                    System.out.println("Initializing receiver thread on port: " + listenerPort);
                    
                    nreceiverThread = new receiverThread(fileName, listenerPort);
                    InetAddress localhost = InetAddress.getLocalHost();
                    String ipAddress = (localhost.getHostAddress()).trim();
                    
                    System.out.println("Registering with IP address: " + ipAddress + " and port: " + listenerPort);
                    command = "register#" + uniqueID + "#" + ipAddress + "#" + listenerPort;
                } else {
                    error = "Participant is already registered!";
                    System.err.println(error);
                }
            }
        }

///////////////////////////////////////////////////////        
        else if (line.equals("deregister") && !line.contains(" ")) {
            System.out.println("Attempting to process 'deregister' command...");
        
            if (!status.equals(Status.Deregistered)) {
                System.out.println("Preparing to deregister participant with unique ID: " + uniqueID);
                command = line + "#" + uniqueID;
            } else {
                error = "Participant is not registered yet!";
                System.err.println(error);
            }
        }

//debugged       
        else if (line.equals("disconnect") && !line.contains(" ")) {
        if (!status.equals(Status.Disconnected) && !status.equals(Status.Deregistered)) {
            command = line + "#" + uniqueID;
        } else {
            error = "Participant " + uniqueID + " is already disconnected or not registered.";
        }
       }


////////////////////////////////////////////////////      
        else if (line.contains("reconnect ")) {
            System.out.println("Attempting to process 'reconnect' command...");
        
            if (line.split(" ").length != 2) {
                error = "reconnect: missing operand";
                System.err.println(error + ". Correct format: 'reconnect <listenerPort>'");
            } else {
                System.out.println("Validating reconnect request...");
        
                if (status.equals(Status.Disconnected)) {
                    int listenerPort = Integer.parseInt(line.split(" ")[1]);
                    System.out.println("Preparing to reconnect participant with unique ID: " + uniqueID + " on port: " + listenerPort);
        
                    nreceiverThread = new receiverThread(fileName, listenerPort);
                    InetAddress localhost = InetAddress.getLocalHost();
                    String ipAddress = (localhost.getHostAddress()).trim();
        
                    command = "reconnect#" + uniqueID + "#" + ipAddress + "#" + listenerPort;
                    System.out.println("Reconnect command prepared successfully.");
                } else if (status.equals(Status.Deregistered)) {
                    error = "Participant is not registered yet!";
                    System.err.println(error + ". Please register before attempting to reconnect.");
                } else {
                    error = "Participant is already connected!";
                    System.err.println(error + ". No need to reconnect.");
                }
            }
        }

///////////////////////////////////////////////////       
        else if (line.contains("msend ")) {
            System.out.println("Attempting to process 'msend' command...");
        
            if (line.split(" ").length < 2) {
                error = "msend: missing operand";
                System.err.println(error + ". Correct format: 'msend <message>'");
            } else {
                System.out.println("Validating message send request...");
        
                if (status.equals(Status.Registered) || status.equals(Status.Reconnected)) {
                    line = line.replaceFirst(" ", "#");
                    command = line + "#" + uniqueID;
                    System.out.println("Message prepared for sending with unique ID: " + uniqueID);
                } else if (status.equals(Status.Deregistered)) {
                    error = "Participant is not registered yet!";
                    System.err.println(error + ". Please register before attempting to send messages.");
                } else {
                    error = "Participant is not connected!";
                    System.err.println(error + ". Ensure you're connected before sending messages.");
                }
            }
        }
////////////////////////////////////////////////////
        else {
          error = "<---COMMAND NOT RECOGNIZED--->";
        }
        //put error
        if (error.isEmpty()) {
              coordinatorOut.writeUTF(command);
              String response = coordinatorInput.readUTF();
          
              if (response.contains("deregister")) {
                  nreceiverThread.stop();
                  status = Status.Deregistered;
                  System.out.println("Successfully deregistered.");
              } else if (response.contains("disconnect")) {
                  nreceiverThread.stop();
                  status = Status.Disconnected;
                  System.out.println("Successfully disconnected.");
              } else if (response.contains("reconnect")) {
                  status = Status.Reconnected;
                  System.out.println("Successfully reconnected.");
              } else if (response.contains(" register")) {
                  status = Status.Registered;
                  System.out.println("Successfully registered.");
              } else {
                  System.out.println(response);
              }
        } else {
            System.out.println("Error: " + error);
        }

      }

            try {
              if(input != null) input.close();
              if(coordinatorOut != null) coordinatorOut.close();
              if(coordinatorInput != null) coordinatorInput.close();
              if(socket != null) socket.close();
              System.out.println("Connection closed successfully.");
          } catch(IOException e) {
              System.err.println("Error closing resources: " + e.getMessage());
          }
    
    } catch (UnknownHostException e) {
        System.err.println("Host could not be determined: " + e.getMessage());
    } catch (IOException e) {
        System.err.println("I/O error: " + e.getMessage());
    } catch (Exception e) {
        System.err.println("Exception caught: " + e.getMessage());
    }
  }
}

class receiverThread implements Runnable {

    private Socket listenerSocket = null;
    private ServerSocket listener = null;
    private DataOutputStream messageOutput = null;
    
    private FileOutputStream fileOutStream;
    private String fileName;
    private int listenport;
    
    private DataInputStream messageInput = null;
    
    receiverThread(String fname, int port) {
        this.fileName = fname;
        this.listenport = port;
        new Thread(this).start();
    }


  @Override
  public void run() {
      try {
          listener = new ServerSocket(listenport);
          System.out.println("Listener operational, awaiting connection on port: " + listenport + "...");
  
          listenerSocket = listener.accept();
          //System.out.println("Connection established with Coordinator.");
  
          messageInput = new DataInputStream(new BufferedInputStream(listenerSocket.getInputStream()));
          messageOutput = new DataOutputStream(listenerSocket.getOutputStream());
  
          File logFile = new File(fileName);
          if (!logFile.exists()) {
              logFile.createNewFile();
          }
          fileOutStream = new FileOutputStream(logFile, true);
  
          String message = "";
          while (!message.equals("eof")) {
              message = messageInput.readUTF();
              System.out.println("Received message: "+message+"\nCOMMAND-->");
              fileOutStream.write((message + "\n").getBytes());
          }
      } catch (IOException e) {
          //System.err.println("Receiver Thread IO Exception: " + e.getMessage());
      } catch (Exception e) {
          System.err.println("Receiver Thread General Exception: " + e.getMessage());
      } finally {
          try {
              if (messageInput != null) messageInput.close();
              if (messageOutput != null) messageOutput.close();
              if (fileOutStream != null) fileOutStream.close();
              if (listenerSocket != null) listenerSocket.close();
              if (listener != null) listener.close();
          } catch (IOException e) {
              System.err.println("Error closing resources: " + e.getMessage());
          }
      }
  }


  public void stop() {
    try {
      System.out.println("closing.........");
      listenerSocket.close();
      listener.close();
      messageInput.close();
      messageOutput.close();
      fileOutStream.flush();
      fileOutStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

public class Participant {
    public Participant(int id, String logFile, String address, int nPort) {
        System.out.println("Initializing participant with ID: " + id);
        new workerThread(id, logFile, address, nPort).start();
    }

    //available commands
    private static void printAvailableCommands() {
        System.out.println("---------Available Commands---------");
        System.out.println("- register <port>");
        System.out.println("- deregister");
        System.out.println("- disconnect: Temporarily disconnect");
        System.out.println("- reconnect <port>");
        System.out.println("- msend <alphanumeric message>");
        System.out.println("------------------------------------");
    }

    // Method to process configuration file
    private static void processConfigFile(String configFileName) {
        File configFile = new File(configFileName);
        if (configFile.exists() && configFile.isFile()) {
            try (Scanner scanner = new Scanner(configFile)) {
                int uniqueID = Integer.parseInt(scanner.nextLine());
                String logFileName = scanner.nextLine();
                String[] socketInfo = scanner.nextLine().split(" ");
                String coordinatorIP = socketInfo[0];
                int coordinatorListenPort = Integer.parseInt(socketInfo[1]);

                new Participant(uniqueID, logFileName, coordinatorIP, coordinatorListenPort);
            } catch (IOException e) {
                System.err.println("Error reading configuration file: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format in configuration file: " + e.getMessage());
            }
        } else {
            System.err.println("Configuration file " + configFileName + " not found.");
        }
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            printAvailableCommands();
            processConfigFile(args[0]);
        } else {
            System.err.println("Error: Missing argument. Please provide the configuration file name.");
        }
    }
}
