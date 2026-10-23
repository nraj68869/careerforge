package com.careerforge.backend.dto;

import com.careerforge.backend.entity.ApprovalStatus;
import com.careerforge.backend.entity.Role;
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;
    private ApprovalStatus approvalStatus;

    public UserResponse(){

    }
    
    public UserResponse(Long id, String name, String email, Role role, boolean enabled, ApprovalStatus approvalStatus){
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;  
        this.enabled = enabled;     
        this.approvalStatus = approvalStatus;
    }
    public long getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String getEmail(){
        return email;
    }
    public Role getRole(){
        return role;
    }
    public boolean isEnabled(){
        return enabled;
    }
    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }
}
