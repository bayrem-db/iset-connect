package com.iset.connect.network;

import com.iset.connect.models.ApiData;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Complete typed ISET Connect REST API. Paths are relative to BASE_URL (…/backend/api/). */
public interface ApiService {

    // ---------- Authentication ----------
    @FormUrlEncoded
    @POST("auth/register.php")
    Call<ApiResponse<Object>> register(
            @Field("first_name") String firstName,
            @Field("last_name") String lastName,
            @Field("cin") String cin,
            @Field("email") String email,
            @Field("password") String password,
            @Field("confirm_password") String confirmPassword,
            @Field("class_name") String className);

    @FormUrlEncoded
    @POST("auth/login.php")
    Call<ApiResponse<ApiData.LoginData>> login(
            @Field("email") String email,
            @Field("password") String password);

    @POST("auth/logout.php")
    Call<ApiResponse<Object>> logout();

    @GET("auth/profile.php")
    Call<ApiResponse<ApiData.UserData>> getProfile();

    @FormUrlEncoded
    @POST("auth/profile.php")
    Call<ApiResponse<ApiData.UserData>> updateProfile(
            @Field("first_name") String firstName,
            @Field("last_name") String lastName,
            @Field("class_name") String className,
            @Field("email") String email);

    @FormUrlEncoded
    @POST("auth/change_password.php")
    Call<ApiResponse<Object>> changePassword(
            @Field("current_password") String currentPassword,
            @Field("new_password") String newPassword,
            @Field("confirm_password") String confirmPassword);

    // ---------- Dashboards ----------
    @GET("dashboard/admin.php")
    Call<ApiResponse<ApiData.AdminStats>> adminStats();

    @GET("dashboard/student.php")
    Call<ApiResponse<ApiData.StudentStats>> studentStats();

    // ---------- Students (admin) ----------
    @GET("students/list.php")
    Call<ApiResponse<ApiData.StudentListData>> listStudents(
            @Query("q") String query,
            @Query("status") String status);

    @GET("students/details.php")
    Call<ApiResponse<ApiData.StudentDetailsData>> studentDetails(@Query("id") long id);

    @FormUrlEncoded
    @POST("students/approve.php")
    Call<ApiResponse<Object>> approveStudent(@Field("id") long id);

    @FormUrlEncoded
    @POST("students/reject.php")
    Call<ApiResponse<Object>> rejectStudent(@Field("id") long id, @Field("reason") String reason);

    @FormUrlEncoded
    @POST("students/suspend.php")
    Call<ApiResponse<Object>> suspendStudent(@Field("id") long id, @Field("reason") String reason);

    @FormUrlEncoded
    @POST("students/reactivate.php")
    Call<ApiResponse<Object>> reactivateStudent(@Field("id") long id);

    // ---------- Materials ----------
    @GET("materials/list.php")
    Call<ApiResponse<ApiData.MaterialListData>> listMaterials(
            @Query("q") String query,
            @Query("category") String category);

    @GET("materials/details.php")
    Call<ApiResponse<ApiData.MaterialData>> materialDetails(@Query("id") long id);

    @FormUrlEncoded
    @POST("materials/create.php")
    Call<ApiResponse<Object>> createMaterial(
            @Field("name") String name,
            @Field("description") String description,
            @Field("category") String category,
            @Field("total_quantity") String totalQuantity,
            @Field("image_url") String imageUrl);

    @FormUrlEncoded
    @POST("materials/update.php")
    Call<ApiResponse<Object>> updateMaterial(
            @Field("id") long id,
            @Field("name") String name,
            @Field("description") String description,
            @Field("category") String category,
            @Field("total_quantity") String totalQuantity,
            @Field("image_url") String imageUrl);

    @FormUrlEncoded
    @POST("materials/delete.php")
    Call<ApiResponse<Object>> deleteMaterial(@Field("id") long id);

    // ---------- Loans ----------
    @FormUrlEncoded
    @POST("loans/create.php")
    Call<ApiResponse<Object>> createLoan(
            @Field("material_id") long materialId,
            @Field("quantity") String quantity,
            @Field("borrowing_date") String borrowingDate,
            @Field("expected_return_date") String expectedReturnDate);

