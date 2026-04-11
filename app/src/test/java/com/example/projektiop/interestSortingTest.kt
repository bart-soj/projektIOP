package com.example.projektiop

import com.example.projektiop.domain.OtsukaOchiaiSimilarityImpl
import org.junit.Test

import org.junit.Assert.*
import kotlin.math.sqrt

/**
 *
 * testing implementation of interest similarity
 *
 */
class InterestSimilarityUnitTest{
    val toTest = OtsukaOchiaiSimilarityImpl()

    @Test
    fun `identical set similarity should equal 1`(): Unit {
        assertEquals(1.0, toTest.interestSimilarity<String>(
                a = setOf("name1", "name2"),
                b = setOf("name1", "name2"),
            ), 0.0
        )
    }

    @Test
    fun `empty set similarity should equal 1`(): Unit {
        assertEquals(0.0, toTest.interestSimilarity<String>(
                a = emptySet(),
                b = emptySet()
            ), 0.0
        )
    }

    @Test
    fun `sets without same members similarity should equal 0`(): Unit {
            assertEquals(0.0, toTest.interestSimilarity<String>(
                    a = setOf("name1", "name2"),
                    b = setOf("name3", "name4")
                ), 0.0
            )
    }

    @Test
    fun `one common member same size similarity test`(): Unit {
        assertEquals(0.5, toTest.interestSimilarity<String>(
                a = setOf("name1", "name2"),
                b = setOf("name1", "name3")
            ), 0.0
        )
    }
}