package com.example.projektiop

import org.junit.Test

import org.junit.Assert.*
import kotlin.math.sqrt

/**
 *
 * testing of sorting discovered users by interest similarity
 *
 */
class InterestSortingUnitTest {
    fun <T> cosineSimilarity(a: Set<Any?>, b: Set<Any?>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val intersectionSize = a.intersect(b).size
        return intersectionSize / sqrt(a.size.toDouble() * b.size.toDouble())
    }


    @Test
    fun `identical set similarity should equal 1`(): Unit {
        assertEquals(1, cosineSimilarity<String>(
                a = setOf("name1", "name2"),
                b = setOf("name1", "name2"),
            )
        )
    }

    @Test
    fun `empty set similarity should equal 1`(): Unit {
            assertEquals(1, cosineSimilarity<String>(
                a = emptySet(),
                b = emptySet()
            )
        )
    }

    @Test
    fun `sets without same members similarity should equal 0`(): Unit {
            assertEquals(0, cosineSimilarity<String>(
                    a = setOf("name1", "name2"),
                    b = setOf("name3", "name4")
                )
            )
    }

    @Test
    fun `one common member same size similarity test`(): Unit {
        assertEquals(0.5, cosineSimilarity<String>(
            a = setOf("name1", "name2"),
            b = setOf("name1", "name3")
        )
        )
    }
}