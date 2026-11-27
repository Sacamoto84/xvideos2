package com.client.xvideos.l.repository

import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.graphQl.FavoriteAdd
import com.client.xvideos.l.net.graphQl.FavoriteRemove
import com.client.xvideos.l.net.graphQl.FavoritesByDatePicture
import com.client.xvideos.l.net.graphQl.FavoritesByDatePictureSet
import com.client.xvideos.l.net.graphQl.GraphQlRequest
import com.client.xvideos.l.net.graphQl.refreshMediaCategories
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.net.normalizePictureUrls
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

internal class LSessionRequiredException :
    IllegalStateException("Вход в L не выполнен: сервер не назвал пользователя сессии")

/**
 * Реализация репозитория серверных подписок и лайков L.
 *
 * Осуществляет взаимодействие с GraphQL API L для синхронизации
 * избранных альбомов и лайкнутых картинок пользователя, а также выполнения мутаций
 * добавления и удаления из избранного.
 *
 * @param repository Базовый репозиторий [Repository] для выполнения сетевых запросов.
 */
@Singleton
class LusciousServerFavoritesRepositoryImpl @Inject constructor(
    private val repository: Repository
) : LusciousServerFavoritesRepository {

    /** id пользователя и номер записи профиля, под которым сервер его назвал. */
    private class SessionUser(val id: String, val profileRevision: Int)

    private val sessionMutex = Mutex()
    private var sessionUser: SessionUser? = null

    /**
     * Идентификатор, подтверждённый сервером для текущего профиля.
     * Первая страница списка всегда подтверждает сессию заново.
     */
    override suspend fun getSessionUserId(): Result<String> = getSessionUserId(verify = false)

    private suspend fun getSessionUserId(verify: Boolean): Result<String> = sessionMutex.withLock {
        val revision = repository.profileRevision()
        if (!verify) {
            sessionUser?.takeIf { it.profileRevision == revision }?.let { return@withLock Result.success(it.id) }
        }
        sessionUser = null

        var userId = fetchSessionUserId().getOrElse { return@withLock Result.failure(it) }
        if (userId <= 0 && repository.handleRejectedSession(revision, retry = true)) {
            userId = fetchSessionUserId().getOrElse { return@withLock Result.failure(it) }
            if (userId <= 0) repository.handleRejectedSession(revision, retry = false)
        }
        if (revision != repository.profileRevision()) {
            return@withLock Result.failure(IllegalStateException("Профиль L изменился во время загрузки"))
        }
        if (userId <= 0) {
            return@withLock Result.failure(repository.sessionLoginNetworkFailure(revision) ?: LSessionRequiredException())
        }

        val id = userId.toString()
        sessionUser = SessionUser(id, revision)
        Timber.i("L session confirmed by GraphQL")
        Result.success(id)
    }

    private suspend fun fetchSessionUserId(): Result<Long> =
        refreshMediaCategories(repository, forceRefresh = true).map { it.filterSettings.userId }

    override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> {
        val targetUserId = if (!userId.isNullOrBlank()) {
            userId
        } else {
            val sessionResult = getSessionUserId(verify = page == 1)
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: IllegalStateException("Missing user_id"))
            }
            sessionResult.getOrThrow()
        }

        return FavoritesByDatePictureSet(
            repository = repository,
            userId = targetUserId,
            page = page
        )
    }

    override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> {
        val rawResult = getSubscribedAlbumsRaw(userId = null, page = page)
        if (rawResult.isFailure) {
            return Result.failure(rawResult.exceptionOrNull() ?: IllegalStateException("Request failed"))
        }

        return withContext(Dispatchers.Default) {
            runCatchingCancellable {
                val raw = rawResult.getOrThrow()
                val json = LJson.parseToJsonElement(raw).jsonObject
                val listByDate = json.objectAt("data", "favorite", "list_by_date")

                listByDate?.get("errors").errorMessagesOrNull("Unknown error")?.let { msg ->
                    throw IllegalStateException("Server error: $msg")
                }

                val items = listByDate.objectAt("picture_sets")?.get("items")

                if (items is JsonArray) {
                    LJson.decodeFromJsonElement<List<AlbumDetails>>(items)
                } else {
                    emptyList()
                }
            }
        }.onFailure { e ->
            Timber.e("Failed to parse subscribed albums response: ${e.javaClass.simpleName}")
        }
    }

    override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> {
        val targetUserId = if (!userId.isNullOrBlank()) {
            userId
        } else {
            val sessionResult = getSessionUserId(verify = page == 1)
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: IllegalStateException("Missing user_id"))
            }
            sessionResult.getOrThrow()
        }

        return FavoritesByDatePicture(
            repository = repository,
            userId = targetUserId,
            page = page
        )
    }

    override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> {
        val rawResult = getServerLikedPicturesRaw(userId = null, page = page)
        if (rawResult.isFailure) {
            return Result.failure(rawResult.exceptionOrNull() ?: IllegalStateException("Request failed"))
        }

        return withContext(Dispatchers.Default) {
            runCatchingCancellable {
                val raw = rawResult.getOrThrow()
                val json = LJson.parseToJsonElement(raw).jsonObject
                val listByDate = json.objectAt("data", "favorite", "list_by_date")

                listByDate?.get("errors").errorMessagesOrNull("Unknown error")?.let { msg ->
                    throw IllegalStateException("Server error: $msg")
                }

                val itemsElement = listByDate.objectAt("pictures")?.get("items")

                if (itemsElement is JsonArray) {
                    val sanitized = buildJsonArray {
                        itemsElement.forEach { elem ->
                            if (elem is JsonObject) {
                                val albumElem = elem["album"]
                                val albumId = if (albumElem is JsonObject) {
                                    albumElem["id"]?.jsonPrimitive?.contentOrNull
                                } else if (albumElem is JsonPrimitive) {
                                    albumElem.contentOrNull
                                } else {
                                    null
                                }
                                add(buildJsonObject {
                                    elem.forEach { (k, v) ->
                                        if (k == "album") {
                                            // Нет альбома — null. Раньше сюда писалась
                                            // строка "null", и она уходила в метаданные
                                            // сохранённого лайка.
                                            put("album", albumId?.let(::JsonPrimitive) ?: JsonNull)
                                        } else {
                                            put(k, v)
                                        }
                                    }
                                })
                            } else {
                                add(elem)
                            }
                        }
                    }
                    val pics = LJson.decodeFromJsonElement<List<PicsDetails>>(sanitized)
                    normalizePictureUrls(pics)
                } else {
                    emptyList()
                }
            }
        }.onFailure { e ->
            Timber.e("Failed to parse server liked pictures response: ${e.javaClass.simpleName}")
        }
    }

    override suspend fun addFavorite(
        anchorId: String,
        anchorType: String,
        favoriteType: String
    ): Result<Unit> {
        val cleanAnchorId = anchorId.trim()
        if (cleanAnchorId.isBlank()) {
            return Result.failure(IllegalArgumentException("anchor_id cannot be blank"))
        }

        val rawResult = FavoriteAdd(
            repository = repository,
            anchorId = cleanAnchorId,
            anchorType = anchorType,
            favoriteType = favoriteType
        )
        return parseFavoriteMutationResult(rawResult, "add_favorite", "FavoriteAdd")
    }

    /**
     * Ищет id картинки на сервере по фрагменту её адреса: листает страницы
     * альбома, пока не найдёт совпадение.
     *
     * Любой сбой возвращается отказом: вызывают отсюда без `try`, из области
     * приложения, и исключение уронило бы приложение. Раньше разбор шёл через
     * `.jsonObject` и `.jsonArray`, которые бросают на `null` — хватало
     * `"thumbnails": null` у одной картинки альбома.
     */
    override suspend fun resolvePictureId(
        albumId: String,
        mediaUrlOrFileName: String
    ): Result<String> = try {
        findPictureId(albumId, mediaUrlOrFileName)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e("resolvePictureId failed: ${e.javaClass.simpleName}")
        Result.failure(e)
    }

    private suspend fun findPictureId(albumId: String, mediaUrlOrFileName: String): Result<String> {
        val cleanAlbumId = albumId.trim()
        val albumInt = cleanAlbumId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid album id: $albumId"))

        val targetSlug = extractSlugCandidate(mediaUrlOrFileName)
        Timber.d("resolvePictureId: albumId=$cleanAlbumId, targetSlug='$targetSlug'")
        if (targetSlug.isBlank()) {
            return Result.failure(IllegalStateException("Картинка не найдена в альбоме"))
        }

        var page = 1
        var totalPages = 1
        while (page <= totalPages && page <= PICTURE_LOOKUP_MAX_PAGES) {
            val query = GraphQlRequest.pictureListInsideAlbum(albumInt, page)
            val raw = repository.openURI(query, config = RepositoryUriConfig.CACHE_RAM)
                .getOrElse { return Result.failure(it) }

            val json = runCatching { LJson.parseToJsonElement(raw) as? JsonObject }.getOrNull()
                ?: return Result.failure(IllegalStateException("Malformed album JSON"))
            val pictureList = json.objectAt("data", "picture", "list_in_album")
                ?: return Result.failure(IllegalStateException("Missing picture list"))

            totalPages = pictureList.objectAt("info")?.get("total_pages").contentOrNull()?.toIntOrNull() ?: totalPages

            for (element in pictureList["items"] as? JsonArray ?: JsonArray(emptyList())) {
                val picObj = element as? JsonObject ?: continue
                val picId = picObj["id"].contentOrNull()?.trim()
                if (picId.isNullOrBlank()) continue

                if (pictureMatchesSlug(picObj, targetSlug)) {
                    Timber.d("resolvePictureId: matched slug '$targetSlug' with picture id: $picId")
                    return Result.success(picId)
                }
            }
            page++
        }

        // Альбом длиннее предела: поиск остановлен, а не исчерпан — говорим об этом прямо.
        if (totalPages > PICTURE_LOOKUP_MAX_PAGES) {
            return Result.failure(
                IllegalStateException(
                    "Картинка не найдена на первых $PICTURE_LOOKUP_MAX_PAGES страницах альбома из $totalPages"
                )
            )
        }
        return Result.failure(IllegalStateException("Картинка не найдена в альбоме"))
    }

    override suspend fun removeFavorite(
        anchorId: String,
        anchorType: String,
        favoriteType: String
    ): Result<Unit> {
        val cleanAnchorId = anchorId.trim()
        if (cleanAnchorId.isBlank()) {
            return Result.failure(IllegalArgumentException("anchor_id cannot be blank"))
        }

        val rawResult = FavoriteRemove(repository).removeFavorite(
            anchorId = cleanAnchorId,
            anchorType = anchorType,
            favoriteType = favoriteType
        )
        return parseFavoriteMutationResult(rawResult, "remove_favorite", "FavoriteRemove")
            .onSuccess { if (anchorType == "album") _removedAlbumIds.tryEmit(cleanAnchorId) }
    }

    private val _removedAlbumIds = MutableSharedFlow<String>(extraBufferCapacity = REMOVED_ALBUMS_BUFFER)

    override val removedAlbumIds: SharedFlow<String> = _removedAlbumIds.asSharedFlow()

    /**
     * Разбирает результат мутации добавления/удаления избранного, проверяя ошибки GraphQL и структуры мутации.
     */
    private fun parseFavoriteMutationResult(
        rawResult: Result<String>,
        mutationField: String,
        operationLabel: String
    ): Result<Unit> {
        if (rawResult.isFailure) {
            return Result.failure(rawResult.exceptionOrNull() ?: IllegalStateException("Request failed"))
        }

        return runCatching {
            val raw = rawResult.getOrThrow()
            val json = LJson.parseToJsonElement(raw).jsonObject

            json["errors"].errorMessagesOrNull("GraphQL error")?.let { throw IllegalStateException(it) }

            json.objectAt("data", "favorite", mutationField)
                ?.get("errors")
                .errorMessagesOrNull("Mutation error")
                ?.let { throw IllegalStateException(it) }
            Unit
        }.onFailure { e ->
            Timber.e("Failed to parse $operationLabel response: ${e.javaClass.simpleName}")
        }
    }
}

