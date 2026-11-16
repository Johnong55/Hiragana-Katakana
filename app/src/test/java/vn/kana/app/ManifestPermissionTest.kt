package vn.kana.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Kiểm thử bắt buộc:
 * Đảm bảo AndroidManifest.xml (source & merged) KHÔNG cấp quyền android.permission.INTERNET.
 */
class ManifestPermissionTest {

    @Test
    fun `source AndroidManifest does not grant INTERNET permission`() {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        val manifestFile = candidates.firstOrNull { it.exists() }
            ?: error("Không tìm thấy AndroidManifest.xml")

        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifestFile)

        val permissionNodes = doc.getElementsByTagName("uses-permission")
        for (i in 0 until permissionNodes.length) {
            val node = permissionNodes.item(i)
            val attrs = node.attributes
            val nameAttr = attrs?.getNamedItem("android:name")?.nodeValue
            val toolsNodeAttr = attrs?.getNamedItem("tools:node")?.nodeValue

            if (nameAttr == "android.permission.INTERNET") {
                assertTrue(
                    "Nếu khai báo thẻ INTERNET trong source manifest thì bắt buộc phải là tools:node=\"remove\"",
                    toolsNodeAttr == "remove"
                )
            }
        }
    }

    @Test
    fun `merged debug AndroidManifest if built does not contain INTERNET permission`() {
        val mergedCandidates = listOf(
            File("build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml"),
            File("app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml")
        )
        val mergedManifest = mergedCandidates.firstOrNull { it.exists() } ?: return
        val content = mergedManifest.readText(Charsets.UTF_8)
        assertFalse(
            "Merged AndroidManifest.xml KHÔNG ĐƯỢC chứa quyền android.permission.INTERNET!",
            content.contains("android.permission.INTERNET")
        )
    }
}
