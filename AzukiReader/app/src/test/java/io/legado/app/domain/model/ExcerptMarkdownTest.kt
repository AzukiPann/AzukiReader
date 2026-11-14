package io.legado.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class ExcerptMarkdownTest {
    @Test fun `all books are grouped by author and passages are ordered with multiline quotes preserved`() {
        val markdown = excerptsToMarkdown(listOf(
            ExcerptEntry("同名书", "乙", "终章", 8, 3, "另一位作者的句子"),
            ExcerptEntry("同名书", "甲", "第二章", 1, 50, "第二章摘录"),
            ExcerptEntry("同名书", "甲", "第一章", 0, 1, "第一句\r\n第二句\r第三句", "一条笔记"),
            ExcerptEntry("空定位书签", "", "", 0, 0, "  "),
        ))
        assertTrue(markdown.contains("共 3 条摘录"))
        assertTrue(markdown.contains("作者：甲"))
        assertTrue(markdown.contains("作者：乙"))
        assertTrue(markdown.contains("> 第一句\n> 第二句\n> 第三句\n"))
        assertTrue(markdown.indexOf("第一章") < markdown.indexOf("第二章"))
        assertTrue(markdown.contains("备注：\n\n一条笔记"))
        assertFalse(markdown.contains("空定位书签"))
    }

    @Test fun `stored markdown and HTML remain literal text and empty library still produces a document`() {
        val markdown = excerptsToMarkdown(listOf(ExcerptEntry("# 标题\n新行", "作者", "[章节]", 0, 0,
            "**句子** <script> & [链接](x)\n> 第二行")))
        assertTrue(markdown.contains("## \\# 标题 新行"))
        assertTrue(markdown.contains("\\*\\*句子\\*\\* &lt;script&gt; &amp; \\[链接\\](x)"))
        assertTrue(markdown.contains("> &gt; 第二行"))
        assertTrue(excerptsToMarkdown(emptyList()).contains("共 0 条摘录\n\n暂无摘录。"))
    }
}
