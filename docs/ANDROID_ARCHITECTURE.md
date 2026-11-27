# ISET Connect — Android implementation guide (Java + XML)

Package root: `com.iset.connect`. Java only, XML layouts only, no Compose, no Kotlin, no SQLite/Firebase.

## Available infrastructure (already written — do not modify)

`network/ApiService.java` — typed Retrofit interface, all methods listed below.
`network/ApiClient.getService(SessionManager)` — singleton Retrofit instance (adds bearer token).
`network/ApiResponse<T>` — `isSuccess()`, `getMessage()`, `getData()`, `firstError()`.
`utils/SessionManager` — `saveLogin`, `getToken`, `getFullName`, `getClassName`, `getRole`, `isAdmin`, `isStudent`, `logout`, `updateProfile`.
`utils/Constants` — EXTRA_* keys, status constants, `MATERIAL_CATEGORIES`, `INTERNSHIP_DOMAINS`, `ACCOUNT_STATUSES`, `LOAN_STATUSES`, `APPLICATION_STATUSES`.
`utils/UiUtils.applyStatus(TextView, String)` — colors a status chip.
`utils/DateUtils.formatDisplay(...)`, `formatDateTime(...)`, `today()`, `isAfter(...)`.
`utils/ValidationUtils` — client-side checks (backend repeats them).
`models/*` — `User`, `Material`, `Loan`, `Internship`, `Application`, `Notification`; `models/ApiData` holds response wrappers (`StudentListData`, `AdminStats`, `StudentStats`, etc.).
`adapters/*` — `StudentAdapter(List<User>, listener)`, `MaterialAdapter(list, listener, actionListener, adminMode)`, `LoanAdapter(list, listener, adminMode)`, `InternshipAdapter(list, listener)`, `ApplicationAdapter(list, listener, adminMode)`, `NotificationAdapter(list, listener)`.
Item layouts: `item_student.xml`, `item_material.xml`, `item_loan.xml`, `item_internship.xml`, `item_application.xml`, `item_notification.xml`.
Design resources: colors, dimens, styles (`Text.Headline`, `Text.Title`, `Text.Subtitle`, `Text.Body`, `Text.Caption`, `Text.Link`, `Button.Primary`, `Button.Outline`, `Input.Outlined`), drawables (`bg_card`, `bg_input`, `bg_header_gradient`, `bg_primary_button`, `ic_*`).

## Conventions

- Every activity extends `AppCompatActivity`, sets its layout in `onCreate`, uses `MaterialToolbar` id `toolbar` with `setSupportActionBar(toolbar)` and `getSupportActionBar().setDisplayHomeAsUpEnabled(true)` where relevant; handle `android.R.id.home` → `finish()` or drawer.
- API pattern:

```java
api.listMaterials(query, category).enqueue(new Callback<ApiResponse<ApiData.MaterialListData>>() {
    @Override public void onResponse(Call<...> call, Response<...> response) {
        hideLoading();
        if (!response.isSuccessful() || response.body() == null) { toast("Server error"); return; }
        ApiResponse<ApiData.MaterialListData> body = response.body();
        if (!body.isSuccess()) { toast(body.getMessage()); return; }
        adapter.submit(body.getData().getMaterials());
    }
    @Override public void onFailure(Call<...> call, Throwable t) { hideLoading(); toast(getString(R.string.error_network)); }
});
```

- For write actions use `new MaterialAlertDialogBuilder(this)` confirmations, then `ApiResponse<Object>` callbacks; on success `Toast` + `finish()`/`loadData()`.
- Progress: `ProgressBar` id `progressBar` (indeterminate, `View.GONE` by default) + empty-state `TextView` id `emptyState` where lists appear.
- Use `RecyclerView` with `LinearLayoutManager`; never fake/local data.
- Session: `new SessionManager(this)`, `ApiClient.getService(session)`. If `!session.isLoggedIn()` redirect to `LoginActivity` and `finish()`.
- 401/403 response: clear session and go to `LoginActivity` for 401.
- All user-facing strings may be literal English; existing `strings.xml` values are available.

## Shared navigation resources

`layout/nav_bottom_student.xml` (created with the dashboards) is a `BottomNavigationView` with menu `menu/menu_student_bottom.xml`: ids `nav_home`, `nav_equipment`, `nav_internships`, `nav_notifications`, `nav_profile`.
Student list screens may `<include layout="@layout/nav_bottom_student" />` as the last child of a vertical root and wire item selection with `NavigationBarView.OnItemSelectedListener`.

## Endpoint method signatures (ApiService)

Auth: `register(...)`, `login(email,password)`, `logout()`, `getProfile()`, `updateProfile(first,last,class,email)`, `changePassword(current,new,confirm)`.
Dashboards: `adminStats()`, `studentStats()`.
Students: `listStudents(q,status)`, `studentDetails(id)`, `approveStudent(id)`, `rejectStudent(id,reason)`, `suspendStudent(id,reason)`, `reactivateStudent(id)`.
Materials: `listMaterials(q,category)`, `materialDetails(id)`, `createMaterial(name,desc,category,totalQuantity,imageUrl)`, `updateMaterial(id,...)`, `deleteMaterial(id)`.
Loans: `createLoan(materialId,quantity,borrowingDate,expectedReturnDate)`, `listLoans(q,status,scope)`, `loanDetails(id)`, `approveLoan(id)`, `rejectLoan(id,reason)`, `cancelLoan(id)`, `returnLoan(id)`.
Internships: `listInternships(q,domain,status)`, `internshipDetails(id)`, `createInternship(11 args)`, `updateInternship(12 args)`, `deleteInternship(id)`, `publishInternship(id)`, `closeInternship(id)`.
Applications: `createApplication(internshipId,motivation)`, `listApplications(q,status,internshipId)`, `acceptApplication(id)`, `rejectApplication(id,reason)`, `cancelApplication(id)`.
Notifications: `listNotifications(unreadOnly)`, `markNotificationRead(id,all)`.

## Extras keys

`Constants.EXTRA_STUDENT_ID`, `EXTRA_MATERIAL_ID`, `EXTRA_LOAN_ID`, `EXTRA_INTERNSHIP_ID`, `EXTRA_APPLICATION_ID`, `EXTRA_STATUS`, `EXTRA_MODE` ("add"/"edit"), `EXTRA_TITLE`.

`loanDetails` returns only the loan; use `listLoans`/`materialDetails` for joined names. `studentDetails` returns `data.student`, `data.loans`, `data.applications`.
