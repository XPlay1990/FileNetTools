package com.qd.filenet.tools.delete

import com.qd.filenet.tools.delete.requests.DeleteRequestPath
import com.qd.filenet.tools.delete.requests.DeleteRequestSQL
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/delete")
class DeleteFileNetFolderController(
    private val fileNetService: FileNetService
) {
    @PostMapping
    fun deleteFolderRecursive(
        @RequestBody deleteRequestPath: DeleteRequestPath
    ) {
        fileNetService.deleteFileNetFolder(deleteRequestPath)
    }

    @PostMapping("/sql")
    fun deleteFolderRecursiveFromSearch(@RequestBody deleteRequestSQL: DeleteRequestSQL) {
        fileNetService.deleteFileNetFolderViaSearch(deleteRequestSQL)
    }
}