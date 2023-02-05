package com.qd.filenetDeleter.controller

import com.qd.filenetDeleter.service.FileNetP8Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/delete")
class DeleteFileNetFolderController(
    private val fileNetP8Service: FileNetP8Service
) {
    @PostMapping
    fun deleteFolderRecursive(
        @RequestBody deleteRequestPath: DeleteRequestPath
    ) {
        fileNetP8Service.deleteFileNetFolder(deleteRequestPath)
    }

    @PostMapping
    fun deleteFolderRecursiveFromSearch(@RequestBody deleteRequestSQL: DeleteRequestSQL) {
        fileNetP8Service.deleteFileNetFolderViaSearch(deleteRequestSQL)
    }
}

class DeleteRequestPath(
    val objectStoreName: String, val path: String
)

class DeleteRequestSQL(
    val objectStoreName: String, val searchSQL: String
)