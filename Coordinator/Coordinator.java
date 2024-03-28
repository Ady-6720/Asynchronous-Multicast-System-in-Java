import java.io.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;


class Globals {

  public static final Map<Integer, Socket> participantMap = new HashMap<>();
  public static final List<Integer> activeParticipants = new ArrayList<>();
  public static final Map<Long, String> messageMap = new TreeMap<>();
  public static final Map<Long, List<Integer>> nonMessageRecipients = new TreeMap<>();
}

class connectionThread extends Thread {

    private int port;
    private int timeout;
    connectionThread(int port, int timeout) {
        this.port = port;
        this.timeout = timeout;
        System.out.println("Connection thread initialized with port: " + port + " and timeout: " + timeout + "ms.");
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server listening on port: " + port);

            // Continuously accepting new connections unless interrupted
            while (!Thread.interrupted()) {
                System.out.println("Awaiting new connection...");

                try {
                    Socket socket = server.accept();
                    System.out.println("Connection established with a new participant.");

                    // Setting up data streams for communication
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    // Starting a new thread for handling this participant
                    new workerThread(in, out, timeout).start();
                } catch (IOException e) {
                    System.err.println("Connection attempt failed: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server could not start on port " + port + ": " + e.getMessage());
        }
    }
}



class messageSender extends Thread {

  private String message;
  private Integer senderID;

  messageSender(String message, Integer senderID) {
    this.message = message;
    this.senderID = senderID;
  }
  //---------------------------------------------------------------------------done ---------------------------------------------------
  @Override
  public void run() {
    Globals.activeParticipants.forEach(participantID -> {
      if (!participantID.equals(senderID)) {
        try {
          Socket socket = Globals.participantMap.get(participantID);
          if (socket != null && !socket.isClosed()) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(message);
            System.out.println("Sent message to Participant ID: " + participantID);
          }
        } catch (IOException e) {
          System.out.println("Error sending message to Participant ID: " + participantID + ", Error: " + e.getMessage());
        }
      }
    });
    recordMessageForNonActiveParticipants();
  }

  private void recordMessageForNonActiveParticipants() {
    long currentTime = System.currentTimeMillis();
    List<Integer> nonRecipients = new ArrayList<>();
    Globals.participantMap.keySet().stream()
            .filter(participantID -> !Globals.activeParticipants.contains(participantID))
            .forEach(nonRecipients::add);

    if (!nonRecipients.isEmpty()) {
      Globals.messageMap.put(currentTime, message);
      Globals.nonMessageRecipients.put(currentTime, nonRecipients);
      System.out.println("Recorded message for non-active participants: " + nonRecipients);

      // Display the list of inactive participants.
      System.out.println("Inactive participants at " + currentTime + ": " + nonRecipients);
    } else {
      System.out.println("No non-active participants to record message for at " + currentTime);
    }
  }

}


class workerThread extends Thread {

    private int timeout;
    private DataInputStream in;
    private DataOutputStream out;

    workerThread(DataInputStream in, DataOutputStream out, int timeout) {
        this.timeout = timeout;
        this.in = in;
        this.out = out;
    }

    private void removeOldMessages(long time) {
        for (Long messageTime : Globals.messageMap.keySet()) {
            if ((time - messageTime) / 1000 > timeout) {
                Globals.messageMap.remove(messageTime);
                Globals.nonMessageRecipients.remove(messageTime);
            }
        }
    }

    @Override
    public void run() {
        try {
            String input = "";
            String errorMessage = "You ARE NOT REGISTERED!!";
            while (!input.equals("quit")) {
                input = in.readUTF();
                System.out.println("Participant input: " + input.split("#")[0]);
                removeOldMessages(System.currentTimeMillis());

                // Switch cases for command handling
                switch (input.split("#")[0]) {
                    case "register":
                        handleRegisterCommand(input, errorMessage);
                        break;
                    case "deregister":
                        handleDeregisterCommand(input, errorMessage);
                        break;
                    case "reconnect":
                        handleReconnectCommand(input, errorMessage);
                        break;
                    case "disconnect":
                        handleDisconnectCommand(input, errorMessage);
                        break;
                    case "msend":
                        handleMsendCommand(input, errorMessage);
                        break;
                    default:
                        System.out.println("Unknown command received.");
                        break;
                }
            }
        } catch (IOException i) {
            System.out.println("workerThread: Exception is caught" + i.getMessage());
        }
    }

