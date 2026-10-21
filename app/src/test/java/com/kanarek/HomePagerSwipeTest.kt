package com.kanarek

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePagerSwipeTest {
    @Test fun swipeBetweenNewsAndPlayerIsAvailable() {
        assertTrue(homePagerSwipeEnabled(HomeActivity.PAGE_READER))
        assertTrue(homePagerSwipeEnabled(HomeActivity.PAGE_PLAYER))
    }

    @Test fun toolsOwnDragGesturesWithoutAccidentalPageExit() {
        assertFalse(homePagerSwipeEnabled(HomeActivity.PAGE_TOOLS))
    }
}
