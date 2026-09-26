package com.client.xvideos.l.model

import com.client.xvideos.common.json.AppJson
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import com.client.xvideos.l.net.LAlbumBundleCache
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.net.graphQl.MediaCategoriesBootstrapResponse
import com.client.xvideos.l.net.json.LJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Тесты сериализации моделей Luscious (:feature-l):
 * 1. Разбор через kotlinx.serialization с лояльным парсером LJson (сетевой вход).
 * 2. Полная обратная совместимость с дисковыми кэшами (LAlbumBundleCache, LMediaPersist).
 * 3. Совместимость с Java Serializable (Voyager Navigation saved state).
 */
class LSerializationCompatibilityTest {

    /* ---------- 1. kotlinx.serialization: сетевые DTO ---------- */

    @Test
    fun `AlbumDetails разбирается через LJson даже с неожиданными полями и пропусками`() {
        val json = """
            {
                "id": "12345",
                "title": "Test Album",
                "is_manga": true,
                "number_of_pictures": 42,
                "download_url": "/download/12345/",
                "like_status": "liked",
                "tags": [
                    {"id": "1", "text": "cosplay", "count": 10, "url": "/tags/cosplay/"}
                ],
                "genres": [
                    {"id": "2", "title": "Ecchi", "acts_as_warning": false, "url": "/genres/ecchi/"}
                ],
                "cover": {
                    "width": 800,
                    "height": 1200,
                    "size": "large",
                    "url": "https://cdn/cover.jpg"
                },
                "content": {
                    "id": "c1",
                    "title": "Manga",
                    "url": "/manga/"
                },
                "created_by": {
                    "id": "u1",
                    "name": "Artist1",
                    "display_name": "Artist One",
                    "url": "/users/u1/"
                },
                "language": {
                    "id": "lang1",
                    "title": "English",
                    "url": "/languages/english/"
                },
                "unexpected_field": "some_extra_payload"
            }
        """.trimIndent()

        val album = LJson.decodeFromString<AlbumDetails>(json)

        assertEquals("12345", album.id)
        assertEquals("Test Album", album.title)
        assertTrue(album.is_manga)
        assertEquals(42, album.number_of_pictures)
        assertEquals("/download/12345/", album.download_url)
        assertEquals("liked", album.likeStatus)
        assertEquals(1, album.tags.size)
        assertEquals("cosplay", album.tags[0].text)
        assertEquals(1, album.genres.size)
        assertEquals("Ecchi", album.genres[0].title)
        assertEquals("https://cdn/cover.jpg", album.cover?.url)
        assertEquals("Manga", album.content.title)
        assertEquals("Artist1", album.createdBy?.name)
        assertEquals("English", album.language?.title)
    }

    @Test
    fun `PicsDetails и Thumbnails разбираются через LJson`() {
        val json = """
            {
                "height": 1080,
                "width": 1920,
                "is_animated": false,
                "url_to_original": "https://cdn/original.jpg",
                "url_to_video": null,
                "album": "album_123",
                "thumbnails": [
                    {
                        "width": 640,
                        "height": 360,
                        "size": "small",
                        "url": "https://cdn/small.jpg"
                    },
                    {
                        "width": 1600,
                        "height": 900,
                        "size": "xMax",
                        "url": "https://cdn/xmax.jpg"
                    }
                ]
            }
        """.trimIndent()

        val pic = LJson.decodeFromString<PicsDetails>(json)

        assertEquals(1080, pic.height)
        assertEquals(1920, pic.width)
        assertEquals(false, pic.is_animated)
        assertEquals("https://cdn/original.jpg", pic.url_to_original)
        assertEquals("album_123", pic.album)
        assertEquals(2, pic.thumbnails?.size)
        assertEquals("xMax", pic.thumbnails?.get(1)?.size)
    }

    @Test
    fun `AlbumResponse, AlbumList и FacetCollectionInfo разбираются через LJson`() {
        val json = """
            {
                "data": {
                    "album": {
                        "list": {
                            "info": {
                                "page": 1,
                                "has_next_page": true,
                                "has_previous_page": false,
                                "total_items": 100,
                                "total_pages": 4,
                                "items_per_page": 25,
                                "url_complete": "https://example/list"
                            },
                            "items": [
                                {
                                    "__typename": "Album",
                                    "id": "a1",
                                    "title": "Album One",
                                    "number_of_pictures": 10,
                                    "is_manga": true,
                                    "url": "/albums/a1/",
                                    "download_url": "/download/a1/"
                                }
                            ]
                        }
                    }
                }
            }
        """.trimIndent()

        val response = LJson.decodeFromString<AlbumResponse>(json)
        val list = response.data.album.list

        assertEquals(1, list.info.page)
        assertTrue(list.info.hasNextPage)
        assertEquals(100, list.info.totalItems)
        assertEquals(1, list.items.size)
        assertEquals("a1", list.items[0].id)
        assertEquals("Album One", list.items[0].title)
        assertTrue(list.items[0].isManga)
    }

