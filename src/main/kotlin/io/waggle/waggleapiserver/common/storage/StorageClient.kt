package io.waggle.waggleapiserver.common.storage

import java.util.UUID

interface StorageClient {
    fun generateUploadUrl(
        directory: ImageDirectory,
        uploaderId: UUID,
        contentType: ImageContentType,
    ): GeneratedUploadUrl

    fun isUploadedBy(
        objectUrl: String,
        directory: ImageDirectory,
        uploaderId: UUID,
    ): Boolean

    fun delete(objectUrl: String)
}