/**
 * Проверяет соответствие адресов картинки в GraphQL-ответе [picObj] искомому ключу [targetSlug].
 *
 * Ключ должен стоять в адресе целиком, а не частью другого слова или числа.
 * У файла без длинного идентификатора ключ короткий — хвост имени вроде `0012`,
 * — и поиск подстрокой находил его в `…100127…` чужой картинки: лайк на
 * сервере ставился или снимался не у той.
 */
internal fun pictureMatchesSlug(picObj: JsonObject, targetSlug: String): Boolean {
    fun JsonElement?.mentionsSlug() = contentOrNull()?.containsWholeToken(targetSlug) == true

    if (picObj["url"].mentionsSlug()) return true
    if (picObj["url_to_original"].mentionsSlug()) return true
    if (picObj["url_to_video"].mentionsSlug()) return true
    val thumbnails = picObj["thumbnails"] as? JsonArray ?: return false
    return thumbnails.any { (it as? JsonObject)?.get("url").mentionsSlug() }
}

/** Есть ли в строке [token] целиком: по обе стороны от него не буква и не цифра. Регистр не важен. */
private fun String.containsWholeToken(token: String): Boolean {
    if (token.isEmpty()) return false
    var from = 0
    while (true) {
        val at = indexOf(token, from, ignoreCase = true)
        if (at < 0) return false
        val before = getOrNull(at - 1)
        val after = getOrNull(at + token.length)
        if (before?.isLetterOrDigit() != true && after?.isLetterOrDigit() != true) return true
        from = at + 1
    }
}

