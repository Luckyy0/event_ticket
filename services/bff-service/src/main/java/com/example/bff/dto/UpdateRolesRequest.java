package com.example.bff.dto;

import java.util.List;

public class UpdateRolesRequest {
    private List<String> roles;

    public UpdateRolesRequest() {}

    public UpdateRolesRequest(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
