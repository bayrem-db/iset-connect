<#
.SYNOPSIS
End-to-end API smoke test for ISET Connect (PHP backend + MySQL).
Run after deploy_backend.ps1 and with Apache (or `php -S`) listening.
Usage: powershell -ExecutionPolicy Bypass -File tests\api_smoke_test.ps1 -BaseUrl "http://127.0.0.1:8765/api/"
#>
param([string]$BaseUrl = "http://127.0.0.1:8765/api/")

$script:passed = 0
$script:failed = 0

function Invoke-Api {
    param([string]$Method, [string]$Path, [hashtable]$Body = @{}, [string]$Token = "")
    $url = $BaseUrl.TrimEnd('/') + '/' + $Path.TrimStart('/')
    $headers = @{ 'Accept' = 'application/json' }
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $requestBody = $Body
    if ($Method -eq 'GET' -and $Body.Count -gt 0) {
        $pairs = @()
        foreach ($key in $Body.Keys) {
            $pairs += [uri]::EscapeDataString($key) + '=' + [uri]::EscapeDataString([string]$Body[$key])
        }
        $url += '?' + ($pairs -join '&')
        $requestBody = @{}
    }
    try {
        $response = Invoke-WebRequest -Uri $url -Method $Method -Headers $headers -Body $requestBody -UseBasicParsing -TimeoutSec 20
        $code = [int]$response.StatusCode
        $json = $response.Content | ConvertFrom-Json
    } catch {
        $code = 0
        $json = $null
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $raw = $reader.ReadToEnd()
            try { $json = $raw | ConvertFrom-Json } catch { $json = @{ message = $raw } }
        } else {
            $json = @{ message = $_.Exception.Message }
        }
    }
    return @{ Code = $code; Json = $json }
}

function Assert-True {
    param([bool]$Condition, [string]$Name, [string]$Detail = "")
    if ($Condition) {
        $script:passed++
        Write-Host "  PASS  $Name" -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host "  FAIL  $Name $Detail" -ForegroundColor Red
    }
}

Write-Host "=== ISET Connect API smoke test ===" -ForegroundColor Cyan

# --- Authentication ---
$r = Invoke-Api -Method POST -Path 'auth/login.php' -Body @{ email = 'admin@iset.tn'; password = 'wrong' }
Assert-True ($r.Code -eq 401 -and -not $r.Json.success) "Login rejects wrong password" "(code $($r.Code))"

$r = Invoke-Api -Method POST -Path 'auth/login.php' -Body @{ email = 'admin@iset.tn'; password = 'Admin@ISET2026' }
Assert-True ($r.Code -eq 200 -and $r.Json.success -and $r.Json.data.token) "Admin login"
$adminToken = if ($r.Json.data.token) { $r.Json.data.token } else { "" }
Assert-True ($r.Json.data.user.role -eq 'ADMIN') "Admin role returned"

$r = Invoke-Api -Method POST -Path 'auth/login.php' -Body @{ email = 'mohamed.t@iset.tn'; password = 'Student@123' }
Assert-True ($r.Code -eq 403 -and $r.Json.data.status -eq 'PENDING') "Pending login blocked with status" "(code $($r.Code))"

$r = Invoke-Api -Method POST -Path 'auth/login.php' -Body @{ email = 'sara.benali@iset.tn'; password = 'Student@123' }
Assert-True ($r.Code -eq 200 -and $r.Json.success) "Approved student login"
$studentToken = if ($r.Json.data.token) { $r.Json.data.token } else { "" }

# --- Authorization ---
$r = Invoke-Api -Method GET -Path 'dashboard/admin.php'
Assert-True ($r.Code -eq 401) "Admin endpoint requires token" "(code $($r.Code))"

$r = Invoke-Api -Method GET -Path 'dashboard/admin.php' -Token $studentToken
Assert-True ($r.Code -eq 403) "Student cannot access admin endpoint" "(code $($r.Code))"

# --- Dashboards ---
$r = Invoke-Api -Method GET -Path 'dashboard/admin.php' -Token $adminToken
Assert-True ($r.Json.success -and $r.Json.data.total_students -ge 3 -and $r.Json.data.total_materials -ge 7) "Admin dashboard statistics"
$r = Invoke-Api -Method GET -Path 'dashboard/student.php' -Token $studentToken
Assert-True ($r.Json.success -and $r.Json.data.user.email -eq 'sara.benali@iset.tn') "Student dashboard statistics"