/**
 * Вложенный объект по цепочке ключей. `null` на любом шаге, где значения нет
 * или оно не объект: сервер отдаёт `null` там, где обычно объект, а `.jsonObject`
 * на таком значении бросает исключение.
 */
private fun JsonObject?.objectAt(vararg path: String): JsonObject? =
    path.fold(this) { node, key -> node?.get(key) as? JsonObject }

/** Строковое значение примитива; `null`, если значения нет, оно `null` или это не примитив. */
private fun JsonElement?.contentOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

/**
 * Тексты ошибок из поля `errors`, склеенные в одну строку. `null`, если ошибок
 * нет: поле отсутствует, равно `null` или пустому списку. Раньше `null` читался
 * как список и превращал успешный ответ в ошибку разбора.
 */
private fun JsonElement?.errorMessagesOrNull(fallback: String): String? {
    val errors = (this as? JsonArray)?.takeIf { it.isNotEmpty() } ?: return null
    return errors.joinToString { (it as? JsonObject)?.get("message").contentOrNull() ?: fallback }
}

/**
 * Сколько страниц альбома просматривает поиск id картинки. Запросы идут по
 * одному с паузой, поэтому предел есть, но он выше прежних десяти страниц:
 * картинка с одиннадцатой уже не находилась.
 */
