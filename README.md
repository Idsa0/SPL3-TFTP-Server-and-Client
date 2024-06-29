# Extended TFTP Server and Client

## General Description

This project was given to us as a second year assignment in the course Systems Programming, written by [Idan Saltzman](https://github.com/idsa0) and [Tomer Kandel](https://github.com/teramisuskandel).

This project involves an implementation of an extended TFTP (Trivial File Transfer Protocol) server and client. Unlike the traditional TFTP which uses UDP, this implementation uses TCP to ensure reliable transmission and connection-oriented communication. The TFTP server allows multiple users to upload and download files, and announces when files are added or deleted from the server. 

To enhance security and manageability, the server requires users to log in with a unique username before they can perform any file operations. Communication between the server and clients is done using a binary communication protocol, supporting the upload, download, and lookup of files.

The server is based on the Thread-Per-Client (TPC) model, enabling it to handle multiple client connections simultaneously. This project also involves replacing some interfaces to facilitate sending messages between clients or broadcasting announcements to all clients.

## Client Behavior

The client operates using two threads:
1. A keyboard thread that reads input from the user keyboard and sends packets to the server based on the command.
2. A listening thread that reads packets from the socket, displays messages, or sends packets in return.

### Keyboard Thread Commands

1. **LOGRQ (Login Request)**
   - **Format:** `LOGRQ <Username>`
   - **Result:** Sends a LOGRQ packet to the server with the `<Username>` and waits for an `ACK` with block number 0 or an `ERROR` packet.

2. **DELRQ (Delete Request)**
   - **Format:** `DELRQ <Filename>`
   - **Result:** Sends a DELRQ packet to the server with the `<Filename>` and waits for an `ACK` with block number 0 or an `ERROR` packet.

3. **RRQ (Read Request)**
   - **Format:** `RRQ <Filename>`
   - **Result:** Sends an RRQ packet to the server to download the file. The file is created in the current working directory if it doesn't already exist.

4. **WRQ (Write Request)**
   - **Format:** `WRQ <Filename>`
   - **Result:** Sends a WRQ packet to the server to upload the file.

5. **DIRQ (Directory Request)**
   - **Format:** `DIRQ`
   - **Result:** Sends a DIRQ packet to the server to list all file names in the server's Files folder.

6. **DISC (Disconnect)**
   - **Format:** `DISC`
   - **Result:** Disconnects the user from the server and closes the program.

### Listening Thread

1. **DATA Packet**
   - Saves the data to a file or a buffer depending on whether the command is `RRQ` or `DIRQ`, and sends an `ACK` packet with the corresponding block number.

2. **ACK Packet**
   - Prints `ACK <block number>` to the terminal.

3. **BCAST Packet**
   - Prints `BCAST <del/add> <file name>` to the terminal.

4. **Error Packet**
   - Prints `Error <Error number> <Error message if exists>` to the terminal.

## TFTP Protocol

The extended TFTP supports various commands for receiving and uploading files. Upon connecting, a client must specify their username using a Login command. The nickname must be unique and cannot be changed after it is set. Once the command is sent, the server will reply on the validity of the username. Once a user is logged in successfully, they can submit commands for file transferring.

### Supported Packets

There are two types of commands, Server-to-Client and Client-to-Server. The commands begin with 2 bytes (short) to describe the opcode. The rest of the message will be defined specifically for each command as such:

1. **LOGRQ (Login Request)**
   - **Opcode:** 7
   - **Format:** `[Opcode] [Username] 0`
   - **Example:** `07 53 61 6C 74 7A 6D 61 6E 00`

2. **DELRQ (Delete Request)**
   - **Opcode:** 8
   - **Format:** `[Opcode] [Filename] 0`

3. **RRQ (Read Request)**
   - **Opcode:** 1
   - **Format:** `[Opcode] [Filename] 0`

4. **WRQ (Write Request)**
   - **Opcode:** 2
   - **Format:** `[Opcode] [Filename] 0`

5. **DIRQ (Directory Request)**
   - **Opcode:** 6
   - **Format:** `[Opcode]`

6. **DATA (Data Packet)**
   - **Opcode:** 3
   - **Format:** `[Opcode] [Block number] [Data]`

7. **ACK (Acknowledgment)**
   - **Opcode:** 4
   - **Format:** `[Opcode] [Block number]`

8. **ERROR (Error)**
   - **Opcode:** 5
   - **Format:** `[Opcode] [Error code] [Error message] 0`

9. **BCAST (Broadcast)**
   - **Opcode:** 9
   - **Format:** `[Opcode] [Add/Delete] [Filename] 0`

10. **DISC (Disconnect)**
   - **Opcode:** 10
   - **Format:** `[Opcode]`

## Building and Running

### Prerequisites

- Ensure you have Java Development Kit (JDK) installed, preferably version 8 or higher.
- Apache Maven is recommended for building the project.

### Building the Project

1. **Clone the Repository**
   ```sh
   git clone <repository-url>
   cd <repository-directory>
   ```

2. **Build with Maven**
   - **From the server directory and from the client directory:** `mvn compile`

### Running the Project

- **From the server directory:**
     ```sh
     mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.tftp.TftpServer" -Dexec.args="<port>"
     ```
- **From the client directory:**
     ```sh
     mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.tftp.TftpClient" -Dexec.args="<ip> <port>"
     ```