# --- Students management ---
$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$newEmail = "test$stamp@iset.tn"
$newCin = "TEST$stamp"
$r = Invoke-Api -Method POST -Path 'auth/register.php' -Body @{
    first_name='Test'; last_name='Student'; cin=$newCin; email=$newEmail
    password='Student@123'; confirm_password='Student@123'; class_name='GI1'
}
Assert-True ($r.Code -eq 201 -and $r.Json.success) "Student registration" "$($r.Json.message)"

$r = Invoke-Api -Method POST -Path 'auth/register.php' -Body @{
    first_name='Dup'; last_name='Student'; cin=$newCin; email="other$stamp@iset.tn"
    password='Student@123'; confirm_password='Student@123'; class_name='GI1'
}
Assert-True ($r.Code -eq 400 -and -not $r.Json.success) "Duplicate CIN rejected"

$r = Invoke-Api -Method GET -Path 'students/list.php' -Token $adminToken -Body @{ q = $newEmail }
Assert-True ($r.Json.data.students.Count -eq 1) "Student search by email"
$newId = [long]$r.Json.data.students[0].id

$r = Invoke-Api -Method POST -Path 'students/approve.php' -Token $adminToken -Body @{ id = $newId }
Assert-True ($r.Json.success) "Student approval"
$r = Invoke-Api -Method POST -Path 'auth/login.php' -Body @{ email = $newEmail; password = 'Student@123' }
Assert-True ($r.Code -eq 200) "Approved student can log in"
$tempStudentToken = if ($r.Json.data.token) { $r.Json.data.token } else { "" }

# --- Materials CRUD ---
$r = Invoke-Api -Method POST -Path 'materials/create.php' -Token $adminToken -Body @{
    name="Test Kit $stamp"; description='Integration test material'; category='Electronics'; total_quantity='3'; image_url=''
}
Assert-True ($r.Code -eq 201) "Create material"
$r = Invoke-Api -Method GET -Path 'materials/list.php' -Token $adminToken -Body @{ q = "Test Kit $stamp" }
$materialId = [long]$r.Json.data.materials[0].id
Assert-True ($materialId -gt 0 -and $r.Json.data.materials[0].available_quantity -eq 3) "Material stock initialized"
$r = Invoke-Api -Method POST -Path 'materials/update.php' -Token $adminToken -Body @{
    id=$materialId; name="Updated Kit $stamp"; description='Updated'; category='Electronics'; total_quantity='4'; image_url=''
}
Assert-True ($r.Json.success) "Update material"

# --- Loans lifecycle ---
$r = Invoke-Api -Method POST -Path 'loans/create.php' -Token $tempStudentToken -Body @{
    material_id=$materialId; quantity='9'; borrowing_date=(Get-Date -Format 'yyyy-MM-dd'); expected_return_date=(Get-Date).AddDays(7).ToString('yyyy-MM-dd')
}
Assert-True ($r.Code -eq 400) "Insufficient quantity rejected"
$r = Invoke-Api -Method POST -Path 'loans/create.php' -Token $tempStudentToken -Body @{
    material_id=$materialId; quantity='2'; borrowing_date=(Get-Date -Format 'yyyy-MM-dd'); expected_return_date=(Get-Date).AddDays(7).ToString('yyyy-MM-dd')
}
Assert-True ($r.Code -eq 201) "Create borrowing request"
$r = Invoke-Api -Method GET -Path 'loans/list.php' -Token $adminToken -Body @{ status='PENDING'; q="Updated Kit $stamp" }
$loanId = [long]$r.Json.data.loans[0].id
$r = Invoke-Api -Method POST -Path 'loans/approve.php' -Token $adminToken -Body @{ id = $loanId }
Assert-True ($r.Json.success) "Approve borrowing request"
$r = Invoke-Api -Method GET -Path 'materials/details.php' -Token $adminToken -Body @{ id = $materialId }
Assert-True ($r.Json.data.material.available_quantity -eq 2) "Stock decreased on approval"
$r = Invoke-Api -Method POST -Path 'loans/return.php' -Token $adminToken -Body @{ id = $loanId }
Assert-True ($r.Json.success) "Register equipment return"
$r = Invoke-Api -Method POST -Path 'loans/return.php' -Token $adminToken -Body @{ id = $loanId }
Assert-True ($r.Code -eq 400) "Double return prevented"
$r = Invoke-Api -Method GET -Path 'materials/details.php' -Token $adminToken -Body @{ id = $materialId }
Assert-True ($r.Json.data.material.available_quantity -eq 4) "Stock restored on return"
$r = Invoke-Api -Method POST -Path 'materials/delete.php' -Token $adminToken -Body @{ id = $materialId }
Assert-True ($r.Code -eq 400 -and -not $r.Json.success) "Material with loan history is protected from deletion"
$r = Invoke-Api -Method POST -Path 'materials/create.php' -Token $adminToken -Body @{
    name="Delete Me $stamp"; description='Temporary material'; category='Other'; total_quantity='1'; image_url=''
}
$r = Invoke-Api -Method GET -Path 'materials/list.php' -Token $adminToken -Body @{ q = "Delete Me $stamp" }
$tempMaterialId = [long]$r.Json.data.materials[0].id
$r = Invoke-Api -Method POST -Path 'materials/delete.php' -Token $adminToken -Body @{ id = $tempMaterialId }
Assert-True ($r.Json.success) "Delete material CRUD"

