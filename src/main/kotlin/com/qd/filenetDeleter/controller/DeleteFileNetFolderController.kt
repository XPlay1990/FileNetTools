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
        @RequestBody folderPath: String
    ) {
        fileNetP8Service.deleteFileNetFolder(folderPath)
    }
    @PostMapping
    fun deleteFolderRecursiveFromSearch(
        @RequestBody searchSQL: String
    ) {
        fileNetP8Service.deleteFileNetFolderViaSearch(searchSQL)
    }
}