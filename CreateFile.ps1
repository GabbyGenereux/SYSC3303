
    param(
        [Parameter(Mandatory=$true)]
        [string] $fileName,
        [Parameter(Mandatory=$true)]
        [int]$fileSize,
        [string]$sizeSuffix,
        [int]$additionalBytes = 0,
        [bool]$random = $true  

    )

    $sizeSuffix = $sizeSuffix.ToLower()

    If($sizeSuffix.ToCharArray()[0] -eq "k") {
        $fileSize = $fileSize * 1024
    }
    If($sizeSuffix.ToCharArray()[0] -eq "m") {
        $fileSize = $fileSize * (1024 * 1024)
    }
    if ($sizeSuffix.ToCharArray()[0] -eq "g") {
        $fileSize = $fileSize * (1024 * 1024 * 1024) 
    }
    $fileSize = $fileSize + $additionalBytes
    
    if ($random -eq $false) {
        fsutil file createnew $fileName $fileSize
    }
    else {
        $bytes = New-Object Byte[] $fileSize
        (New-Object Random).NextBytes($bytes)

        $path = (Get-Item -Path ".\" -Verbose).FullName + "\" + $fileName

        
        [System.IO.File]::WriteAllBytes($path, $bytes)
    }
    
    echo "File Created"