    @GET("loans/list.php")
    Call<ApiResponse<ApiData.LoanListData>> listLoans(
            @Query("q") String query,
            @Query("status") String status,
            @Query("scope") String scope);

    @GET("loans/details.php")
    Call<ApiResponse<ApiData.LoanData>> loanDetails(@Query("id") long id);

    @FormUrlEncoded
    @POST("loans/approve.php")
    Call<ApiResponse<Object>> approveLoan(@Field("id") long id);

    @FormUrlEncoded
    @POST("loans/reject.php")
    Call<ApiResponse<Object>> rejectLoan(@Field("id") long id, @Field("reason") String reason);

    @FormUrlEncoded
    @POST("loans/cancel.php")
    Call<ApiResponse<Object>> cancelLoan(@Field("id") long id);

    @FormUrlEncoded
    @POST("loans/return.php")
    Call<ApiResponse<Object>> returnLoan(@Field("id") long id);

    // ---------- Internships ----------
    @GET("internships/list.php")
    Call<ApiResponse<ApiData.InternshipListData>> listInternships(
            @Query("q") String query,
            @Query("domain") String domain,
            @Query("status") String status);

    @GET("internships/details.php")
    Call<ApiResponse<ApiData.InternshipData>> internshipDetails(@Query("id") long id);

    @FormUrlEncoded
    @POST("internships/create.php")
    Call<ApiResponse<Object>> createInternship(
            @Field("title") String title,
            @Field("company_name") String companyName,
            @Field("company_address") String companyAddress,
            @Field("domain") String domain,
            @Field("description") String description,
            @Field("required_skills") String requiredSkills,
            @Field("start_date") String startDate,
            @Field("end_date") String endDate,
            @Field("application_deadline") String deadline,
            @Field("total_positions") String totalPositions,
            @Field("status") String status);

    @FormUrlEncoded
    @POST("internships/update.php")
    Call<ApiResponse<Object>> updateInternship(
            @Field("id") long id,
            @Field("title") String title,
            @Field("company_name") String companyName,
            @Field("company_address") String companyAddress,
            @Field("domain") String domain,
            @Field("description") String description,
            @Field("required_skills") String requiredSkills,
            @Field("start_date") String startDate,
            @Field("end_date") String endDate,
            @Field("application_deadline") String deadline,
            @Field("total_positions") String totalPositions,
            @Field("status") String status);

    @FormUrlEncoded
    @POST("internships/delete.php")
    Call<ApiResponse<Object>> deleteInternship(@Field("id") long id);

    @FormUrlEncoded
    @POST("internships/publish.php")
    Call<ApiResponse<Object>> publishInternship(@Field("id") long id);

    @FormUrlEncoded
    @POST("internships/close.php")
    Call<ApiResponse<Object>> closeInternship(@Field("id") long id);

    // ---------- Applications ----------
    @FormUrlEncoded
    @POST("applications/create.php")
    Call<ApiResponse<Object>> createApplication(
            @Field("internship_id") long internshipId,
            @Field("motivation_message") String motivationMessage);

    @GET("applications/list.php")
    Call<ApiResponse<ApiData.ApplicationListData>> listApplications(
            @Query("q") String query,
            @Query("status") String status,
            @Query("internship_id") Long internshipId);

    @FormUrlEncoded
    @POST("applications/accept.php")
    Call<ApiResponse<Object>> acceptApplication(@Field("id") long id);

    @FormUrlEncoded
    @POST("applications/reject.php")
    Call<ApiResponse<Object>> rejectApplication(@Field("id") long id, @Field("reason") String reason);

    @FormUrlEncoded
    @POST("applications/cancel.php")
    Call<ApiResponse<Object>> cancelApplication(@Field("id") long id);

    // ---------- Notifications ----------
    @GET("notifications/list.php")
    Call<ApiResponse<ApiData.NotificationListData>> listNotifications(@Query("unread_only") Integer unreadOnly);

    @FormUrlEncoded
    @POST("notifications/mark_read.php")
    Call<ApiResponse<Object>> markNotificationRead(@Field("id") Long id, @Field("all") Integer all);
}
