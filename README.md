# Asynchronous-Multicast-System-in-Java

# Programming Projects 3: Persistent and Asynchronous Multicast System


### Coding Language

The project is implemented in Java 

## How to Run

### Run Coordinator

Open a terminal in the Server directory and enter the following commands:

```bash
javac Coordinator.java
java Coordinator Cconfig.txt
```

The server opens a socket and is ready for connection!

### Run Participant

Open a terminal on the separate systems in the Client directory and enter the following commands:

```bash
javac Participant.java
java Participant P1config.txt
```

Configuration files are included in the repository.
## Important Note

:warning: **Before running the participant script, ensure you modify the participant configuration files with the correct IP address of the coordinator.** This step is crucial for the successful registration and communication between the participant and the coordinator in the multicast system.

The configuration files are located within each project's Client directory. You'll need to replace the placeholder IP address with the actual IP address of the machine where the Coordinator script is running.

Example of configuration file adjustment:
- Find the line that specifies the coordinator's IP address, typically formatted as `coordinatorIP=<IP_ADDRESS>`.
- Replace `<IP_ADDRESS>` with the actual IP address of your coordinator.

Failure to update the configuration files with the correct IP address may result in participants being unable to connect to the multicast system.

## Implemented Functionalities

1. **Register**: Participants must register with the coordinator, specifying their ID, IP address, and port number.
    - `register [portnumber]`
2. **Deregister**: Participants indicate to the coordinator that they no longer belong to the multicast group.
    - `deregister`
3. **Disconnect**: Participants indicate to the coordinator that they are temporarily going offline.
    - `disconnect`
4. **Reconnect**: Participants indicate to the coordinator that they are online again and specify their IP address and port number.
    - `reconnect [portnumber]`
5. **Multicast Send**: Multicast a message to all current members. Note that the message is an alphanumeric string (e.g., UGACS2024).
    - `msend [message]`

All received messages at the participant are stored in a text file specified in the configuration file.


## Honor Pledge for Project

This project was done in its entirety by **Aditya Malode** and **Yash Joshi**. We hereby state that we have not received unauthorized help of any form.

### Members

1. Aditya Malode
2. Yash Joshi
```


