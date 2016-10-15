SYSC3303 Project Iteration 1
Team 9: Gabrielle, Henri, Joshua, Travis, Iain
Java Files Included:
Client.java, IntermediateHost.java, Server.java, ServerThread.java, TFTPInfoPrinter.java
Setup Instructions:
Compile the included files, then Run Server.java, followed by IntermediateHost.java, and finally Client.java


Note for future README writer: Need the ClientFiles and ServerFiles folders.
The ClientFiles folder needs to be in the same directory as the bin folder
Same thing with the ServerFiles.
structure should be something like this:
\Server\bin\
			Server.class
			ServerInput.class
			ServerThread.class
			TFTPInfoPrinter.class
			AckPacket.class
			ErrorPacket.class
			DataPacket.class
			RequestPacket.class
\Server\ServerFiles\
					512.bin
					1024.bin
					2M.bin

\Client\bin\
			Client.class
			TFTPInfoPrinter.class
			AckPacket.class
			ErrorPacket.class
			DataPacket.class
			RequestPacket.class
\Client\ClientFiles\
					myfile.txt
					myotherfile.png
					
USAGE:
In eclipse:
1. Make sure the folder structure is as described above, If the ServerFiles and ClientFiles folders do not exist, make them according to the structure.
2. Start the Server/Client/IntermediateHost
3. Set up Server/Client/IntermediateHost
3a. In the server console, select the verbosity by entering "verbose"/"v" for verbose mode or "quiet"/"q" for quiet mode. 
3b. Do the same for IntermediateHost if run.
3c. In the Client console, select normal/test mode with either "normal"/"n" or "test"/"t" respectively. The test mode connects through the IntermediateHost, and as such the IntermediateHost should be run if test mode is enabled.
4. In the Client console: Enter the filename name you wish to transfer, or "shutdown" without quotes to stop the client.
	Note: 	When sending from the client to the server, either a enter a simple filename such as "file.txt" or the full path, such as "C:\Users\JoeSmith\file.txt".
			In the case of entering a simple filename, the client will look for the file in the ClientFiles folder, as shown in the above folder structure.
5. The transfer will now take place and any file I/O issue will be reported and handled.

					