$instr = 'SDRTrunk build failed.'
$body = @{ contents = @( @{ parts = @( @{ text = $instr } ) } ) } | ConvertTo-Json -Depth 10
Write-Host $body
