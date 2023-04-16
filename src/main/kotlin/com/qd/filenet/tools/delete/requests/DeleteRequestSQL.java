package com.qd.filenet.tools.delete.requests;

public record DeleteRequestSQL(String objectStoreName, String searchSQL, String wsiUrl, String username, String password) {
}
