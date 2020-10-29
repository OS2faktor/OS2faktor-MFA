# connectionString skal være en lovlig SQL Server connectionString, der peger
# på den database hvor medarbejdernes personnumre er opbevaret
$connectionString = "Server=172.16.173.1; Database=rc; User Id=SA; Password=Test1234"

# sqlCommand udfyldes med det SQL statement der henter brugernes personnumre og pseudonymer ud.
# hvis man sørger for at de udlæste værdier har de rigtige navne (dvs bruger AS værdierne i eksemplet),
# så vil resten af scriptet passe
$sqlCommand = "SELECT personnummer AS ssn, sAMAccountName AS pseudonym FROM users"

# indsæt kommune-api nøglen her
$apiKey = "xxxxxxxxxxxxxxxxxxx"

function Invoke-SQL {
    param(
        [string] $connectionString = $(throw "Please specify a connection string."),
        [string] $sqlCommand = $(throw "Please specify a query.")
    )

    $connection = new-object system.data.SqlClient.SQLConnection($connectionString)
    $command = new-object system.data.sqlclient.sqlcommand($sqlCommand, $connection)
    $connection.Open()

    $adapter = New-Object System.Data.sqlclient.sqlDataAdapter $command
    $dataset = New-Object System.Data.DataSet
    $adapter.Fill($dataSet)

    $connection.Close()

    return $dataSet.Tables[0].Rows
}

function Invoke-Sha256AndBase64 {
    param(
        [string] $ssn = $(throw "Please supply a ssn string.")
    )

    $md = new-object System.Security.Cryptography.SHA256Managed

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($ssn)
    $digest = $md.ComputeHash($bytes)
    $encodedText = [Convert]::ToBase64String($digest)

    return $encodedText
}

# query database
$result = Invoke-SQL -connectionString $connectionString -sqlCommand $sqlCommand

# extract values
$users = New-Object System.Collections.ArrayList
for ($i = 1; $i -lt $result.Count; $i++) {
    $ssnClear = $result[$i].ssn
    $ssnEncoded = Invoke-Sha256AndBase64 -ssn $ssnClear
    $pseudonym = $result[$i].pseudonym

    $users.Add([pscustomobject]@{pseudonym=$pseudonym;ssn=$ssnEncoded})
}

$json = ConvertTo-Json -InputObject $users

Invoke-WebRequest -Uri "https://backend.os2faktor.dk/api/municipality/pseudonyms" -Method Post -Body $json -Headers @{"ApiKey"=$apiKey} -ContentType "application/json"
