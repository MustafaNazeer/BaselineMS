package com.mustafanazeer.baselinems.report.pdf

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Picture
import android.graphics.RectF
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
    private val pageTextDrawCounts: MutableList<Int> = mutableListOf()
    private val pictures: MutableList<Picture> = mutableListOf()
    private val countingCanvases: MutableList<CountingCanvas> = mutableListOf()
    private var currentPage: PdfDocument.Page? = null
    private var currentPicture: Picture? = null
    private var currentCountingCanvas: CountingCanvas? = null
    private var closed: Boolean = false

    @Implementation
    fun startPage(info: PdfDocument.PageInfo): PdfDocument.Page {
        check(!closed) { "document is closed!" }
        check(currentPage == null) { "Previous page not finished." }
        val picture = Picture()
        val pictureCanvas: Canvas = picture.beginRecording(info.pageWidth, info.pageHeight)
        val countingCanvas = CountingCanvas(pictureCanvas)
        val pageCtor = PdfDocument.Page::class.java.getDeclaredConstructor(
            Canvas::class.java,
            PdfDocument.PageInfo::class.java
        )
        pageCtor.isAccessible = true
        val page = pageCtor.newInstance(countingCanvas, info)
        currentPage = page
        currentPicture = picture
        currentCountingCanvas = countingCanvas
        return page
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {
        check(!closed) { "document is closed!" }
        require(page === currentPage) { "invalid page" }
        currentPicture?.endRecording()
        pages.add(page.info)
        currentPicture?.let { pictures.add(it) }
        currentCountingCanvas?.let {
            countingCanvases.add(it)
            pageTextDrawCounts.add(it.textDrawCount)
        }
        currentPage = null
        currentPicture = null
        currentCountingCanvas = null
    }

    @Implementation
    fun writeTo(out: OutputStream) {
        check(!closed) { "document is closed!" }
        out.write(MINIMAL_PDF_HEADER)
        val body = "% pages=${pages.size}\n".toByteArray(Charsets.US_ASCII)
        out.write(body)
        pageTextDrawCounts.forEachIndexed { index, count ->
            val line = "% page${index + 1}_text_draws=$count\n".toByteArray(Charsets.US_ASCII)
            out.write(line)
        }
        out.write(MINIMAL_PDF_FOOTER)
    }

    @Implementation
    fun getPages(): List<PdfDocument.PageInfo> = pages.toList()

    @Implementation
    fun close() {
        closed = true
        currentPage = null
        currentPicture = null
        currentCountingCanvas = null
    }

    companion object {
        private val MINIMAL_PDF_HEADER: ByteArray = byteArrayOf(
            0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A
        )
        private val MINIMAL_PDF_FOOTER: ByteArray = "%%EOF\n".toByteArray(Charsets.US_ASCII)
    }
}

internal class CountingCanvas(private val delegate: Canvas) : Canvas() {
    var textDrawCount: Int = 0
        private set

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        textDrawCount += 1
        delegate.drawText(text, x, y, paint)
    }

    override fun drawText(text: CharSequence, start: Int, end: Int, x: Float, y: Float, paint: Paint) {
        textDrawCount += 1
        delegate.drawText(text, start, end, x, y, paint)
    }

    override fun drawText(text: CharArray, index: Int, count: Int, x: Float, y: Float, paint: Paint) {
        textDrawCount += 1
        delegate.drawText(text, index, count, x, y, paint)
    }

    override fun drawText(text: String, start: Int, end: Int, x: Float, y: Float, paint: Paint) {
        textDrawCount += 1
        delegate.drawText(text, start, end, x, y, paint)
    }

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        delegate.drawLine(startX, startY, stopX, stopY, paint)
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        delegate.drawCircle(cx, cy, radius, paint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        delegate.drawRect(left, top, right, bottom, paint)
    }

    override fun drawRect(rect: RectF, paint: Paint) {
        delegate.drawRect(rect, paint)
    }

    override fun drawPath(path: Path, paint: Paint) {
        delegate.drawPath(path, paint)
    }
}
