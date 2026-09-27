package com.client.xvideos.x.parcer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserChannelTest {

    @Test
    fun `parserChannelHeader correctly parses banner, avatar, counters and about me`() {
        val html = """
            <!doctype html>
            <html>
            <body>
                <div id="profile-title" class="banner-sliders">
                    <div class="banner-slider">
                        <img src="https://cdn.example.com/banner.jpg" />
                    </div>
                    <div class="profile-infos">
                        <div class="profile-pic">
                            <img src="https://cdn.example.com/pp_big.jpg" />
                        </div>
                        <h2 class="with-aka">
                            <strong class="text-danger">Dartoficial</strong>
                            <span class="user-subscribe"><span class="count">10,1 к</span></span>
                        </h2>
                    </div>
                </div>
                <div id="header-about-me">
                    D.ART<br />Dominar é minha arte<div class="show-more">+</div>
                </div>
                <div id="tab-videos"><span class="count">70</span></div>
                <p id="pinfo-videos-views"><span>22 045 087</span></p>
            </body>
            </html>
        """.trimIndent()

        val header = parserChannelHeader(html, fallbackSlug = "dart_oficial")

        assertEquals("dart_oficial", header.slug)
        assertEquals("Dartoficial", header.name)
        assertEquals("https://cdn.example.com/banner.jpg", header.bannerUrl)
        assertEquals("https://cdn.example.com/pp_big.jpg", header.avatarUrl)
        assertEquals("10,1 к", header.subscribers)
        assertEquals("22 045 087", header.totalViews)
        assertTrue(header.aboutMe.contains("D.ART"))
        assertTrue(header.aboutMe.contains("Dominar é minha arte"))
        assertFalse(header.aboutMe.contains("+")) // .show-more must be stripped
        assertEquals(70, header.videoCount)
    }

    @Test
    fun `parserChannelHeader falls back gracefully on empty HTML`() {
        val header = parserChannelHeader(
            html = "",
            fallbackSlug = "my_channel",
            fallbackName = "My Channel",
            fallbackSubscribers = "5k"
        )

        assertEquals("my_channel", header.slug)
        assertEquals("My Channel", header.name)
        assertEquals("5k", header.subscribers)
        assertEquals("", header.bannerUrl)
        assertEquals(0, header.videoCount)
    }

    @Test
    fun `parserChannelHeader correctly parses model profile details`() {
        val html = """
            <!doctype html>
            <html>
            <body>
                <div id="profile-title" class="banner-sliders">
                    <div class="profile-infos">
                        <div class="profile-pic">
                            <img src="https://cdn.example.com/joy_sky.jpg" />
                        </div>
                        <h2 class="with-aka">
                            <strong class="text-danger">Joy Ski</strong>
                            <span class="user-subscribe"><span class="count">24,5 к</span></span>
                        </h2>
                    </div>
                </div>
                <div id="pinfo-sex"><span>Женщина</span></div>
                <div id="pinfo-age"><span>26 лет</span></div>
                <div id="pinfo-country"><span>Бразилия</span></div>
                <div id="pinfo-workedfor"><span>Studio X, Private</span></div>
                <div id="pinfo-aboutme">
                    Official Joy Ski profile. Enjoy my videos!<div class="show-more">+</div>
                </div>
                <div id="tab-videos"><span class="count">48</span></div>
                <p id="pinfo-videos-views"><span>14 200 000</span></p>
                <a href="/models/joy-sky">Model link</a>
            </body>
            </html>
        """.trimIndent()

        val header = parserChannelHeader(html, fallbackSlug = "joy-sky", isModel = true)

        assertTrue(header.isModel)
        assertEquals("joy-sky", header.slug)
        assertEquals("Joy Ski", header.name)
        assertEquals("https://cdn.example.com/joy_sky.jpg", header.avatarUrl)
        assertEquals("", header.bannerUrl)
        assertFalse(header.hasBanner)
        assertEquals("24,5 к", header.subscribers)
        assertEquals("14 200 000", header.totalViews)
        assertEquals(48, header.videoCount)
        assertEquals("Женщина", header.gender)
        assertEquals("26 лет", header.age)
        assertEquals("Бразилия", header.country)
        assertEquals("Studio X, Private", header.workedWith)
        assertEquals("Женщина, Бразилия, 26 лет", header.subtitle)
        assertTrue(header.aboutMe.contains("Official Joy Ski profile"))
        assertFalse(header.aboutMe.contains("+"))
    }

    @Test
    fun `parserChannelHeader correctly parses collaborators links`() {
        val html = """
            <!doctype html>
            <html>
            <body>
                <div class="profile-infos">
                    <h2><strong class="text-danger">Joy Sky</strong></h2>
                </div>
                <p id="pinfo-workedfor">
                    <strong>Работал для/с:</strong>
                    <span>
                        <a href="/profiles/loupan-1" class="text-danger">Loupan Producoes</a>, 
                        <a href="/profiles/pernocasoficial" class="text-danger">Pernocas</a>, 
                        <a href="/models/lady_milf" class="text-danger">Lady Milf</a>,
                        <a href="/channels/dart_oficial" class="text-danger">Dartoficial</a>
                    </span>
                </p>
            </body>
            </html>
        """.trimIndent()

        val header = parserChannelHeader(html, fallbackSlug = "joy-sky", isModel = true)
        assertTrue(header.hasCollaborators)
        assertEquals(4, header.collaborators.size)

        val c1 = header.collaborators[0]
        assertEquals("Loupan Producoes", c1.name)
        assertEquals("/profiles/loupan-1", c1.href)
        assertEquals("loupan-1", c1.cleanSlug)
        assertFalse(c1.isModel)

        val c2 = header.collaborators[1]
        assertEquals("Pernocas", c2.name)
        assertEquals("pernocasoficial", c2.cleanSlug)

        val c3 = header.collaborators[2]
        assertEquals("Lady Milf", c3.name)
        assertEquals("lady_milf", c3.cleanSlug)
        assertTrue(c3.isModel)

        val c4 = header.collaborators[3]
        assertEquals("Dartoficial", c4.name)
        assertEquals("dart_oficial", c4.cleanSlug)
        assertFalse(c4.isModel)
    }

    @Test
    fun `parserChannelVideosJson extracts video cards and unescapes titles`() {
        val json = """
            {
                "nb_videos": 72,
                "current_page": 0,
                "videos": [
                    {
                        "id": 86016303,
                        "eid": "oitlhif2529",
                        "u": "/prof-video-click/upload/dart_oficial/oitlhif2529/video_one",
                        "i": "https://thumb.example.com/xv_14_t.jpg",
                        "ipu": "https://thumb.example.com/preview.mp4",
                        "tf": "Video with &#039;quotes&#039; &amp; fun",
                        "d": "10 мин.",
                        "r": "95%",
                        "n": "388,2 к",
                        "pn": "Dartoficial",
                        "pu": "/dart_oficial"
                    },
                    {
                        "id": 83072541,
                        "u": "/prof-video-click/upload/dart_oficial/83072541/video_two",
                        "i": "https://thumb.example.com/xv_6_t.jpg",
                        "t": "Second Video",
                        "d": "15 мин.",
                        "r": "100%",
                        "n": "284,6 к",
                        "pn": "Dartoficial",
                        "pu": "/dart_oficial"
                    }
                ],
                "result": true
            }
        """.trimIndent()

        val videos = parserChannelVideosJson(json)

        assertEquals(2, videos.size)

        val v1 = videos[0]
        assertEquals(86016303L, v1.id)
        assertEquals("Video with 'quotes' & fun", v1.title)
        assertTrue(v1.href.contains("video_one"))
        assertEquals("https://thumb.example.com/xv_14_t.jpg", v1.previewImage)
        assertEquals("https://thumb.example.com/preview.mp4", v1.previewVideo)
        assertEquals("10 мин.", v1.duration)
        assertEquals("388,2 к", v1.views)
        assertEquals("Dartoficial", v1.nameProfile)

        val v2 = videos[1]
        assertEquals(83072541L, v2.id)
        assertEquals("Second Video", v2.title)
    }

    @Test
    fun `parserChannelVideosJson handles empty or malformed json safely`() {
        assertEquals(0, parserChannelVideosJson("").size)
        assertEquals(0, parserChannelVideosJson("invalid json").size)
        assertEquals(0, parserChannelVideosJson("{\"videos\": []}").size)
    }

    @Test
    fun `parseChannelModels correctly extracts workedForFree models`() {
        val html = """
            <!doctype html>
            <html>
            <head>
            <script>
            window.xv.conf = {
                "data": {
                    "workedForFree": [
                        {"idUser": 11831644, "displayName": "Cory Chase", "gender": "Woman", "nbVideos": 413, "fNbVideos": "413"},
                        {"idUser": 12262697, "displayName": "Melanie Hicks", "gender": "Woman", "nbVideos": 93, "fNbVideos": "93"},
                        {"idUser": 853595215, "displayName": "Dart", "gender": "Man", "nbVideos": 70, "fNbVideos": "70"}
                    ]
                }
            };
            </script>
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val models = parseChannelModels(html)
        assertEquals(3, models.size)

        val m1 = models[0]
        assertEquals(11831644L, m1.idUser)
        assertEquals("Cory Chase", m1.displayName)
        assertTrue(m1.isWoman)
        assertFalse(m1.isMan)
        assertEquals("413", m1.countText)
        assertEquals("Cory Chase (413)", m1.formattedTitle)
        assertTrue(m1.matches("cory"))
        assertTrue(m1.matches("CHASE"))
        assertFalse(m1.matches("Melanie"))

        val m3 = models[2]
        assertEquals("Dart", m3.displayName)
        assertTrue(m3.isMan)
    }

    @Test
    fun `parseChannelModels handles missing or empty workedForFree safely`() {
        assertEquals(0, parseChannelModels("").size)
        assertEquals(0, parseChannelModels("<html></html>").size)
        assertEquals(0, parseChannelModels("\"workedForFree\": []").size)
        assertEquals(0, parseChannelModels("\"workedForFree\": invalid").size)
    }
}
