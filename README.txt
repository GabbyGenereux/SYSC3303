SYSC3303 Project Iteration 1
Team 9: Gabrielle, Henri, Joshua, Travis, Iain
Java Files Included:
Client.java - A client to a TFTP server, handles reading from and writing to a server listening to the well known port 69, or 23 in test mode.
HostInput.java - A separate thread to handle input for the Intermediate Host without interfering with operation.
IntermediateHost.java - Host that the server connects to in test mode, will pass on all packets from client to server and vice versa.
Server.java - A server to a TFTP server, handles reading from and writing to client. 
ServerInput.java - Thread to handle input on server without interacting with main thread.
ServerThread.java - Thread that handles the connection after the initial request was received, so the server can go back to waiting for more connections.
TFTPInfoPrinter.java - Centralized class to print information on sent and received packets, depending on verbosity.
RequestPacket.java - Class to represent TFTP specified packet with opcode 01/02.
DataPacket.java - Class to represent TFTP specified packet with opcode 03.
AckPacket.java - Class to represent TFTP specified packet with opcode 04.
ErrorPacket.Java - Class to represent TFTP specified packet with opcode 05.

Binary Files Included:
Client.class, IntermediateHost.class, Server.class, HostInput.class, ServerInput.class, ServerThread.class, TFTPInfoPrinter.class, DataPacket.class, ErrorPacket.class, AckPacket.class, RequestPacket.class

Other Files Included:
Responsibilities.txt - Outlines the roles each team member had for this and past iterations.
UCM request read-write.png - UCM showing the initial read/write request from client to server, through the IntermediateHost
UCM SS read transfer.png - UCM for the steady state read transfer.
UCM SS write transfer.png - UCM for the steady state write transfer
timing_diag_access_violation.png - timing diagram for the occurrence of an invalid permissions I/O error
timing_diag_disk_full_read.png - timing diagram for the occurrence of a disk full I/O error while reading.
timimg_diag_disk_full_write.png - timing diagram for the occurrence of a disk full I/O error while writing.
timing_diag_file_already_exists.png - timing diagram for the occurrence of a file already exits I/O error.
timing_diag_file_not_found.png - timing diagram for the occurrence of a file not found I/O error.
0.bin - Empty file
511.bin - 511 random bytes.
512.bin - 512 random bytes.
513.bin - 513 random bytes.
2k.bin - 2 KB (2048 bytes) random bytes.
1M.bin - 1 MB random bytes.
2M.bin - 2MB random bytes.
5M.bin - 5MB random bytes.


Setup:
/**************** Folder structure for program to work properly ****************\
/*** INSIDE Eclipse ***\
\bin\
    Server.class
    ServerInput.class
    ServerThread.class
    Client.class
    TFTPInfoPrinter.class
    HostInput.class
    IntermediateHost.class
    AckPacket.class
    ErrorPacket.class
    DataPacket.class
    RequestPacket.class
\ServerFiles\
            512.bin
            1024.bin
            2M.bin
\ClientFiles\
            myfile.txt
            myotherfile.png
			 
/*** OUTSIDE Eclipse ***\
\Server\
        Server.class
        ServerInput.class
        ServerThread.class
        TFTPInfoPrinter.class
        AckPacket.class
        ErrorPacket.class
        DataPacket.class
        RequestPacket.class
        \ServerFiles\
                     512.bin
                     1024.bin
                     2M.bin
\Client\
        Client.class
        TFTPInfoPrinter.class
        AckPacket.class
        ErrorPacket.class
        DataPacket.class
        RequestPacket.class	
        \ClientFiles\
                    myfile.txt
                    myotherfile.png
 \IntermediateHost\
 		IntermediateHost.class
 		HostInput.class
 		TFTPInfoPrinter.class


Note: the error simulator should be located on the same machine as the server.

USAGE:
In eclipse:
1. Make sure the folder structure is as described above, If the ServerFiles and ClientFiles folders do not exist, make them according to the structure.
2. Start the Server/Client/IntermediateHost
3. Set up Server/Client/IntermediateHost
3a. In the server console, select the verbosity by entering "verbose"/"v" for verbose mode or "quiet"/"q" for quiet mode. Then enter the server IP.
3b. In IntermediateHost, select the verbosity in the same way as server. 
	To perform an error simulation, enter 1 to lose a packet, 2 to delay a packet, 3 to duplicate a packet, 4 to change the opcode of a packet, 5 to change the block number of a packet, 6 to replace a zero byte and 7 to change the port of the transfer. Then, enter the type of packet to perform the error on, as well as the block number and delay between packets (if necessary). Entering a 0 when choosing an error performs normal operation.
	When a simulation is over, enter "r" to reset the IntermediateHost for the next transfer. This must be done between transfers or else the client will not be able to connect through the IntermediateHost. 
3c. In the Client console, select normal/test mode with either "normal"/"n" or "test"/"t" respectively. The test mode connects through the IntermediateHost, and as such the IntermediateHost needs to be run if test mode is enabled.
4. In the Client console: Enter the filename name you wish to transfer, or "shutdown" without quotes to stop the client.
	Note: 	When sending from the client to the server, either a enter a simple filename such as "file.txt" or the full path, such as "C:\Users\JoeSmith\file.txt".
			In the case of entering a simple filename, the client will look for the file in the ClientFiles folder, as shown in the above folder structure.
5. The transfer will now take place and any file I/O issue will be reported and handled.

					
