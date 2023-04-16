package com.qd.filenet.tools.create.requests;

import java.util.List;

public record CreateMarkingsRequest(ConnectionData connectionData, String markingSetId, String securityTemplate,
                                    List<String> markingList) {

}
