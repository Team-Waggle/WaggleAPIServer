package io.waggle.waggleapiserver.common.storage

interface StorageClient {
    fun generateUploadUrl(directory: String, contentType: ImageContentType): GeneratedUploadUrl
    fun delete(objectUrl: String)
}
