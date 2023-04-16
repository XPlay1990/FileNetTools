package com.qd.filenet.tools.create;

import com.filenet.api.authentication.SubjectCredentials;
import com.filenet.api.collection.AccessPermissionList;
import com.filenet.api.collection.MarkingList;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.*;
import com.filenet.api.property.PropertyFilter;
import com.filenet.api.security.Marking;
import com.filenet.api.security.MarkingSet;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import com.qd.filenet.tools.create.requests.ConnectionData;
import com.qd.filenet.tools.create.requests.CreateMarkingsRequest;
import com.qd.filenet.tools.create.requests.CreateTaskInboxesRequest;
import com.qd.filenet.tools.create.requests.TaskBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileNetCreateService {

    private final Logger logger = LoggerFactory.getLogger(FileNetCreateService.class);

    private Connection connection;
    private final PropertyFilter emptyPropertyFilter = new PropertyFilter();


    private Subject getSubject(String baseUrl, String username, String password) {
        log.debug("Authenticating in P8");
        connection = Factory.Connection.getConnection(baseUrl);
        return UserContext.createSubject(connection, username, password, null);
    }

    public void createTaskInboxes(@NotNull CreateTaskInboxesRequest createRequest) {
        ConnectionData connectionData = createRequest.connectionData();
        Subject subject = getSubject(connectionData.wsiUrl(), connectionData.username(), connectionData.password());

        SubjectCredentials subjectCredentials = new SubjectCredentials(subject);
        TaskBoxCreator taskBoxCreator = new TaskBoxCreator(connectionData.objectStoreName(), createRequest.securityTemplate(), createRequest.taskBoxList());
        subjectCredentials.doAs(taskBoxCreator);
    }

    public void createMarkings(@NotNull CreateMarkingsRequest createMarkingsRequest) {
        ConnectionData connectionData = createMarkingsRequest.connectionData();
        Subject subject = getSubject(connectionData.wsiUrl(), connectionData.username(), connectionData.password());

        SubjectCredentials subjectCredentials = new SubjectCredentials(subject);
        MarkingsCreator markingsCreator = new MarkingsCreator(createMarkingsRequest.markingSetId(), createMarkingsRequest.securityTemplate(), createMarkingsRequest.markingList());
        subjectCredentials.doAs(markingsCreator);
    }

    @RequiredArgsConstructor
    private class TaskBoxCreator implements PrivilegedExceptionAction<Object> {
        private final String objectStoreName;
        private final TaskBox securityTemplate;
        private final List<TaskBox> taskBoxList;

        @Override
        public Object run() {
            EntireNetwork entireNetwork = Factory.EntireNetwork.fetchInstance(connection, emptyPropertyFilter);
            Domain domain = entireNetwork.get_LocalDomain();
            ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain, objectStoreName, emptyPropertyFilter);

            String securityTemplateFullPath = securityTemplate.path() + "/" + securityTemplate.name();
            logger.info("Fetching security template: " + securityTemplateFullPath);
            Folder securityTemplateFolder = Factory.Folder.fetchInstance(objectStore, securityTemplateFullPath, emptyPropertyFilter);
            AccessPermissionList permissions = securityTemplateFolder.get_Permissions();

            UpdatingBatch updatingBatchInstance = UpdatingBatch.createUpdatingBatchInstance(domain, RefreshMode.REFRESH);
            for (TaskBox taskBox : taskBoxList) {
                logger.info("Fetching parent folder: " + taskBox.path());
                Folder parentFolder = Factory.Folder.fetchInstance(objectStore, taskBox.path(), emptyPropertyFilter);
                String taskBoxFullPath = taskBox.path() + "/" + taskBox.name();
                logger.info("Creating task box: " + taskBoxFullPath);
                Folder inbox = Factory.Folder.createInstance(objectStore, "Folder", null);
                inbox.set_FolderName(taskBox.name());
                inbox.set_Parent(parentFolder);
                inbox.set_Permissions(permissions);
                updatingBatchInstance.add(inbox, null);
            }

            updatingBatchInstance.updateBatch();
            return null;
        }
    }

    @RequiredArgsConstructor
    private class MarkingsCreator implements PrivilegedExceptionAction<Object> {
        private final String markingSetId;
        private final String securityTemplate;
        private final List<String> markingsList;

        @Override
        public Object run() {
            EntireNetwork entireNetwork = Factory.EntireNetwork.fetchInstance(connection, emptyPropertyFilter);
            Domain domain = entireNetwork.get_LocalDomain();

            logger.info("Fetching marking set: " + markingSetId);
            MarkingSet markingSet = Factory.MarkingSet.fetchInstance(domain, new Id(markingSetId), emptyPropertyFilter);


            UpdatingBatch updatingBatchInstance = UpdatingBatch.createUpdatingBatchInstance(domain, RefreshMode.REFRESH);

            MarkingList markings = markingSet.get_Markings();
            Integer constraintMask = null;
            AccessPermissionList permissions = null;
            for (Object markingObject : markings) {
                Marking marking = (Marking) markingObject;
                if (Objects.equals(marking.get_MarkingValue(), securityTemplate)) {
                    logger.info("Found security template: " + marking.get_MarkingValue());
                    constraintMask = marking.get_ConstraintMask();
                    permissions = marking.get_Permissions();
                }
            }

            if (constraintMask == null || permissions == null) {
                logger.error("Security template not found");
                return null;
            }

            for (String markingToCreate : markingsList) {
                logger.info("Creating marking: " + markingToCreate);
                Marking markingInstance = Factory.Marking.createInstance();
                markingInstance.set_MarkingValue(markingToCreate);
                markingInstance.set_ConstraintMask(constraintMask);
                markingInstance.set_Permissions(permissions);

                markings.add(markingInstance);
            }
            updatingBatchInstance.add(markingSet, null);
            updatingBatchInstance.updateBatch();
            return null;
        }
    }
}
