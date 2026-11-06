$ErrorActionPreference = 'Stop'

$client = $null
$stream = $null
$reader = $null
$writer = $null

try {
    $client = [System.Net.Sockets.TcpClient]::new()
    $client.Connect('127.0.0.1', 9090)
    $stream = $client.GetStream()
    $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::UTF8)
    $writer = [System.IO.StreamWriter]::new($stream, [System.Text.UTF8Encoding]::new($false))
    $writer.AutoFlush = $true

    Write-Host 'Connected to SCASP on 127.0.0.1:9090. Type QUIT to disconnect.'
    while ($true) {
        $command = Read-Host 'SCASP'
        $writer.WriteLine($command)
        $reply = $reader.ReadLine()
        if ($null -eq $reply) {
            Write-Host 'The server closed the connection.'
            break
        }
        Write-Host $reply
        if ($command.Trim().ToUpperInvariant() -eq 'QUIT') { break }
    }
}
catch {
    Write-Host "Could not connect to the gateway: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host 'Start the gateway first in another PowerShell window with .\run.ps1.'
}
finally {
    if ($writer) { $writer.Dispose() }
    if ($reader) { $reader.Dispose() }
    if ($stream) { $stream.Dispose() }
    if ($client) { $client.Dispose() }
}
