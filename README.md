# Asynchronous-Multicast-System-in-Java

```markdown
# Programming Projects 3: Persistent and Asynchronous Multicast System

## About The Project

This project involves the implementation of a persistent, asynchronous, and temporal multicast system.

### Persistence

Messages are stored in a text file for each participant, with the coordinator responsible for maintaining messages for a specified duration (td) to ensure delivery to all participants.

### Asynchronous

The message sender is not blocked upon sending a message request and resumes operation after receiving the coordinator's acknowledgement.

### Temporally-bound Persistence

In this model, if a participant is offline for longer than a specified time threshold (td), upon reconnecting, they will receive messages that were sent in the past td seconds. Messages sent before td will be permanently lost to the participant. Participants disconnected for less than or equal to td seconds will not lose any messages.

> **Note:** This project implements only a subset of multicast functionalities.

## About The Repository

The repository contains two separate folders for the project, with distinct participant and coordinator codes within each project.

### Coding Language

The project is implemented in Java (version 11.0.10).

### Pre-requisites

1. Java environment (OpenJDK)

## How to Run

### Run Coordinator

Open a terminal in the Server directory and enter the following commands:

```bash
javac Coordinator.java
java Coordinator coordinator-conf.txt
```

The server opens a socket and is ready for connection!

### Run Participant

Open a terminal on the separate systems in the Client directory and enter the following commands:

```bash
javac Participant.java
java Participant participant-conf.txt
```

Configuration files are included in the repository.

## Implemented Functionalities

1. **Register**: Participants must register with the coordinator, specifying their ID, IP address, and port number.
    - `register [portnumber]`
2. **Deregister**: Participants indicate to the coordinator that they no longer belong to the multicast group.
    - `deregister`
3. **Disconnect**: Participants indicate to the coordinator that they are temporarily going offline.
    - `disconnect`
4. **Reconnect**: Participants indicate to the coordinator that they are online again and specify their IP address and port number.
    - `reconnect [portnumber]`
5. **Multicast Send**: Multicast a message to all current members. Note that the message is an alphanumeric string (e.g., UGACSRocks).
    - `msend [message]`

All received messages at the participant are stored in a text file specified in the configuration file.

## Assumptions

1. Configuration files must be available in their respective directories and written in the standard format as described [here](https://cobweb.cs.uga.edu/~laks/DCS-2016-Sp/pp3/PP3-participant-conf.txt).
2. No more than 10 participants are expected in this system.
3. Messages are only stored at the coordinator when it is live (not stored on an external file for persistence).
4. Participants are only allowed to input a port number from a valid port range.

## Honor Pledge for Project

This project was done in its entirety by **Aditya Malode** and **Yash Joshi**. We hereby state that we have not received unauthorized help of any form.

### Members

1. Aditya Malode
2. Yash Joshi
```

This structured README.md format is designed for clarity and easy navigation within your GitHub repository, making it straightforward for others to understand the purpose, structure, and usage of your project.
