package com.example.myapplication__volume.ui.login;

import androidx.annotation.Nullable;

/**
 * Authentication result : success (user details) or error message.
 */
class LoginResult {
    @Nullable
    private LoggedInUserView success;
//    @Nullable
//    private Integer error;

//    LoginResult(@Nullable Integer error) {
//        this.error = error;
//    }

//    @Nullable
//    Integer getError() {
//        return error;
//    }

    @Nullable
    private String error;

    LoginResult(@Nullable String error) {
        this.error = error;
    }

    LoginResult(@Nullable LoggedInUserView success) {
        this.success = success;
    }

    @Nullable
    LoggedInUserView getSuccess() {
        return success;
    }

    @Nullable
    String getError() {
        return error;
    }
}