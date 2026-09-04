package fr.suivimuscu.app

import fr.suivimuscu.app.ui.libraryMatchesQuery
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchTest {
    @Test
    fun emptyQueryMatchesEverything() {
        assertTrue(libraryMatchesQuery("Press pecs", ""))
        assertTrue(libraryMatchesQuery("Press pecs", "   "))
    }

    @Test
    fun matchIgnoresCaseAndSurroundingSpaces() {
        assertTrue(libraryMatchesQuery("Tirage vertical", "tirage"))
        assertTrue(libraryMatchesQuery("Tirage vertical", "  VERTICAL "))
        assertFalse(libraryMatchesQuery("Tirage vertical", "press"))
    }
}