    private void handleRegisterCommand(String input, String errorMessage) throws IOException {
        // Handle register command
        String[] participantInput = input.split("#");
        try {
            Integer participantID = Integer.parseInt(participantInput[1]);
            String participantIP = participantInput[2];
            int participantListenPort = Integer.parseInt(participantInput[3]);

            System.out.println("Attempting to register new participant with ID: " + participantID);

            try {
                Socket socket = new Socket(participantIP, participantListenPort);
                Globals.participantMap.put(participantID, socket);
                Globals.activeParticipants.add(participantID);

                System.out.println("Participant with ID " + participantID + " registered successfully.");
                out.writeUTF("Participant registered with ID " + participantID);
            } catch (IOException e) {
                System.err.println("Failed to register participant with ID " + participantID + ": " + e.getMessage());
                out.writeUTF("Failed to register participant.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid participant ID format: " + e.getMessage());
            out.writeUTF("Invalid registration command format.");
        } catch (IOException e) {
            System.err.println("Error sending registration confirmation: " + e.getMessage());
        }
    }

    private void handleDeregisterCommand(String input, String errorMessage) throws IOException {
        // Handle deregister command
        String[] participantInput = input.split("#");
        try {
            Integer participantID = Integer.parseInt(participantInput[1]);
            System.out.println("Attempting to deregister participant with ID: " + participantID);

            if (Globals.participantMap.containsKey(participantID)) {
                try {
                    if (Globals.participantMap.get(participantID) != null) {
                        Globals.participantMap.get(participantID).close();
                        Globals.activeParticipants.remove(participantID);
                        System.out.println("Participant with ID " + participantID + " successfully deregistered and disconnected.");
                    } else {
                        System.out.println("Participant with ID " + participantID + " is already disconnected.");
                    }
                    Globals.participantMap.remove(participantID);
                    out.writeUTF("Participant deregistered");
                } catch (IOException e) {
                    System.err.println("Failed to close connection for participant with ID " + participantID + ": " + e.getMessage());
                    out.writeUTF("Failed to fully deregister participant.");
                }
            } else {
                System.err.println("Attempt to deregister a non-existing participant with ID: " + participantID);
                out.writeUTF(errorMessage);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid participant ID format for deregistration: " + e.getMessage());
            out.writeUTF("Invalid deregister command format.");
        } catch (IOException e) {
            System.err.println("Error sending deregistration confirmation: " + e.getMessage());
        }
    }

    private void handleReconnectCommand(String input, String errorMessage) throws IOException {
        // Handle reconnect command
        String[] participantInput = input.split("#");
        try {
            Integer participantID = Integer.parseInt(participantInput[1]);
            System.out.println("Attempting to reconnect participant with ID: " + participantID);

            if (Globals.participantMap.containsKey(participantID)) {
                if (!Globals.activeParticipants.contains(participantID)) {
                    String participantIP = participantInput[2];
                    int participantListenPort = Integer.parseInt(participantInput[3]);
                    try {
                        Socket socket = new Socket(participantIP, participantListenPort);
                        Globals.participantMap.put(participantID, socket);
                        Globals.activeParticipants.add(participantID);
                        out.writeUTF("Participant reconnected");
                        System.out.println("Participant with ID " + participantID + " successfully reconnected.");

                        Map<Long, String> copyMessageMap = new TreeMap<>(Globals.messageMap);
                        for (Long messageTime : copyMessageMap.keySet()) {
                            if (Globals.nonMessageRecipients.containsKey(messageTime) &&
                                    Globals.nonMessageRecipients.get(messageTime).contains(participantID)) {
                                try (DataOutputStream resend = new DataOutputStream(socket.getOutputStream())) {
                                    resend.writeUTF(Globals.messageMap.get(messageTime));
                                    System.out.println("Resent old message to participant ID " + participantID);
                                } catch (IOException e) {
                                    System.err.println("Failed to resend message to participant ID " + participantID + ": " + e.getMessage());
                                }
                                Globals.nonMessageRecipients.get(messageTime).remove(participantID);
                                if (Globals.nonMessageRecipients.get(messageTime).isEmpty()) {
                                    Globals.nonMessageRecipients.remove(messageTime);
                                    Globals.messageMap.remove(messageTime);
                                }
                            }
                        }
                        copyMessageMap.clear();
                    } catch (IOException e) {
                        System.err.println("Failed to reconnect participant ID " + participantID + ": " + e.getMessage());
                        out.writeUTF("Failed to reconnect.");
                    }
                } else {
                    System.out.println("Requested Participant ID " + participantID + " is already connected.");
                    out.writeUTF("Requested Participant is already Connected");
                }
            } else {
                System.err.println("Reconnect attempt for non-existing participant ID: " + participantID);
                out.writeUTF(errorMessage);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid participant ID format for reconnecting: " + e.getMessage());
            out.writeUTF("Invalid reconnect command format.");
        } catch (IOException e) {
            System.err.println("Error sending message for reconnect attempt: " + e.getMessage());
        }
    }

        private void handleDisconnectCommand(String input, String errorMessage) throws IOException {
        // Handle disconnect command
        String[] participantInput = input.split("#");
        try {
            Integer participantID = Integer.parseInt(participantInput[1]);
            System.out.println("Attempting to disconnect participant with ID: " + participantID);

            if (Globals.participantMap.containsKey(participantID)) {
                try {
                    Globals.participantMap.get(participantID).close();
                    System.out.println("Participant with ID " + participantID + " has been disconnected.");

                    Globals.participantMap.put(participantID, null);
                    Globals.activeParticipants.remove(participantID);
                    out.writeUTF("Participant disconnected");
                } catch (IOException e) {
                    System.err.println("Error disconnecting participant with ID " + participantID + ": " + e.getMessage());
                    out.writeUTF("Error disconnecting participant.");
                }
            } else {
                System.err.println("No participant found with ID: " + participantID);
                out.writeUTF(errorMessage);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid participant ID format: " + e.getMessage());
            out.writeUTF("Invalid disconnect command format.");
        } catch (IOException e) {
            System.err.println("Error sending message to participant: " + e.getMessage());
        }
    }

    private void handleMsendCommand(String input, String errorMessage) throws IOException {
        // Handle msend command
        String[] participantInput = input.split("#");
        try {
            int senderID = Integer.parseInt(participantInput[2]);
            System.out.println("Received msend request from participant ID: " + senderID);

            if (Globals.participantMap.containsKey(senderID)) {
                if (Globals.activeParticipants.contains(senderID)) {
                    String message = participantInput[1];
                    System.out.println("Sending message from ID " + senderID + ": \"" + message + "\"");
                    new messageSender(message, senderID).start();
                    out.writeUTF("Message Acknowledged");
                } else {
                    System.err.println("msend request from a non-connected participant ID: " + senderID);
                    out.writeUTF("Requested Participant is not Connected");
                }
            } else {
                System.err.println("msend request for a non-existing participant ID: " + senderID);
                out.writeUTF(errorMessage);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid sender ID format in msend request: " + e.getMessage());
            out.writeUTF("Invalid msend command format.");
        } catch (IOException e) {
            System.err.println("Error sending acknowledgement for msend request: " + e.getMessage());
        }
    }
}



//---------------------------------------------------------------------------done---------------------------------------------------
public class Coordinator {

  public Coordinator(int port, int timeout) {
    new connectionThread(port, timeout).start();
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Error: Missing arguments. Usage: java Coordinator <configuration-file-path>");
      return;
    }

    File configFile = new File(args[0]);
    if (!configFile.exists() || !configFile.isFile()) {
      System.out.println("Error: Configuration file not found at " + args[0]);
      return;
    }

    try (Scanner scanner = new Scanner(configFile)) {
      int coordinatorListenPort = Integer.parseInt(scanner.nextLine().trim());
      int messageTimeout = Integer.parseInt(scanner.nextLine().trim());
      new Coordinator(coordinatorListenPort, messageTimeout);
      System.out.println("Coordinator is running...");
    } catch (FileNotFoundException e) {
      System.out.println("Error: Unable to open the configuration file at " + args[0]);
    } catch (NumberFormatException e) {
      System.out.println("Error: Invalid format in configuration file. Ensure it contains port and timeout values.");
    }
  }
}
