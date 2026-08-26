[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ProgressPreference = "SilentlyContinue"
$base = "http://localhost:8080/api"

function Check($name, $resp) {
    $code = if ($resp.PSObject.Properties.Name -contains "code") { $resp.code } else { -1 }
    $ok = ($code -eq 1)
    $tag = if ($ok) {"PASS"} else {"FAIL"}
    $color = if ($ok) {"Green"} else {"Red"}
    Write-Host ("[{0}] {1}  code={2}" -f $tag, $name, $code) -ForegroundColor $color
    return $ok
}

Write-Host "========== Smoke-1 Admin Login ==========" -ForegroundColor Cyan
$body = @{ username="Admin"; password="Admin2026" } | ConvertTo-Json
$r = Invoke-RestMethod -Method Post "$base/auth/login" -Body $body -ContentType "application/json; charset=utf-8"
$adminOk = Check "AdminLogin" $r
if (-not $adminOk) { Write-Host "ABORT: admin login failed" ($r | ConvertTo-Json -Depth 5); exit 1 }
$adminToken = $r.data.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host ("admin token OK len={0}" -f $adminToken.Length)

Write-Host "========== Smoke-2 Student Register & Login ==========" -ForegroundColor Cyan
$rand = Get-Random -Minimum 100000 -Maximum 999999
$stuUser = "stu$rand"
$stuBody = @{
    username   = $stuUser
    password   = "Student@2026"
    confirmPassword = "Student@2026"
    role       = 2
    studentId  = "2026$rand"
    name       = "Test Student $rand"
    department = "CS"
    major      = "SE"
    phone      = "13800138000"
    email      = "$stuUser@qingzhi.edu.cn"
} | ConvertTo-Json -Depth 3
$r = Invoke-RestMethod -Method Post "$base/auth/register" -Body $stuBody -ContentType "application/json; charset=utf-8"
$regOk = Check "StudentRegister" $r
$loginBody = @{ username = $stuUser; password = "Student@2026" } | ConvertTo-Json
$r = Invoke-RestMethod -Method Post "$base/auth/login" -Body $loginBody -ContentType "application/json; charset=utf-8"
$loginOk = Check "StudentLogin" $r
if (-not $loginOk) { Write-Host "ABORT: student login failed"; exit 1 }
$stuToken = $r.data.token
$stuHeaders = @{ Authorization = "Bearer $stuToken" }
$studentId = $r.data.userInfo.id
Write-Host ("student id={0} tokenOK len={1}" -f $studentId, $stuToken.Length)

Write-Host "========== Smoke-2 Upload File + Publish Resource (PENDING) ==========" -ForegroundColor Cyan
$tmpFile = Join-Path $env:TEMP ("smoke_" + [Guid]::NewGuid().ToString("N") + ".txt")
$content = "Hello Qingzhi Smoke TICK=" + (Get-Date).Ticks
[System.IO.File]::WriteAllText($tmpFile, $content, [System.Text.Encoding]::UTF8)
Add-Type -AssemblyName System.Net.Http
$client  = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(60)
$client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $stuToken)
$form = New-Object System.Net.Http.MultipartFormDataContent
$fs   = [System.IO.File]::OpenRead($tmpFile)
$fileContent = New-Object System.Net.Http.StreamContent($fs)
$fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/plain")
$fileContent.Headers.ContentDisposition = New-Object System.Net.Http.Headers.ContentDispositionHeaderValue("form-data")
$fileContent.Headers.ContentDisposition.Name = "`"file`""
$fileContent.Headers.ContentDisposition.FileName = "`"$([System.IO.Path]::GetFileName($tmpFile))`""
$form.Add($fileContent)
$task = $client.PostAsync("$base/file/upload", $form)
$task.Wait()
$respBody = $task.Result.Content.ReadAsStringAsync().Result
$fs.Dispose(); $fileContent.Dispose(); $form.Dispose()
$r = $respBody | ConvertFrom-Json
$upOk = Check "UploadFile" $r
if (-not $upOk) { Write-Host "UPLOAD RAW: $respBody"; exit 1 }
$upData = $r.data
Write-Host ("Upload OK: fileStorageId={0} fileHash={1} fileName={2} storagePath={3}" -f $upData.fileStorageId, $upData.fileHash, $upData.fileName, $upData.storagePath)

