package com.qd.filenet.tools.create

import com.qd.filenet.tools.create.requests.CreateMarkingsRequest
import com.qd.filenet.tools.create.requests.CreateTaskInboxesRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/create")
class CreateController(
    private val fileNetCreateService: FileNetCreateService
) {
    @PostMapping("/taskInboxes")
    fun createTaskInboxes(
        @RequestBody createRequest: CreateTaskInboxesRequest
    ): ResponseEntity<String> {
        fileNetCreateService.createTaskInboxes(createRequest)

        return ResponseEntity(
            "Success", HttpStatus.OK
        )
    }

    @PostMapping("/markings")
    fun createMarkingValues(@RequestBody createMarkingsRequest: CreateMarkingsRequest): ResponseEntity<String> {
        fileNetCreateService.createMarkings(createMarkingsRequest)

        return ResponseEntity(
            "Success", HttpStatus.OK
        )
    }
}