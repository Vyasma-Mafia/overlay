package com.stoum.overlay.config

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "s3", ignoreUnknownFields = true)
data class ObjectStorageConfig(
    val accessKeyId: String,
    val secretAccessKey: String,
    val region: String,
    val endpoint: String,
    val bucket: String
) {
    @Bean
    fun objectStorage(): S3Client {
        return S3Client {
            region = this@ObjectStorageConfig.region
            endpointUrl = Url.parse(this@ObjectStorageConfig.endpoint)
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = this@ObjectStorageConfig.accessKeyId
                secretAccessKey = this@ObjectStorageConfig.secretAccessKey
            }
        }
    }
}