$publishBody = @{
    title         = "Smoke Resource $rand"
    description   = "Auto smoke test resource $rand"
    course        = "Intro SE"
    fileStorageId = $upData.fileStorageId
    fileName      = $upData.fileName
    filePath      = $upData.storagePath
    fileSize      = $upData.fileSize
    fileExt       = $upData.fileExt
    fileHash      = $upData.fileHash
} | ConvertTo-Json -Depth 3
$r = Invoke-RestMethod -Method Post "$base/resource/publish" -Body $publishBody -Headers $stuHeaders -ContentType "application/json; charset=utf-8"
$pubOk = Check "PublishResource(PENDING)" $r
if (-not $pubOk) { Write-Host ($r | ConvertTo-Json -Depth 6); exit 1 }
$resourceId = $r.data.resourceId
Write-Host ("resourceId={0}" -f $resourceId)

Write-Host "========== Smoke-3 Admin Review PASS (approve=true) ==========" -ForegroundColor Cyan
$reviewBody = @{
    resourceId   = $resourceId
    approve      = $true
    rejectReason = $null
} | ConvertTo-Json -Depth 3
$r = Invoke-RestMethod -Method Post "$base/admin/resources/review" -Body $reviewBody -Headers $adminHeaders -ContentType "application/json; charset=utf-8"
$reviewOk = Check "AdminReviewPass(approve=true)" $r
if (-not $reviewOk) { Write-Host ($r | ConvertTo-Json -Depth 6) }

Write-Host "========== Smoke-4 Download + download_count +1 ==========" -ForegroundColor Cyan
$r = Invoke-RestMethod -Method Get "$base/resource/$resourceId" -Headers $stuHeaders
$null = Check "ResourceDetailGET" $r
$before = [int]$r.data.downloadCount
Write-Host ("download_count BEFORE={0}" -f $before)
$dlOk = $false
try {
    $wr = Invoke-WebRequest -Method Get "$base/file/download/$resourceId" -UseBasicParsing -Headers $stuHeaders -TimeoutSec 30
    if ($wr.StatusCode -eq 200) {
        $obj = [PSCustomObject]@{ code=1; message="dl200" }
        $dlOk = Check "DownloadHTTP200" $obj
    }
} catch {
    Write-Host ("Download ERR: " + $_.Exception.Message) -ForegroundColor Red
    if ($_.Exception.Response) {
        $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream(), [System.Text.Encoding]::UTF8)
        Write-Host ("BODY: " + $sr.ReadToEnd())
    }
}
$r = Invoke-RestMethod -Method Get "$base/resource/$resourceId" -Headers $stuHeaders
$after = [int]$r.data.downloadCount
Write-Host ("download_count AFTER={0}" -f $after)
$expected = $before + 1
$dcOk = if ($after -eq $expected) { $o=[PSCustomObject]@{code=1}; Check "download_count +1 ($before -> $after)" $o; $true } else { $o=[PSCustomObject]@{code=0}; Check "download_count +1 (expected=$expected got=$after)" $o; $false }

Write-Host "========== Smoke-5 Favorite (add/remove/add again) ==========" -ForegroundColor Cyan
$favBody = @{ resourceId = $resourceId } | ConvertTo-Json
$r = Invoke-RestMethod -Method Post "$base/favorite/add" -Body $favBody -Headers $stuHeaders -ContentType "application/json; charset=utf-8"
$favOk = Check "FavoriteAdd (POST /favorite/add)" $r
Start-Sleep -Milliseconds 300
$r = Invoke-RestMethod -Method Post "$base/favorite/remove" -Body $favBody -Headers $stuHeaders -ContentType "application/json; charset=utf-8"
$unfavOk = Check "FavoriteRemove (POST /favorite/remove)" $r
$r = Invoke-RestMethod -Method Post "$base/favorite/add" -Body $favBody -Headers $stuHeaders -ContentType "application/json; charset=utf-8"
$null = Check "FavoriteAddAgain(keep state)" $r