internal const val PICTURE_LOOKUP_MAX_PAGES = 100

/** Запас потока удалённых альбомов: без подписчика события просто теряются, список и так загрузится заново. */
private const val REMOVED_ALBUMS_BUFFER = 16

private val ULID_REGEX = Regex("""[0-9A-HJKMNP-TV-Z]{26}""")
private val DIMENSIONS_EXT_REGEX = Regex("""\.\d+x\d+\.[a-zA-Z0-9]+$""")
private val EXT_REGEX = Regex("""\.[a-zA-Z0-9]+$""")

/**
 * Извлекает кандидат на слаг или ULID идентификатор из строки URL или локального имени файла.
 */
internal fun extractSlugCandidate(input: String): String {
    val clean = input.substringBefore('?').substringBefore('#').trim()
    val ulidMatch = ULID_REGEX.find(clean)
    if (ulidMatch != null) return ulidMatch.value

    val fileName = clean.substringAfterLast('/').substringAfterLast('\\')
    val withoutExt = fileName
        .replace(DIMENSIONS_EXT_REGEX, "")
        .replace(EXT_REGEX, "")

    val slug = withoutExt.substringAfterLast('_')
    return if (slug.length >= 4) slug else withoutExt
}

/**
 * Hilt-модуль привязки реализации [LusciousServerFavoritesRepositoryImpl] к интерфейсу [LusciousServerFavoritesRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LusciousServerFavoritesModule {

    /** Привязывает реализацию репозитория серверных подписок и лайков L. */
    @Binds
    @Singleton
    abstract fun bindLusciousServerFavoritesRepository(
        impl: LusciousServerFavoritesRepositoryImpl
    ): LusciousServerFavoritesRepository
}
