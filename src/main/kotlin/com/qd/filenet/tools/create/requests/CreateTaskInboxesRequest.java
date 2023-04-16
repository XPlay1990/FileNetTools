package com.qd.filenet.tools.create.requests;

import java.util.List;

public record CreateTaskInboxesRequest(ConnectionData connectionData, TaskBox securityTemplate,
                                       List<TaskBox> taskBoxList) {

}