# --- Internships & applications ---
$r = Invoke-Api -Method POST -Path 'internships/create.php' -Token $adminToken -Body @{
    title="Intern $stamp"; company_name='Test Company'; company_address='Tunis'; domain='Web Development'
    description='Test internship'; required_skills='PHP'; start_date=(Get-Date).AddDays(10).ToString('yyyy-MM-dd')
    end_date=(Get-Date).AddDays(60).ToString('yyyy-MM-dd'); application_deadline=(Get-Date).AddDays(5).ToString('yyyy-MM-dd')
    total_positions='1'; status='DRAFT'
}
Assert-True ($r.Code -eq 201) "Create internship"
$r = Invoke-Api -Method GET -Path 'internships/list.php' -Token $adminToken -Body @{ q="Intern $stamp" }
$internshipId = [long]$r.Json.data.internships[0].id
$r = Invoke-Api -Method POST -Path 'internships/publish.php' -Token $adminToken -Body @{ id = $internshipId }
Assert-True ($r.Json.success) "Publish internship"
$r = Invoke-Api -Method POST -Path 'applications/create.php' -Token $tempStudentToken -Body @{
    internship_id=$internshipId; motivation_message='I am interested in this position.'
}
Assert-True ($r.Code -eq 201) "Student application"
$r = Invoke-Api -Method POST -Path 'applications/create.php' -Token $tempStudentToken -Body @{
    internship_id=$internshipId; motivation_message='Duplicate application.'
}
Assert-True ($r.Code -eq 400) "Duplicate application rejected"
$r = Invoke-Api -Method GET -Path 'applications/list.php' -Token $adminToken -Body @{ internship_id=$internshipId }
$applicationId = [long]$r.Json.data.applications[0].id
$r = Invoke-Api -Method POST -Path 'applications/accept.php' -Token $adminToken -Body @{ id = $applicationId }
Assert-True ($r.Json.success) "Accept application"
$r = Invoke-Api -Method POST -Path 'applications/accept.php' -Token $adminToken -Body @{ id = $applicationId }
Assert-True ($r.Code -eq 400) "Double acceptance prevented"
$r = Invoke-Api -Method GET -Path 'internships/details.php' -Token $adminToken -Body @{ id = $internshipId }
Assert-True ($r.Json.data.internship.available_positions -eq 0) "Positions decreased on acceptance"

# --- Notifications ---
$r = Invoke-Api -Method GET -Path 'notifications/list.php' -Token $tempStudentToken
Assert-True ($r.Json.success -and $r.Json.data.notifications.Count -gt 0 -and $r.Json.data.unread_count -gt 0) "Notifications generated"
$r = Invoke-Api -Method POST -Path 'notifications/mark_read.php' -Token $tempStudentToken -Body @{ all = 1 }
Assert-True ($r.Json.success) "Mark all notifications read"
$r = Invoke-Api -Method GET -Path 'notifications/list.php' -Token $tempStudentToken -Body @{ unread_only = 1 }
Assert-True ($r.Json.data.notifications.Count -eq 0) "Unread list empty after mark all"

# --- Profile ---
$r = Invoke-Api -Method POST -Path 'auth/profile.php' -Token $tempStudentToken -Body @{
    first_name='Updated'; last_name='Student'; class_name='GI2'; email=$newEmail
}
Assert-True ($r.Json.success) "Profile update"
$r = Invoke-Api -Method POST -Path 'auth/change_password.php' -Token $tempStudentToken -Body @{
    current_password='Student@123'; new_password='NewPass@123'; confirm_password='NewPass@123'
}
Assert-True ($r.Json.success) "Change password"

# --- Extra business rules: cancellation, rejection, invalid dates, filters ---
# Changing the password revokes all tokens, so obtain a fresh one.
$r = Invoke-Api -Method POST -Path 'auth/login.php' -Body @{ email = $newEmail; password = 'NewPass@123' }
Assert-True ($r.Code -eq 200) "Login with new password after token revocation"
$tempStudentToken = if ($r.Json.data.token) { $r.Json.data.token } else { "" }

