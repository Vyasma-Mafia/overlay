package com.stoum.overlay.service

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import com.stoum.overlay.entity.Player
import com.stoum.overlay.entity.PlayerPhoto
import com.stoum.overlay.entity.enums.PhotoType
import com.stoum.overlay.repository.PlayerPhotoRepository
import com.stoum.overlay.repository.PlayerRepository
import jakarta.transaction.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.DownloadRequest
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.nio.file.Files
import java.util.UUID
import com.stoum.overlay.service.DEFAULT_PHOTO_URL

@Service
class PhotoMigrationService(
    val playerRepository: PlayerRepository,
    val playerPhotoRepository: PlayerPhotoRepository,
    val objectStorage: S3Client,
    val s3TransferManager: S3TransferManager
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Value("\${s3.bucket.name}")
    private lateinit var newBucketName: String

    @Value("\${s3.endpoint}")
    private lateinit var baseUrl: String

    private val oldBucketName = "mafia-photos"

    class MigrationResult {
        var totalPhotos: Int = 0
        var migrated: Int = 0
        var skipped: Int = 0
        var errors: Int = 0
        val errorDetails: MutableList<String> = mutableListOf()
    }

    /**
     * Migrates all photos from old S3 bucket (mafia-photos) to new system (mafoverlay-photos + DB)
     * Old structure:
     * - {polemica_id}.jpg - photos by polemica ID
     * - gomafia/{gomafia_id}.jpg - photos by gomafia ID
     */
    @Transactional
    suspend fun migratePhotos(): MigrationResult = withContext(Dispatchers.IO) {
        log.info("Starting photo migration from $oldBucketName to $newBucketName")
        
        val result = MigrationResult()
        var continuationToken: String? = null

        do {
            val listRequest = ListObjectsV2Request {
                bucket = oldBucketName
                continuationToken?.let { this.continuationToken = it }
            }

            val listResponse = objectStorage.listObjectsV2(listRequest)
            val objects = listResponse.contents ?: emptyList()

            result.totalPhotos += objects.size

            for (obj in objects) {
                val key = obj.key ?: continue
                
                try {
                    when {
                        key.startsWith("gomafia/") -> {
                            // gomafia/{id}.jpg
                            val gomafiaId = extractIdFromKey(key.removePrefix("gomafia/"))
                            if (gomafiaId != null) {
                                migratePhotoForGomafiaPlayer(gomafiaId, key, result)
                            } else {
                                result.skipped++
                                log.warn("Could not extract gomafia ID from key: $key")
                            }
                        }
                        key.endsWith(".jpg") && !key.contains("/") -> {
                            // {polemica_id}.jpg
                            val polemicaId = extractIdFromKey(key)
                            if (polemicaId != null) {
                                migratePhotoForPolemicaPlayer(polemicaId, key, result)
                            } else {
                                result.skipped++
                                log.warn("Could not extract polemica ID from key: $key")
                            }
                        }
                        else -> {
                            result.skipped++
                            log.debug("Skipping non-photo file: $key")
                        }
                    }
                } catch (e: Exception) {
                    result.errors++
                    val errorMsg = "Error processing $key: ${e.message}"
                    result.errorDetails.add(errorMsg)
                    log.error(errorMsg, e)
                }
            }

            continuationToken = listResponse.nextContinuationToken
        } while (continuationToken != null)

        log.info("Migration completed: ${result.migrated} migrated, ${result.skipped} skipped, ${result.errors} errors")
        result
    }

    private suspend fun migratePhotoForPolemicaPlayer(
        polemicaId: Long,
        oldKey: String,
        result: MigrationResult
    ) {
        val player = playerRepository.findPlayerByPolemicaId(polemicaId)
        if (player == null) {
            result.skipped++
            log.debug("Player with polemicaId $polemicaId not found, skipping $oldKey")
            return
        }

        // Check if player already has photos in new system
        if (hasPhotosInNewSystem(player)) {
            result.skipped++
            log.debug("Player ${player.id} already has photos in new system, skipping $oldKey")
            return
        }

        // Copy photo and create DB entry
        copyPhotoAndCreateEntry(player, oldKey, result)
    }

    private suspend fun migratePhotoForGomafiaPlayer(
        gomafiaId: Long,
        oldKey: String,
        result: MigrationResult
    ) {
        val player = playerRepository.findPlayerByGomafiaId(gomafiaId)
        if (player == null) {
            result.skipped++
            log.debug("Player with gomafiaId $gomafiaId not found, skipping $oldKey")
            return
        }

        // Check if player already has photos in new system
        if (hasPhotosInNewSystem(player)) {
            result.skipped++
            log.debug("Player ${player.id} already has photos in new system, skipping $oldKey")
            return
        }

        // Copy photo and create DB entry
        copyPhotoAndCreateEntry(player, oldKey, result)
    }

    private fun hasPhotosInNewSystem(player: Player): Boolean {
        // Check if player has any non-deleted photos in DB
        val hasDbPhotos = player.playerPhotos.any { !it.deleted }
        if (hasDbPhotos) {
            return true
        }

        // Also check if photos exist in new S3 bucket (by checking if any photo URL points to new bucket)
        // This is a safety check in case DB is out of sync
        return player.playerPhotos.any { 
            it.url.contains(newBucketName) && !it.deleted 
        }
    }

    private suspend fun copyPhotoAndCreateEntry(
        player: Player,
        oldKey: String,
        result: MigrationResult
    ) {
        try {
            // 1. Download photo from old bucket using S3TransferManager
            val tempFile = Files.createTempFile("photo-migration-", ".jpg")
            val photoBytes: ByteArray
            try {
                val downloadRequest = DownloadRequest.builder()
                    .getObjectRequest { req ->
                        req.bucket(oldBucketName)
                        req.key(oldKey)
                    }
                    .responseTransformer(AsyncResponseTransformer.toFile(tempFile))
                    .build()

                s3TransferManager.download(downloadRequest).completionFuture().join()
                photoBytes = Files.readAllBytes(tempFile)

                if (photoBytes.isEmpty()) {
                    result.errors++
                    log.error("Failed to download photo from $oldBucketName/$oldKey: empty response")
                    return
                }
            } finally {
                Files.deleteIfExists(tempFile)
            }

            // 2. Create DB entry first
            val newPlayerPhoto = playerPhotoRepository.save(
                PlayerPhoto(
                    id = null,
                    url = DEFAULT_PHOTO_URL, // Will be updated after upload
                    type = PhotoType.MAIN,
                    tournamentId = null,
                    tournamentType = null,
                    deleted = false
                )
            )

            val photoId = newPlayerPhoto.id!!
            val objectKey = photoId.toString()

            // 3. Upload to new bucket
            try {
                s3TransferManager.upload(
                    UploadRequest.builder()
                        .putObjectRequest {
                            it.bucket(newBucketName)
                                .key(objectKey)
                                .contentType("image/jpeg")
                                .contentLength(photoBytes.size.toLong())
                                .metadata(
                                    mapOf(
                                        "playerId" to player.id.toString(),
                                        "photoType" to PhotoType.MAIN.toString(),
                                        "migratedFrom" to "$oldBucketName/$oldKey"
                                    )
                                )
                        }
                        .requestBody(AsyncRequestBody.fromBytes(photoBytes))
                        .build()
                ).completionFuture().join()
            } catch (e: Exception) {
                // Rollback DB entry if upload fails
                playerPhotoRepository.delete(newPlayerPhoto)
                throw e
            }

            // 4. Update photo URL
            val photoUrl = "$baseUrl/$newBucketName/$objectKey"
            newPlayerPhoto.url = photoUrl
            playerPhotoRepository.save(newPlayerPhoto)

            // 5. Add photo to player and save
            player.playerPhotos.add(newPlayerPhoto)
            playerRepository.save(player)

            result.migrated++
            log.info("Successfully migrated photo for player ${player.id} (${player.nickname}) from $oldKey")
        } catch (e: Exception) {
            result.errors++
            val errorMsg = "Failed to migrate photo $oldKey for player ${player.id}: ${e.message}"
            result.errorDetails.add(errorMsg)
            log.error(errorMsg, e)
            // Don't rethrow - error is already counted and logged
            // This prevents double-counting in the outer catch block
        }
    }

    private fun extractIdFromKey(key: String): Long? {
        // Remove .jpg extension and try to parse as Long
        val idString = key.removeSuffix(".jpg").removeSuffix(".JPG")
        return idString.toLongOrNull()
    }
}