Write-Host "========== Smoke-6 Quick Upload same hash + UPLOAD_RATE_LIMITED 5003 ==========" -ForegroundColor Cyan
$client2 = New-Object System.Net.Http.HttpClient
$client2.Timeout = [TimeSpan]::FromSeconds(60)
$client2.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $stuToken)
$hasQuick = $false
$hasRate5003 = $false
for ($i = 1; $i -le 8; $i++) {
    $fm = New-Object System.Net.Http.MultipartFormDataContent
    $fs2 = [System.IO.File]::OpenRead($tmpFile)
    $fc  = New-Object System.Net.Http.StreamContent($fs2)
    $fc.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/plain")
    $fc.Headers.ContentDisposition = New-Object System.Net.Http.Headers.ContentDispositionHeaderValue("form-data")
    $fc.Headers.ContentDisposition.Name = "`"file`""
    $fc.Headers.ContentDisposition.FileName = "`"dup_$i.txt`""
    $fm.Add($fc)
    $t = $client2.PostAsync("$base/file/upload", $fm)
    $t.Wait()
    $body2 = $t.Result.Content.ReadAsStringAsync().Result
    $fs2.Dispose(); $fc.Dispose(); $fm.Dispose()
    $p = $body2 | ConvertFrom-Json
    if ($p.code -eq 1) {
        $qu = $false
        if ($p.data -and $p.data.PSObject.Properties.Name -contains "hitQuickUpload") { $qu = [bool]$p.data.hitQuickUpload }
        if ($qu) {
            $hasQuick = $true
            Write-Host ("[PASS] Attempt-{0} code=1 hitQuickUpload=true" -f $i) -ForegroundColor Green
        } else {
            Write-Host ("[INFO] Attempt-{0} code=1 hitQuickUpload?=$qu (fresh upload OK)" -f $i) -ForegroundColor Gray
        }
    } elseif ($p.code -eq 5003) {
        $hasRate5003 = $true
        Write-Host ("[PASS] Attempt-{0} code=5003 UPLOAD_RATE_LIMITED (6/min hit)" -f $i) -ForegroundColor Green
        break
    } else {
        Write-Host ("[FAIL] Attempt-{0} code={1} raw={2}" -f $i, $p.code, $body2) -ForegroundColor Red
    }
}
$client2.Dispose()
$sixOk = if ($hasQuick -and $hasRate5003) { $o=[PSCustomObject]@{code=1}; Check "QuickUpload(true)+RateLimit5003" $o; $true } else { $o=[PSCustomObject]@{code=0}; Check "QuickUpload?=$hasQuick RateLimit?=$hasRate5003" $o; $false }

Write-Host ""
Write-Host "========== FINAL SMOKE SUMMARY ==========" -ForegroundColor Cyan
function P($label, $cond) { $s = if($cond){"PASS"}else{"FAIL"}; $c = if($cond){"Green"}else{"Red"}; Write-Host ("{0,-22} -> {1}" -f $label, $s) -ForegroundColor $c }
P "1_AdminLogin"         $adminOk
P "2_StudentRegLogin"    ($regOk -and $loginOk)
P "2_UploadAndPublish"   ($upOk -and $pubOk)
P "3_AdminReviewPass"    $reviewOk
P "4_DownloadAndCount+1" ($dlOk -and $dcOk)
P "5_FavoriteToggle"     ($favOk -and $unfavOk)
P "6_QuickAndRateLimit"  $sixOk

Remove-Item $tmpFile -ErrorAction SilentlyContinue
Write-Host "Done."