$r = Invoke-Api -Method POST -Path 'loans/create.php' -Token $tempStudentToken -Body @{
    material_id=$materialId; quantity='1'; borrowing_date=(Get-Date).AddDays(1).ToString('yyyy-MM-dd'); expected_return_date=(Get-Date).AddDays(1).ToString('yyyy-MM-dd')
}
Assert-True ($r.Code -eq 400) "Return date must be after borrowing date"
$r = Invoke-Api -Method POST -Path 'loans/create.php' -Token $tempStudentToken -Body @{
    material_id=$materialId; quantity='1'; borrowing_date=(Get-Date -Format 'yyyy-MM-dd'); expected_return_date=(Get-Date).AddDays(7).ToString('yyyy-MM-dd')
}
Assert-True ($r.Code -eq 201) "Create request for cancellation"
$r = Invoke-Api -Method GET -Path 'loans/list.php' -Token $tempStudentToken -Body @{ status='PENDING' }
$cancelLoanId = [long]($r.Json.data.loans | Where-Object { $_.material_id -eq $materialId } | Select-Object -First 1).id
$r = Invoke-Api -Method POST -Path 'loans/cancel.php' -Token $tempStudentToken -Body @{ id = $cancelLoanId }
Assert-True ($r.Json.success) "Student cancels own pending request"
$r = Invoke-Api -Method POST -Path 'loans/cancel.php' -Token $tempStudentToken -Body @{ id = $cancelLoanId }
Assert-True ($r.Code -eq 400) "Cannot cancel same request twice"
$r = Invoke-Api -Method POST -Path 'loans/create.php' -Token $tempStudentToken -Body @{
    material_id=$materialId; quantity='1'; borrowing_date=(Get-Date -Format 'yyyy-MM-dd'); expected_return_date=(Get-Date).AddDays(7).ToString('yyyy-MM-dd')
}
$r = Invoke-Api -Method GET -Path 'loans/list.php' -Token $adminToken -Body @{ status='PENDING'; q="Updated Kit $stamp" }
$rejectLoanId = [long]$r.Json.data.loans[0].id
$r = Invoke-Api -Method POST -Path 'loans/reject.php' -Token $adminToken -Body @{ id = $rejectLoanId; reason = 'Not available' }
Assert-True ($r.Json.success) "Admin rejects loan request with notification"
$r = Invoke-Api -Method GET -Path 'students/list.php' -Token $adminToken -Body @{ q = $newCin }
Assert-True ($r.Json.data.students.Count -eq 1) "Student search by CIN"

$r = Invoke-Api -Method POST -Path 'internships/create.php' -Token $adminToken -Body @{
    title="Reject Intern $stamp"; company_name='Test Company'; company_address='Sfax'; domain='Networking'
    description='Reject flow'; required_skills='TCP/IP'; start_date=(Get-Date).AddDays(10).ToString('yyyy-MM-dd')
    end_date=(Get-Date).AddDays(60).ToString('yyyy-MM-dd'); application_deadline=(Get-Date).AddDays(5).ToString('yyyy-MM-dd')
    total_positions='2'; status='PUBLISHED'
}
$r = Invoke-Api -Method GET -Path 'internships/list.php' -Token $adminToken -Body @{ q="Reject Intern $stamp" }
$rejectInternshipId = [long]$r.Json.data.internships[0].id
$r = Invoke-Api -Method POST -Path 'applications/create.php' -Token $tempStudentToken -Body @{
    internship_id=$rejectInternshipId; motivation_message='Please consider my application.'
}
$r = Invoke-Api -Method GET -Path 'applications/list.php' -Token $adminToken -Body @{ internship_id=$rejectInternshipId }
$rejectApplicationId = [long]$r.Json.data.applications[0].id
$r = Invoke-Api -Method POST -Path 'applications/reject.php' -Token $adminToken -Body @{ id=$rejectApplicationId; reason='Profile mismatch' }
Assert-True ($r.Json.success) "Admin rejects application with notification"
$r = Invoke-Api -Method POST -Path 'applications/create.php' -Token $tempStudentToken -Body @{
    internship_id=$rejectInternshipId; motivation_message='Second application.'
}
Assert-True ($r.Code -eq 400 -and -not $r.Json.success) "Cannot re-apply after rejection (unique constraint)"

Write-Host "`n=== Results: $script:passed passed, $script:failed failed ===" -ForegroundColor Cyan
if ($script:failed -gt 0) { exit 1 }
