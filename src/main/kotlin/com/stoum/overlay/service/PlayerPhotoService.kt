package com.stoum.overlay.service

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromInputStream
import com.stoum.overlay.entity.Player
import com.stoum.overlay.entity.PlayerPhoto
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.enums.PhotoType
import com.stoum.overlay.repository.PlayerPhotoRepository
import com.stoum.overlay.repository.PlayerRepository
import jakarta.transaction.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.util.UUID

const val DEFAULT_PHOTO_URL = "https://storage.yandexcloud.net/mafia-photos/null.jpg"

@Service
class PlayerPhotoService(
    val playerRepository: PlayerRepository,
    val playerPhotoRepository: PlayerPhotoRepository,
    val objectStorage: S3Client,
    val s3TransferManager: S3TransferManager
) {

    @Value("\${s3.bucket.name}")
    private lateinit var bucketName: String

    @Value("\${s3.endpoint}")
    private lateinit var baseUrl: String


    fun getPlayerPhotoForCompetitionRole(
        player: Player,
        tournamentType: GameType,
        tournamentId: Long,
        role: String?
    ): PlayerPhoto? {
        val existingPlayerPhotos = player.playerPhotos.filter { !it.deleted }
        val firstPhoto = existingPlayerPhotos.find {
            it.tournamentType == tournamentType
                && tournamentId == it.tournamentId
                && it.type == roleToPhotoType(role)
        }
        if (firstPhoto != null) {
            return firstPhoto
        }
        val secondPhoto = existingPlayerPhotos.find {
            it.tournamentType == tournamentType
                && tournamentId == it.tournamentId
                && it.type == PhotoType.MAIN
        }
        if (secondPhoto != null) {
            return secondPhoto
        }
        val thirdPhoto = existingPlayerPhotos.find {
            it.tournamentType == null && it.tournamentId == null
                && it.type == roleToPhotoType(role)
        }
        if (thirdPhoto != null) {
            return thirdPhoto
        }
        val fourthPhoto = existingPlayerPhotos.find {
            it.tournamentType == null && it.tournamentId == null
                && it.type == PhotoType.MAIN
        }
        if (fourthPhoto != null) {
            return fourthPhoto
        }
        return null
    }

    fun getPlayerPhotoUrlForPlayerCompetitionRole(
        playerId: Long,
        tournamentType: GameType,
        tournamentId: Long,
        role: String?
    ): String {
        val player = when (tournamentType) {
            GameType.POLEMICA -> playerRepository.findPlayerByPolemicaId(playerId)
            GameType.GOMAFIA -> playerRepository.findPlayerByGomafiaId(playerId)
            GameType.CUSTOM -> null
        }
        if (player != null) {
            val playerPhoto = getPlayerPhotoForCompetitionRole(player, tournamentType, tournamentId, role)
            if (playerPhoto != null) {
                return playerPhoto.url
            }
        }
        return when (tournamentType) {
            GameType.POLEMICA -> "https://storage.yandexcloud.net/mafia-photos/${playerId}.jpg"
            GameType.GOMAFIA -> "https://storage.yandexcloud.net/mafia-photos/gomafia/${playerId}.jpg"
            GameType.CUSTOM -> DEFAULT_PHOTO_URL
        }
    }

    @Transactional
    suspend fun addPlayerPhoto(
        playerId: UUID,
        photoFile: MultipartFile,
        photoType: PhotoType?,
        tournamentType: GameType?,
        tournamentId: Long?
    ): Player = withContext(Dispatchers.IO) {
        // 1. Находим игрока или выбрасываем исключение
        val player = playerRepository.findById(playerId)
            .orElseThrow { PlayerNotFoundException("Player with id $playerId not found") } // Используйте свое исключение

        // 2. Генерируем уникальное имя для файла

        val newPlayerPhoto = playerPhotoRepository.save(
            PlayerPhoto(
                id = null,
                url = DEFAULT_PHOTO_URL,
                type = photoType ?: PhotoType.MAIN,
                tournamentId = tournamentId,
                tournamentType = tournamentType,
                deleted = false
            )
        )

        val photoId = newPlayerPhoto.id
        val extension = StringUtils.getFilenameExtension(photoFile.originalFilename)
        val objectKey = "$photoId"

        // 3. Загружаем файл в Object Storage
        try {

            val putObjectRequest = PutObjectRequest {
                bucket = bucketName
                key = objectKey
                body = ByteStream.fromInputStream(photoFile.inputStream)
                contentType = extension
                contentLength = photoFile.size
                metadata = mapOf(
                    "playerId" to playerId.toString(),
                    "photoType" to photoType.toString(),
                    "tournamentId" to tournamentId.toString(),
                    "tournamentType" to tournamentType.toString(),
                )
            }
            s3TransferManager.upload(UploadRequest.builder()
                .putObjectRequest {
                    it.bucket(bucketName)
                        .key(objectKey)
                        .contentType(extension)
                        .contentLength(photoFile.size)
                        .metadata(
                            mapOf(
                                "playerId" to playerId.toString(),
                                "photoType" to photoType.toString(),
                                "tournamentId" to tournamentId.toString(),
                                "tournamentType" to tournamentType.toString(),
                            )
                        )
                }
                .requestBody(AsyncRequestBody.fromBytes(photoFile.inputStream.readAllBytes()))
                .build())
                .completionFuture().join()

            // objectStorage.putObject(putObjectRequest)
            // val upload = objectStorage.createMultipartUpload(CreateMultipartUploadRequest {
            //     bucket = bucketName
            //     key = objectKey
            //     contentType = extension
            //     metadata = mapOf(
            //         "playerId" to playerId.toString(),
            //         "photoType" to photoType.toString(),
            //         "tournamentId" to tournamentId.toString(),
            //         "tournamentType" to tournamentType.toString(),
            //     )
            // })
            // val bytes = photoFile.inputStream.readAllBytes()
            // objectStorage.uploadPart(
            //     UploadPartRequest {
            //         bucket = bucketName
            //         key = objectKey
            //         partNumber = 1
            //         uploadId = upload.uploadId
            //         body = ByteStream.fromBytes(bytes)
            //         contentLength = bytes.size.toLong()
            //     }
            // )
            // objectStorage.completeMultipartUpload(CompleteMultipartUploadRequest {
            //     bucket = bucketName
            //     key = objectKey
            //     uploadId = upload.uploadId
            // })
        } catch (e: Exception) {
            // Если загрузка не удалась, транзакция откатится, и данные в БД не сохранятся
            throw S3UploadException("Failed to upload photo to S3", e) // Ваше кастомное исключение
        }

        // 4. Создаем запись о фото в БД
        val photoUrl = "$baseUrl/$bucketName/$objectKey"
        newPlayerPhoto.url = photoUrl

        player.playerPhotos
            .filter {
                it.tournamentType == newPlayerPhoto.tournamentType
                    && it.tournamentId == newPlayerPhoto.tournamentId
                    && it.type == newPlayerPhoto.type
            }
            .forEach { it.deleted = true }

        player.playerPhotos.add(newPlayerPhoto)

        // 5. Сохраняем обновленную сущность игрока
        return@withContext playerRepository.save(player)
    }

    private fun roleToPhotoType(role: String?): PhotoType = when (role) {
        "don" -> PhotoType.DON
        "black" -> PhotoType.BLACK
        "red" -> PhotoType.RED
        "sher" -> PhotoType.SHER
        else -> PhotoType.MAIN
    }

    // Определите кастомные исключения где-нибудь в вашем проекте
    class PlayerNotFoundException(message: String) : RuntimeException(message)
    class S3UploadException(message: String, cause: Throwable) : RuntimeException(message, cause)
}