    @Test
    fun `Landing_page_albumType и AlbumListTopHits разбираются через LJson`() {
        val landingJson = """
            {
                "title": "Frontpage",
                "sections": [
                    {
                        "title": "Trending",
                        "count": 5,
                        "item_type": "album",
                        "url": "/trending/",
                        "items": [
                            {"id": "t1", "title": "Top Album 1"}
                        ]
                    }
                ]
            }
        """.trimIndent()

        val landing = LJson.decodeFromString<Landing_page_albumType>(landingJson)
        assertEquals("Frontpage", landing.title)
        assertEquals(1, landing.sections.size)
        assertEquals("Trending", landing.sections[0].title)
        assertEquals("t1", landing.sections[0].items[0].id)

        val topHitsJson = """
            {
                "title": "Top Hits",
                "url": "/tophits/",
                "count": 1,
                "item_type": "album",
                "items": [{"id": "th1", "title": "Top Hit 1"}]
            }
        """.trimIndent()

        val topHits = LJson.decodeFromString<AlbumListTopHits>(topHitsJson)
        assertEquals("Top Hits", topHits.title)
        assertEquals(1, topHits.items.size)
        assertEquals("th1", topHits.items[0].id)
    }

    @Test
    fun `MediaCategoriesBootstrapResponse и FilterGenre разбираются через LJson`() {
        val json = """
            {
                "data": {
                    "media_categories": {
                        "genres": [
                            {
                                "id": "g1",
                                "title": "Fantasy",
                                "slug": "fantasy",
                                "description": "Fantasy genre",
                                "uploading_rules": "rules",
                                "acts_as_warning": false,
                                "acts_as_default": true,
                                "represents_uncategorized": false,
                                "url": "/genres/fantasy/",
                                "only_content": {
                                    "id": "c1",
                                    "title": "Pictures",
                                    "url": "/pictures/"
                                }
                            }
                        ],
                        "filter_settings": {
                            "user_id": 999,
                            "has_custom_filters": true,
                            "uses_default_warnings": false,
                            "audience_ids": ["1", "2"],
                            "genres_blocked_ids": [],
                            "genres_subscribed_ids": ["g1"],
                            "preferred_language_ids": ["en"],
                            "default_dashboard_content_id": "0"
                        },
                        "languages": [
                            {"id": "l1", "title": "English", "url": "/lang/en/"}
                        ],
                        "content_types": [
                            {"id": "ct1", "title": "Manga", "url": "/manga/"}
                        ],
                        "audiences": [
                            {"id": "aud1", "title": "Adults", "description": "18+", "url": "/aud/adults/"}
                        ]
                    }
                }
            }
        """.trimIndent()

        val response = LJson.decodeFromString<MediaCategoriesBootstrapResponse>(json)
        val categories = response.data.mediaCategories

        assertEquals(1, categories.genres.size)
        assertEquals("Fantasy", categories.genres[0].title)
        assertEquals("Pictures", categories.genres[0].onlyContent?.title)
        assertEquals(999L, categories.filterSettings.userId)
        assertEquals(1, categories.languages.size)
        assertEquals(1, categories.contentTypes.size)
        assertEquals(1, categories.audiences.size)
    }

    @Test
    fun `AlbumListFilterGenreCountResponse разбирается через LJson`() {
        val json = """{"count": 15, "term": "hentai", "is_active": true}"""
        val response = LJson.decodeFromString<AlbumListFilterGenreCountResponse>(json)

        assertEquals(15, response.count)
        assertEquals("hentai", response.term)
        assertTrue(response.isActive)
    }

    /* ---------- 2. Обратная совместимость (дисковые кэши) ---------- */

