package dev.spark.shared

import dev.spark.shared.util.Result
import dev.spark.shared.util.getOrNull
import dev.spark.shared.util.getOrThrow
import dev.spark.shared.util.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ResultTest {

    @Test
    fun `Success getOrNull returns data`() {
        val result = Result.Success("hello")
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun `Loading getOrNull returns null`() {
        val result: Result<String> = Result.Loading
        assertNull(result.getOrNull())
    }

    @Test
    fun `Error getOrNull returns null`() {
        val result: Result<String> = Result.Error(RuntimeException("boom"))
        assertNull(result.getOrNull())
    }

    @Test
    fun `Success getOrThrow returns data`() {
        val result = Result.Success(42)
        assertEquals(42, result.getOrThrow())
    }

    @Test
    fun `Error getOrThrow throws exception`() {
        val exception = RuntimeException("test error")
        val result: Result<Int> = Result.Error(exception)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `map transforms Success data`() {
        val result = Result.Success(5)
        val mapped = result.map { it * 2 }
        assertEquals(Result.Success(10), mapped)
    }

    @Test
    fun `map preserves Error state`() {
        val error = RuntimeException("err")
        val result: Result<Int> = Result.Error(error)
        val mapped = result.map { it * 2 }
        assertEquals(error, (mapped as Result.Error).exception)
    }
}
