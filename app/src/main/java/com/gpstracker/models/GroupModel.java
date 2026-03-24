package com.gpstracker.models;

import java.util.HashMap;
import java.util.Map;

public class GroupModel {

    private String groupId;
    private String groupName;
    private String groupCode;
    private String groupPassword;
    private String adminId;
    private Map<String, Boolean> members = new HashMap<>(); // memberId -> true

    public boolean isSharingLocation = false; // Transient parameter for UI toggle

    public GroupModel() {
    }

    public GroupModel(String groupId, String groupName, String groupCode, String groupPassword, String adminId) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupCode = groupCode;
        this.groupPassword = groupPassword;
        this.adminId = adminId;
    }

    // Getters and Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }

    public String getGroupPassword() { return groupPassword; }
    public void setGroupPassword(String groupPassword) { this.groupPassword = groupPassword; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public Map<String, Boolean> getMembers() { return members; }
    public void setMembers(Map<String, Boolean> members) { this.members = members; }
}