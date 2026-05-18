package com.mustafanazeer.baselinems.report.pdf

import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.pdf.PdfDocument
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import java.io.OutputStream

@Implements(PdfDocument::class)
class ShadowPdfDocumentForTests {

    @RealObject
    private lateinit var realObject: PdfDocument

    private val pages: MutableList<PdfDocument.PageInfo> = mutableListOf()
    private val pictures: MutableList<Picture> = mutableListOf()
    private var currentPage: PdfDocument.Page? = null
    private var currentPicture: Picture? = null
    private var closed: Boolean = false

    @Implementation
    fun startPage(info: PdfDocument.PageInfo): PdfDocument.Page {
        check(!closed) { "document is closed!" }
        check(currentPage == null) { "Previous page not finished." }
        val picture = Picture()
        val canvas: Canvas = picture.beginRecording(info.pageWidth, info.pageHeight)
        val pageCtor = PdfDocument.Page::class.java.getDeclaredConstructor(
            Canvas::class.java,
            PdfDocument.PageInfo::class.java
        )
        pageCtor.isAccessible = true
        val page = pageCtor.newInstance(canvas, info)
        currentPage = page
        currentPicture = picture
        return page
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {
        check(!closed) { "document is closed!" }
        require(page === currentPage) { "invalid page" }
        currentPicture?.endRecording()
        pages.add(page.info)
        currentPicture?.let { pictures.add(it) }
        currentPage = null
        currentPicture = null
    }

    @Implementation
    fun writeTo(out: OutputStream) {
        check(!closed) { "document is closed!" }
        out.write(MINIMAL_PDF_HEADER)
        val body = "% pages=${pages.size}\n".toByteArray(Charsets.US_ASCII)
        out.write(body)
        out.write(MINIMAL_PDF_FOOTER)
    }

    @Implementation
    fun getPages(): List<PdfDocument.PageInfo> = pages.toList()

    @Implementation
    fun close() {
        closed = true
        currentPage = null
        currentPicture = null
    }

    companion object {
        private val MINIMAL_PDF_HEADER: ByteArray = byteArrayOf(
            0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A
        )
        private val MINIMAL_PDF_FOOTER: ByteArray = "%%EOF\n".toByteArray(Charsets.US_ASCII)
    }
}