    @Test
    fun `AppJson корректно читает и пишет AlbumDetails`() {
        val original = AlbumDetails(
            id = "disk-album-1",
            title = "Disk Album",
            is_manga = true,
            number_of_pictures = 50,
            download_url = "/download/disk/",
            likeStatus = "favorite",
            content = Content(id = "c1", title = "Art", url = "/art/"),
            cover = Cover(width = 400, height = 600, size = "cover", url = "https://cdn/c.jpg")
        )

        val json = AppJson.encodeToString(original)
        val deserialized = AppJson.decodeFromString<AlbumDetails>(json)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.title, deserialized.title)
        assertEquals(original.is_manga, deserialized.is_manga)
        assertEquals(original.number_of_pictures, deserialized.number_of_pictures)
        assertEquals(original.content.title, deserialized.content.title)
        assertEquals(original.cover?.url, deserialized.cover?.url)
    }

    @Test
    fun `AppJson и LJson взаимно читают PicsDetails`() {
        val item = PicsDetails(
            height = 800,
            width = 1200,
            is_animated = true,
            url_to_original = "https://cdn/anim.mp4",
            album = "333",
            thumbnails = listOf(
                Thumbnails(width = 300, height = 200, size = "small", url = "https://cdn/s.jpg")
            )
        )

        // LJson -> AppJson
        val jsonFromLJson = LJson.encodeToString(item)
        val fromAppJson = AppJson.decodeFromString<PicsDetails>(jsonFromLJson)
        assertEquals(item.height, fromAppJson.height)
        assertEquals(item.url_to_original, fromAppJson.url_to_original)
        assertEquals(item.thumbnails?.size, fromAppJson.thumbnails?.size)

        // AppJson -> LJson
        val jsonFromAppJson = AppJson.encodeToString(item)
        val fromLJson = LJson.decodeFromString<PicsDetails>(jsonFromAppJson)
        assertEquals(item.height, fromLJson.height)
        assertEquals(item.url_to_original, fromLJson.url_to_original)
        assertEquals(item.thumbnails?.size, fromLJson.thumbnails?.size)
    }

    @Test
    fun `LAlbumBundleCache формат дискового кэша полностью совместим с AppJson и LJson`() {
        val bundle = LAlbumBundleCache(
            schemaVersion = 1,
            cachedAtMs = 1700000000000L,
            album = AlbumDetails(id = "bundle-1", title = "Bundle Album"),
            totalPages = 3,
            pics = listOf(
                PicsDetails(height = 600, width = 800, url_to_original = "https://cdn/p1.jpg")
            )
        )

        val json = LJson.encodeToString(bundle)
        val restored = AppJson.decodeFromString<LAlbumBundleCache>(json)

        assertNotNull(restored)
        assertEquals(1, restored.schemaVersion)
        assertEquals("bundle-1", restored.album.id)
        assertEquals(3, restored.totalPages)
        assertEquals(1, restored.pics.size)
        assertEquals("https://cdn/p1.jpg", restored.pics[0].url_to_original)
    }

    /* ---------- 3. Совместимость с Java Serializable (Voyager) ---------- */

    @Test
    fun `FilterGenre сериализуется через Java ObjectOutputStream`() {
        val original = FilterGenre(
            id = "g_test",
            title = "Test Genre",
            slug = "test-genre",
            description = "Test Description",
            uploadingRules = "rules",
            actsAsWarning = true,
            actsAsDefault = false,
            representsUncategorized = false,
            url = "https://example/genre",
            onlyContent = OnlyContent(id = "oc1", title = "Pictures", url = "/pics/")
        )

        val bytes = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos -> oos.writeObject(original) }
            baos.toByteArray()
        }

        val restored = ByteArrayInputStream(bytes).use { bais ->
            ObjectInputStream(bais).use { ois -> ois.readObject() as FilterGenre }
        }

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.onlyContent?.title, restored.onlyContent?.title)
    }

    @Test
    fun `AlbumListFilter helper properties and defaults operate correctly`() {
        val defaultFilter = AlbumListFilter.DEFAULT
        assertEquals(false, defaultFilter.hasSearchQuery)
        assertEquals(false, defaultFilter.hasTags)
        assertEquals(false, defaultFilter.hasGenres)
        assertEquals(false, defaultFilter.isFiltered)

        val filtered = defaultFilter.copy(searchQuery = "test", tagPlus = listOf("tag1"))
        assertEquals(true, filtered.hasSearchQuery)
        assertEquals(true, filtered.hasTags)
        assertEquals(true, filtered.isFiltered)
    }

    @Test
    fun `SavedAlbumFilter helper properties and defaults operate correctly`() {
        val emptyFilter = SavedAlbumFilter.EMPTY
        assertEquals("", emptyFilter.name)
        assertEquals(false, emptyFilter.isValid)

        val validFilter = SavedAlbumFilter(name = "Favorites", filter = AlbumListFilter.DEFAULT)
        assertEquals(true, validFilter.isValid)
    }

    @Test
    fun `AlbumType, ContentId, and PictureCountRank operate correctly`() {
        assertEquals(AlbumType.Pictures, AlbumType.DEFAULT)
        assertEquals(AlbumType.Manga, AlbumType.fromValue("manga"))
        assertEquals(AlbumType.Pictures, AlbumType.fromValue("unknown"))
        assertEquals(true, AlbumType.All.isAll)
        assertEquals(true, AlbumType.Manga.isManga)
        assertEquals(true, AlbumType.Pictures.isPictures)

        assertEquals(ContentId.All, ContentId.DEFAULT)
        assertEquals(ContentId.Hentai, ContentId.fromValue(2))
        assertEquals(ContentId.All, ContentId.fromValue(999))
        assertEquals(true, ContentId.All.isAll)

        assertEquals(PictureCountRank.All, PictureCountRank.DEFAULT)
        assertEquals(PictureCountRank.C0_25, PictureCountRank.fromCount(0))
        assertEquals(PictureCountRank.All, PictureCountRank.fromCount(999))
        assertEquals(true, PictureCountRank.All.isAll)
    }

    @Test
    fun `LAlbumBundleCache and FilterGenre helpers operate correctly`() {
        val emptyCache = LAlbumBundleCache.EMPTY
        assertEquals(true, emptyCache.isCurrentSchema)
        assertEquals(false, emptyCache.isValid)

        val emptyGenre = FilterGenre.EMPTY
        assertEquals(false, emptyGenre.isValid)

        val emptyOnlyContent = OnlyContent.EMPTY
        assertEquals(false, emptyOnlyContent.isValid)

        val validContent = OnlyContent(id = "c1", title = "Comics")
        assertEquals(true, validContent.isValid)
    }

    @Test
    fun `AlbumDetails and PicsDetails helpers operate correctly`() {
        val emptyDetails = AlbumDetails.EMPTY
        assertEquals(false, emptyDetails.isValid)
        assertEquals(false, emptyDetails.hasDownloadUrl)
        assertEquals(false, emptyDetails.hasPictures)
        assertEquals(false, emptyDetails.hasAnimatedPictures)
        assertEquals(false, emptyDetails.hasCover)

        val validDetails = AlbumDetails(
            id = "12345",
            download_url = "https://example/dl",
            number_of_pictures = 10,
            number_of_animated_pictures = 2,
            cover = Cover(url = "https://example/cov.jpg")
        )
        assertEquals(true, validDetails.isValid)
        assertEquals(true, validDetails.hasDownloadUrl)
        assertEquals(true, validDetails.hasPictures)
        assertEquals(true, validDetails.hasAnimatedPictures)
        assertEquals(true, validDetails.hasCover)

        val emptyContent = Content.EMPTY
        assertEquals(false, emptyContent.isValid)
        val validContent = Content(id = "cnt_1")
        assertEquals(true, validContent.isValid)

        val emptyPics = PicsDetails.EMPTY
        assertEquals(false, emptyPics.isValid)
        assertEquals(false, emptyPics.hasVideo)
        assertEquals(false, emptyPics.hasOriginal)
        assertEquals(false, emptyPics.hasThumbnails)

        val validPics = PicsDetails(
            id = "pic_1",
            url_to_video = "https://example/v.mp4",
            url_to_original = "https://example/orig.jpg",
            thumbnails = listOf(Thumbnails(url = "https://example/t.jpg"))
        )
        assertEquals(true, validPics.isValid)
        assertEquals(true, validPics.hasVideo)
        assertEquals(true, validPics.hasOriginal)
        assertEquals(true, validPics.hasThumbnails)

        val emptyThumb = Thumbnails.EMPTY
        assertEquals(false, emptyThumb.isValid)
        assertEquals(true, Thumbnails(url = "https://example/t.jpg").isValid)
    }

    @Test
    fun `AlbumListType models and helpers operate correctly`() {
        val emptyLanding = Landing_page_albumType.EMPTY
        assertEquals(true, emptyLanding.isEmpty)
        assertEquals(false, emptyLanding.isNotEmpty)

        val emptySection = Landing_page_albumSection.EMPTY
        assertEquals(true, emptySection.isEmpty)
        assertEquals(false, emptySection.isNotEmpty)

        val emptyTopHits = AlbumListTopHits.EMPTY
        assertEquals(true, emptyTopHits.isEmpty)
        assertEquals(false, emptyTopHits.isNotEmpty)

        assertNotNull(AlbumResponse.EMPTY)
        assertNotNull(AlbumData.EMPTY)
        assertNotNull(AlbumListWrapper.EMPTY)

        val emptyList = AlbumList.EMPTY
        assertEquals(true, emptyList.isEmpty)
        assertEquals(false, emptyList.isNotEmpty)

        val emptyInfo = FacetCollectionInfo.EMPTY
        assertEquals(false, emptyInfo.hasNext)
        assertEquals(false, emptyInfo.hasPrevious)
        assertEquals(true, emptyInfo.isFirstPage)

        val infoPage2 = FacetCollectionInfo(page = 2, hasNextPage = true, hasPreviousPage = true)
        assertEquals(true, infoPage2.hasNext)
        assertEquals(true, infoPage2.hasPrevious)
        assertEquals(false, infoPage2.isFirstPage)

        val emptyAlbum = Album.EMPTY
        assertEquals(false, emptyAlbum.isValid)
        assertEquals(false, emptyAlbum.hasCover)
        assertEquals(false, emptyAlbum.hasPictures)
        assertEquals(false, emptyAlbum.hasAnimatedPictures)
        assertEquals(false, emptyAlbum.hasTags)
        assertEquals(false, emptyAlbum.hasGenres)

        val validAlbum = Album(
            id = "555",
            cover = Cover(url = "https://example/c.jpg"),
            numberOfPictures = 5,
            numberOfAnimatedPictures = 1,
            tags = listOf(Tag(id = "t1", text = "Tag 1")),
            genres = listOf(Genre(id = "g1", title = "Genre 1"))
        )
        assertEquals(true, validAlbum.isValid)
        assertEquals(true, validAlbum.hasCover)
        assertEquals(true, validAlbum.hasPictures)
        assertEquals(true, validAlbum.hasAnimatedPictures)
        assertEquals(true, validAlbum.hasTags)
        assertEquals(true, validAlbum.hasGenres)

        assertEquals(false, Cover.EMPTY.isValid)
        assertEquals(true, Cover(url = "https://example/c.jpg").isValid)

        assertEquals(false, Language.EMPTY.isValid)
        assertEquals(true, Language(id = "en", title = "English").isValid)

        val emptyUser = User.EMPTY
        assertEquals(false, emptyUser.isValid)
        assertEquals("", emptyUser.effectiveName)

        val userWithName = User(id = "u1", name = "john")
        assertEquals(true, userWithName.isValid)
        assertEquals("john", userWithName.effectiveName)

        val userWithDisplayName = User(id = "u2", name = "john", displayName = "John Doe")
        assertEquals("John Doe", userWithDisplayName.effectiveName)

        assertEquals(false, Tag.EMPTY.isValid)
        assertEquals(true, Tag(id = "t1", text = "art").isValid)

        assertEquals(false, Genre.EMPTY.isValid)
        assertEquals(true, Genre(id = "g1", title = "manga").isValid)

        assertEquals(false, Audience.EMPTY.isValid)
        assertEquals(true, Audience(id = "a1", title = "everyone").isValid)
    }

    @Test
    fun `UserProfile helper properties operate correctly`() {
        val emptyProfile = UserProfile.EMPTY
        assertEquals(true, emptyProfile.isEmpty)
        assertEquals(false, emptyProfile.isNotEmpty)
        assertEquals(false, emptyProfile.isValid)
        assertEquals(false, emptyProfile.hasEmail)
        assertEquals(false, emptyProfile.hasPassword)

        val partialProfile = UserProfile(email = "test@example.com")
        assertEquals(false, partialProfile.isValid)
        assertEquals(true, partialProfile.hasEmail)
        assertEquals(false, partialProfile.hasPassword)

        val fullProfile = UserProfile(email = "test@example.com", password = "secretPassword123")
        assertEquals(true, fullProfile.isValid)
        assertEquals(true, fullProfile.hasEmail)
        assertEquals(true, fullProfile.hasPassword)
        assertEquals(false, fullProfile.isEmpty)
        assertEquals(true, fullProfile.isNotEmpty)
    }
}
