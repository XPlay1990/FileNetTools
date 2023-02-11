package com.qd.filenetDeleter.service;

import com.filenet.api.authentication.SubjectCredentials;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.*;
import com.filenet.api.property.PropertyFilter;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.UserContext;
import com.qd.filenetDeleter.util.DeleteRequestPath;
import com.qd.filenetDeleter.util.DeleteRequestSQL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileNetP8Service {

    private final Logger logger = LoggerFactory.getLogger(FileNetP8Service.class);

    @Value("${filenet.username}")
    private String username;
    @Value("${filenet.password}")
    private String password;
    @Value("${filenet.baseUrl}")
    private String baseUrl;

    private Connection connection;
    private final PropertyFilter emptyPropertyFilter = new PropertyFilter();

    public void deleteFileNetFolder(DeleteRequestPath deleteRequestPath) throws IllegalAccessException {
        String folderPath = deleteRequestPath.path();
        if (folderPath.equals("/") || folderPath.equals("/Dossier")) {
            throw new IllegalAccessException("Cannot Delete starting from " + folderPath);
        }

        Subject subject = getSubject();

        SubjectCredentials subjectCredentials = new SubjectCredentials(subject);
        FolderDeleter folderDeleter = new FolderDeleter(deleteRequestPath.objectStoreName(), deleteRequestPath.path());
        subjectCredentials.doAs(folderDeleter);
    }

    public void deleteFileNetFolderViaSearch(DeleteRequestSQL deleteRequestSQL) {
        logger.info("Deleting folders via search");
        Subject subject = getSubject();

        SubjectCredentials subjectCredentials = new SubjectCredentials(subject);
        FolderDeleterViaSearch folderDeleterViaSearch = new FolderDeleterViaSearch(deleteRequestSQL.objectStoreName(), deleteRequestSQL.searchSQL());
        subjectCredentials.doAs(folderDeleterViaSearch);
    }

    private Subject getSubject() {
        log.debug("Authenticating in P8");
        connection = Factory.Connection.getConnection(baseUrl);
        return UserContext.createSubject(connection, username, password, null);
    }

    private void deleteSubFoldersRecursively(Folder folder, UpdatingBatch updatingBatchInstance) {
        FolderSet subFolders = folder.get_SubFolders();
        Iterator iterator = subFolders.iterator();
        while (iterator.hasNext()) {
            Folder subFolder = (Folder) iterator.next();
            deleteSubFoldersRecursively(subFolder, updatingBatchInstance);
            deleteDocuments(subFolder, updatingBatchInstance);
            logger.info("Adding folder {} to delete", subFolder.get_PathName());
            subFolder.delete();
            updatingBatchInstance.add(subFolder, null);
        }
    }

    private void deleteDocuments(Folder subFolder, UpdatingBatch updatingBatchInstance) {
        DocumentSet containedDocuments = subFolder.get_ContainedDocuments();
        Iterator documentIterator = containedDocuments.iterator();
        while (documentIterator.hasNext()) {
            Document document = (Document) documentIterator.next();
            logger.info("Adding document {} to delete", document.get_Name());
            document.delete();
            updatingBatchInstance.add(document, null);
        }
    }

    @RequiredArgsConstructor
    private class FolderDeleter implements PrivilegedExceptionAction<Object> {
        private final String objectStoreName;

        private final String folderPath;

        @Override
        public Object run() {
            EntireNetwork entireNetwork = Factory.EntireNetwork.fetchInstance(connection, emptyPropertyFilter);
            Domain domain = entireNetwork.get_LocalDomain();
            UpdatingBatch updatingBatchInstance = UpdatingBatch.createUpdatingBatchInstance(domain, RefreshMode.REFRESH);
            ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain, objectStoreName, emptyPropertyFilter);

            Folder startFolder = Factory.Folder.fetchInstance(objectStore, folderPath, null);

            deleteSubFoldersRecursively(startFolder, updatingBatchInstance);
            deleteDocuments(startFolder, updatingBatchInstance);

            logger.info("Adding folder {} to delete", startFolder.get_PathName());
            startFolder.delete();
            updatingBatchInstance.add(startFolder, null);

            logger.info("Executing batch update");
            updatingBatchInstance.updateBatch();

            return null;
        }
    }

    @RequiredArgsConstructor
    private class FolderDeleterViaSearch implements PrivilegedExceptionAction<Object> {
        private final String objectStoreName;
        private final String queryString;

        @Override
        public Object run() {
            EntireNetwork entireNetwork = Factory.EntireNetwork.fetchInstance(connection, emptyPropertyFilter);
            Domain domain = entireNetwork.get_LocalDomain();
            UpdatingBatch updatingBatchInstance = UpdatingBatch.createUpdatingBatchInstance(domain, RefreshMode.REFRESH);
            ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain, objectStoreName, emptyPropertyFilter);

            logger.info("Searching for folders");
            SearchSQL searchSQL = new SearchSQL(queryString);
            SearchScope searchScope = new SearchScope(objectStore);
            IndependentObjectSet repositoryRowSet = searchScope.fetchObjects(searchSQL, null, null, true);
            Iterator iterator = repositoryRowSet.iterator();
            while (iterator.hasNext()) {
                Folder startFolder = (Folder) iterator.next();
                deleteSubFoldersRecursively(startFolder, updatingBatchInstance);
                deleteDocuments(startFolder, updatingBatchInstance);

                logger.info("Adding Folder {} to delete", startFolder.get_PathName());
                startFolder.delete();
                updatingBatchInstance.add(startFolder, null);
            }

            logger.info("Executing batch update");
            updatingBatchInstance.updateBatch();

            return null;
        }

    }
}
