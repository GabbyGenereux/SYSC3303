PowerShell Specific note: You may need to enable PowerShell Scripts by typing:
Set-ExecutionPolicy RemoteSigned 
In a PowerShell terminal that is run as administrator.


Creates a file with specified filename and file size, either empty (full of NULL bytes) or full of random bytes.

USAGE
[arg] is mandatory
<arg> can be omitted
Note: if omitting some arugments but not others, the command line switch needs to be used

In a PowerShell window, 
.\CreateFile [fileName] [fileSize] <sizeSuffix> <additionalBytes> <random>

fileName:
	Name of file to be written to disk
fileSize:
	Size of file to be created (not including additionalBytes argument)
sizeSuffix:
	Can specify KB/MB/GB for kilobytes, megabytes, gigabytes
additionalBytes
	Add additional bytes over specified fileSize
random:
	Set whether to create the file with random data (default) or empty.
	Note: For very large file sizes, creating random files may take a long time.


Examples:
Creates a file called SMALLFILE.bin with size of 500 bytes of random data
	Create-RandomFile SMALLFILE.bin 500

Creates a file called MEDFILE.bin with size of 2048 + 36 = 2084 bytes of random data
	Create-FileOfSize MEDFILE.bin 2 kb 36

Creates a file called EMPTYFILE.bin with size of 100 bytes and contains only NULL bytes
	Create-FileOfSize EMPTYFILE.bin 100 -random $false

Verbose example:
Creates a file called FILE.bin with size of 512mb+1 of random data
	Create-FileOfSize -fileName FILE.bin -fileSize 512 -sizeSuffix mb -additionalBytes 1 -random true