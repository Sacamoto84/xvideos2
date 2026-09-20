package com.client.xvideos.x.screens.videoplayer.atom

import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeTagsTest {

    @Test
    fun `empty tags model produces empty visible items and cannot toggle`() {
        val tags = TagsModel(
            mainUploader = emptyList(),
            pornstars = emptyList(),
            tags = emptyList(),
        )

        val stateCollapsed = computeVisibleTags(tags, isExpanded = false)
        assertTrue(stateCollapsed.visibleItems.isEmpty())
        assertEquals(0, stateCollapsed.hiddenCount)
        assertFalse(stateCollapsed.canToggle)
        assertFalse(stateCollapsed.isExpanded)

        val stateExpanded = computeVisibleTags(tags, isExpanded = true)
        assertTrue(stateExpanded.visibleItems.isEmpty())
        assertEquals(0, stateExpanded.hiddenCount)
        assertFalse(stateExpanded.canToggle)
    }

    @Test
    fun `tags below or equal to threshold are all shown without toggle`() {
        val tags = TagsModel(
            mainUploader = listOf(TagsMainUploaderPornstar("href1", "ChannelA", "10k")),
            pornstars = listOf(TagsMainUploaderPornstar("href2", "StarB", "5k")),
            tags = listOf("blonde"),
        )

        val state = computeVisibleTags(tags, isExpanded = false)
        assertEquals(3, state.visibleItems.size)
        assertEquals(0, state.hiddenCount)
        assertFalse(state.canToggle)
        assertEquals("ChannelA", state.visibleItems[0].name)
        assertEquals("StarB", state.visibleItems[1].name)
        assertEquals("blonde", state.visibleItems[2].name)
    }

    @Test
    fun `tags exceeding threshold collapse to limit with accurate hiddenCount`() {
        val tags = TagsModel(
            mainUploader = listOf(TagsMainUploaderPornstar("href1", "ChannelA", "10k")),
            pornstars = listOf(TagsMainUploaderPornstar("href2", "StarB", "5k")),
            tags = listOf("tag1", "tag2", "tag3", "tag4"),
        )
        // Total items = 1 + 1 + 4 = 6 items. Collapsed limit = 2.

        val state = computeVisibleTags(tags, isExpanded = false)
        assertEquals(2, state.visibleItems.size)
        assertEquals(4, state.hiddenCount)
        assertTrue(state.canToggle)
        assertFalse(state.isExpanded)

        assertTrue(state.visibleItems[0] is TagItem.Channel)
        assertEquals("ChannelA", state.visibleItems[0].name)
        assertTrue(state.visibleItems[1] is TagItem.Pornstar)
        assertEquals("StarB", state.visibleItems[1].name)
    }

    @Test
    fun `expanded mode returns all items with hiddenCount 0 and canToggle true`() {
        val tags = TagsModel(
            mainUploader = listOf(TagsMainUploaderPornstar("href1", "ChannelA", "10k")),
            pornstars = listOf(TagsMainUploaderPornstar("href2", "StarB", "5k")),
            tags = listOf("tagC", "tagA", "tagB"),
        )
        // Total items = 5.

        val state = computeVisibleTags(tags, isExpanded = true)
        assertEquals(5, state.visibleItems.size)
        assertEquals(0, state.hiddenCount)
        assertTrue(state.canToggle)
        assertTrue(state.isExpanded)

        // Channels first, then pornstars, then alphabetically sorted tags
        assertEquals("ChannelA", state.visibleItems[0].name)
        assertEquals("StarB", state.visibleItems[1].name)
        assertEquals("tagA", state.visibleItems[2].name)
        assertEquals("tagB", state.visibleItems[3].name)
        assertEquals("tagC", state.visibleItems[4].name)
    }

    @Test
    fun `sanitizes blank items and deduplicates keywords`() {
        val tags = TagsModel(
            mainUploader = listOf(
                TagsMainUploaderPornstar("h1", "ChannelA", "10k"),
                TagsMainUploaderPornstar("h2", "   ", "0"),
            ),
            pornstars = listOf(
                TagsMainUploaderPornstar("h3", "", "5k"),
                TagsMainUploaderPornstar("h4", "StarA", "5k"),
            ),
            tags = listOf("duplicate", "  ", "duplicate", "apple"),
        )

        val state = computeVisibleTags(tags, isExpanded = true)
        // Expected valid items: ChannelA, StarA, apple, duplicate (4 items)
        assertEquals(4, state.visibleItems.size)
        assertEquals(listOf("ChannelA", "StarA", "apple", "duplicate"), state.visibleItems.map { it.name })
    }

    @Test
    fun `custom collapsed limit and expand threshold are respected`() {
        val tags = TagsModel(
            tags = listOf("a", "b", "c", "d", "e"),
        )

        val state = computeVisibleTags(tags, isExpanded = false, collapsedLimit = 3, expandThreshold = 2)
        assertEquals(3, state.visibleItems.size)
        assertEquals(2, state.hiddenCount)
        assertTrue(state.canToggle)
    }

    @Test
    fun `prunes keywords that duplicate channel or pornstar names case-insensitively`() {
        val tags = TagsModel(
            mainUploader = listOf(TagsMainUploaderPornstar("h1", "TopChannel", "10k")),
            pornstars = listOf(TagsMainUploaderPornstar("h2", "FamousStar", "5k")),
            tags = listOf("topchannel", "FamousStar", "unique_tag"),
        )

        val state = computeVisibleTags(tags, isExpanded = true)
        // Should only contain Channel(TopChannel), Pornstar(FamousStar), and Keyword(unique_tag)
        assertEquals(3, state.visibleItems.size)
        assertTrue(state.visibleItems[0] is TagItem.Channel)
        assertEquals("TopChannel", state.visibleItems[0].name)
        assertTrue(state.visibleItems[1] is TagItem.Pornstar)
        assertEquals("FamousStar", state.visibleItems[1].name)
        assertTrue(state.visibleItems[2] is TagItem.Keyword)
        assertEquals("unique_tag", state.visibleItems[2].name)
    }
}
