package com.example.solochef.storage

import com.example.solochef.model.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.nio.file.Files

/**
 * LocalFileManager 核心功能自动化测试
 *
 * 覆盖场景：
 * 1. 菜谱保存 → 读取 → 验证字段完整
 * 2. 食材分类 mapping（物料不会丢失）
 * 3. 备份导出 → 导入 → 数据完整
 * 4. 损坏文件不会导致崩溃
 *
 * 运行方式（项目根目录）：
 *   export JAVA_HOME=/Users/kaka/.workbuddy/binaries/java/jdk-17.0.19+10/Contents/Home
 *   cd solochef-new/android
 *   ./gradlew :app:testReleaseUnitTest --tests "com.example.solochef.storage.LocalFileManagerTest"
 */
class LocalFileManagerTest {

    private lateinit var testDir: File
    private lateinit var storage: LocalFileManager

    @Before
    fun setUp() {
        testDir = Files.createTempDirectory("solochef-test").toFile()
        storage = LocalFileManager(testDir)
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    // ═══════════════════════════════════════════════════
    // 测试 1: 保存菜谱 → 重新读取 → 所有字段都不丢
    // ═══════════════════════════════════════════════════
    @Test
    fun `保存带物料的菜谱后，读取时物料不丢失`() = runTest {
        val recipe = Recipe(
            id = "test-001",
            name = "红烧狮子头",
            materials = mapOf(
                "meat" to listOf(Material(item = "猪肉", amount = "500", unit = "g")),
                "vegetable" to listOf(Material(item = "荸荠", amount = "100", unit = "g")),
                "seasoning" to listOf(Material(item = "生抽", amount = "30", unit = "ml"))
            ),
            tags = listOf("红烧", "淮扬菜"),
            energy_level = EnergyLevel.Mid,
            cost = 38.0,
            price = 88.0,
            description = "淮扬经典名菜"
        )

        storage.saveRecipe(recipe)
        val loaded = storage.getAllRecipes()

        assertEquals(1, loaded.size)
        val r = loaded.first()
        assertEquals("红烧狮子头", r.name)
        assertEquals(38.0, r.cost, 0.01)
        assertEquals(88.0, r.price, 0.01)
        assertEquals("淮扬经典名菜", r.description)
        assertEquals(2, r.tags.size)
        assertEquals(EnergyLevel.Mid, r.energy_level)

        // ★ 这三行就是今天修的 Bug —— 物料不能在保存后偷偷消失
        assertEquals("肉禽物料数", 1, r.materials["meat"]?.size)
        assertEquals("蔬菜物料数", 1, r.materials["vegetable"]?.size)
        assertEquals("调味物料数", 1, r.materials["seasoning"]?.size)
        assertEquals("猪肉", r.materials["meat"]!![0].item)
    }

    // ═══════════════════════════════════════════════════
    // 测试 2: 编辑菜谱改名称，物料不能被覆盖
    // ═══════════════════════════════════════════════════
    @Test
    fun `编辑菜谱只改名称，物料不应丢失`() = runTest {
        val original = Recipe(
            id = "test-002",
            name = "原始菜谱",
            materials = mapOf("meat" to listOf(Material(item = "牛肉", amount = "300", unit = "g")))
        )
        storage.saveRecipe(original)

        // 模拟编辑：只改名称
        storage.saveRecipe(original.copy(name = "改名的菜谱"))

        val r = storage.getAllRecipes().first()
        assertEquals("改名的菜谱", r.name)
        assertEquals("物料不应被清空", 1, r.materials["meat"]?.size ?: 0)
    }

    // ═══════════════════════════════════════════════════
    // 测试 3: 备份导出 → 导入 → 菜谱完整恢复
    // ═══════════════════════════════════════════════════
    @Test
    fun `导出备份再导入，菜谱数据不变`() = runTest {
        val recipe = Recipe(
            id = "test-003",
            name = "备份测试菜谱",
            materials = mapOf(
                "meat" to listOf(Material(item = "鸡腿", amount = "400", unit = "g")),
                "seasoning" to listOf(Material(item = "料酒", amount = "20", unit = "ml"))
            ),
            cost = 25.0, price = 55.0
        )
        storage.saveRecipe(recipe)

        // 导出
        val exported = storage.exportToMarkdown()
        assertTrue("应包含菜谱名", exported.contains("备份测试菜谱"))

        // 模拟换机：清空所有本地文件
        testDir.listFiles()?.filter { it.name != "_" }?.forEach { it.deleteRecursively() }
        assertEquals(0, storage.getAllRecipes().size)

        // 导入
        val restored = storage.importFromMarkdown(exported)
        assertNotNull("导入不应失败", restored)
        assertEquals(1, restored!!.recipes.size)

        val r = restored.recipes.first()
        assertEquals("备份测试菜谱", r.name)
        assertEquals(1, r.materials["meat"]?.size ?: 0)
        assertEquals(1, r.materials["seasoning"]?.size ?: 0)
    }

    // ═══════════════════════════════════════════════════
    // 测试 4: 文件损坏时，不会崩溃，只跳过坏文件
    // ═══════════════════════════════════════════════════
    @Test
    fun `损坏的菜谱文件不应导致崩溃`() = runTest {
        storage.saveRecipe(Recipe(id = "good-1", name = "好菜谱"))

        // 写一个垃圾文件进去
        File(testDir!!, "recipes/broken.md").apply { parentFile.mkdirs() }
            .writeText("这不是合法格式 {{{{{{{")

        val loaded = storage.getAllRecipes()
        assertEquals("只应返回合法菜谱", 1, loaded.size)
        assertEquals("好菜谱", loaded.first().name)
    }
}
