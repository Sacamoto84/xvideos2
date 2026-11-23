package com.client.xvideos.r.network.api

import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.http.ApiClient
import com.client.xvideos.r.network.http.Route
import io.ktor.client.call.body
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal enum class AccountMembership {
    LIKE,
    AUTHOR,
    NICHE;

    suspend fun ids(account: RAccountStatus.SignedIn): Result<Set<String>> = when (this) {
        LIKE -> ApiClient.requestAsAccount(Route.get("/v2/likes"), account) { it.body<List<String>>().toSet() }
        AUTHOR -> ApiClient.requestAsAccount(Route.get("/v1/me/follows"), account) { it.body<List<String>>().toSet() }
        NICHE -> ApiClient.requestAsAccount(Route.get("/v2/niches/following"), account) {
            it.body<AccountNichesResponse>().niches.mapTo(mutableSetOf()) { niche -> niche.id }
        }
    }

    suspend fun set(account: RAccountStatus.SignedIn, id: String, enabled: Boolean): Result<Unit> {
        val method = when {
            !enabled -> "DELETE"
            this == NICHE -> "POST"
            else -> "PUT"
        }
        val path = when (this) {
            LIKE -> "/v2/gifs/{id}/like"
            AUTHOR -> "/v1/me/follows/{id}"
            NICHE -> "/v2/niches/{id}/subscription"
        }
        val payload = when {
            this == LIKE -> buildJsonObject {
                put("context", JsonNull)
                if (enabled) put("source", "other")
            }
            this == AUTHOR && enabled -> buildJsonObject { put("source", "other") }
            else -> null
        }
        return ApiClient.requestAsAccount(Route(method, path, "id" to id), account, payload) {}
    }
}

internal object AccountCollectionsApi {
    suspend fun create(account: RAccountStatus.SignedIn, name: String): Result<AccountCollection> {
        val payload = collectionFields(name, published = false, description = null)
        return ApiClient.requestAsAccount(Route.post("/v2/me/collections"), account, payload) { it.body() }
    }

    suspend fun rename(account: RAccountStatus.SignedIn, id: String, name: String): Result<Unit> {
        val collection = ApiClient.requestAsAccount(collectionRoute("GET", id), account) {
            it.body<AccountCollectionDetails>()
        }.getOrElse { return Result.failure(it) }
        val payload = collectionFields(name, collection.published, collection.description, id)
        return ApiClient.requestAsAccount(collectionRoute("PATCH", id), account, payload) {}
    }

    suspend fun delete(account: RAccountStatus.SignedIn, id: String): Result<Unit> =
        ApiClient.requestAsAccount(collectionRoute("DELETE", id), account) {}

    suspend fun setItem(
        account: RAccountStatus.SignedIn,
        id: String,
        gifId: String,
        added: Boolean,
    ): Result<Unit> {
        val payload: JsonElement = if (added) {
            buildJsonObject {
                put("gifId", gifId)
                put("context", JsonNull)
            }
        } else {
            JsonArray(listOf(JsonPrimitive(gifId)))
        }
        val route = Route(if (added) "POST" else "DELETE", "/v2/me/collections/{id}/gifs", "id" to id)
        return ApiClient.requestAsAccount(route, account, payload) {}
    }

    private fun collectionRoute(method: String, id: String) =
        Route(method, "/v2/me/collections/{id}", "id" to id)

    private fun collectionFields(name: String, published: Boolean, description: String?, id: String? = null) =
        buildJsonObject {
            put("folderName", name)
            put("published", published)
            put("description", description?.let(::JsonPrimitive) ?: JsonNull)
            if (id != null) put("collectionId", id)
        }

    @Serializable
    private data class AccountCollectionDetails(val published: Boolean, val description: String? = null)
}
