package eu.tutorials.mybizz.pdfgen

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.Reporting.BillReportItem
import eu.tutorials.mybizz.Reporting.MonthlyReport
import eu.tutorials.mybizz.Reporting.MonthlyReportSummary
import eu.tutorials.mybizz.Reporting.RentalReportItem
import eu.tutorials.mybizz.Reporting.TaskReportItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595 
        private const val PAGE_HEIGHT = 842 
        private const val MARGIN = 40f
        private const val TITLE_SIZE = 24f
        private const val HEADING_SIZE = 18f
        private const val SUBHEADING_SIZE = 14f
        private const val BODY_SIZE = 10f
        private const val SMALL_SIZE = 8f
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun generateMonthlyReport(monthlyReport: MonthlyReport, selectedMonth: String): File? {
        val pdfDocument = PdfDocument()
        var currentPage = 1
        var pageInfo: PdfDocument.PageInfo
        var page: PdfDocument.Page
        var canvas: Canvas
        var yPos: Float

        try {
            // Page 1: Cover and Financial Overview
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage++).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = drawCoverPage(canvas, selectedMonth)
            yPos = drawFinancialOverview(canvas, monthlyReport, yPos)
            pdfDocument.finishPage(page)

            // Page 2: Bills Report
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage++).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = drawPageHeader(canvas, "Bills Report - $selectedMonth")
            yPos = drawBillsSummary(canvas, monthlyReport.billsSummary, yPos)
            yPos = drawBillsTable(canvas, monthlyReport.billsSummary.items.filterIsInstance<BillReportItem>(), yPos)

            // If bills table doesn't fit, create additional pages
            if (yPos > PAGE_HEIGHT - 100) {
                pdfDocument.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage++).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = MARGIN + 50
            }
            pdfDocument.finishPage(page)

            // Page 3: Rentals Report
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage++).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = drawPageHeader(canvas, "Rentals Report - $selectedMonth")
            yPos = drawRentalsSummary(canvas, monthlyReport.rentalsSummary, yPos)
            yPos = drawRentalsTable(canvas, monthlyReport.rentalsSummary.items.filterIsInstance<RentalReportItem>(), yPos)
            pdfDocument.finishPage(page)

            // Page 4: Tasks Report
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage++).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = drawPageHeader(canvas, "Tasks Report - $selectedMonth")
            yPos = drawTasksSummary(canvas, monthlyReport.tasksSummary, yPos)
            yPos = drawTasksTable(canvas, monthlyReport.tasksSummary.items.filterIsInstance<TaskReportItem>(), yPos)
            pdfDocument.finishPage(page)

            // Saving the document
            val fileName = "MonthlyReport_${selectedMonth.replace("-", "_")}_${System.currentTimeMillis()}.pdf"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                Toast.makeText(
                    context,
                    "PDF saved in Downloads folder",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_LONG).show()
            }

            pdfDocument.close()

            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)

            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            Toast.makeText(context, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            return file

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating PDF: ${e.message}", Toast.LENGTH_LONG).show()
            pdfDocument.close()
            return null
        }
    }

    private fun drawCoverPage(canvas: Canvas, selectedMonth: String): Float {
        var yPos = PAGE_HEIGHT / 3f

        // Title
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            textSize = TITLE_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Monthly Business Report", PAGE_WIDTH / 2f, yPos, titlePaint)
        yPos += 40

        // Months
        val monthPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = HEADING_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val monthDisplay = formatMonthDisplay(selectedMonth)
        canvas.drawText(monthDisplay, PAGE_WIDTH / 2f, yPos, monthPaint)
        yPos += 60

        // Generated date
        val datePaint = Paint().apply {
            color = Color.GRAY
            textSize = BODY_SIZE
            textAlign = Paint.Align.CENTER
        }
        val currentDate = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $currentDate", PAGE_WIDTH / 2f, yPos, datePaint)
        yPos += 80

        // Decorative line
        val linePaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            strokeWidth = 3f
        }
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, linePaint)

        return yPos + 40
    }

    private fun drawFinancialOverview(canvas: Canvas, report: MonthlyReport, startY: Float): Float {
        var yPos = startY

        // Section heading
        val headingPaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            textSize = HEADING_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Financial Overview", MARGIN, yPos, headingPaint)
        yPos += 30

        // Draw boxes for Income, Expenses, and Net
        val boxWidth = (PAGE_WIDTH - (3 * MARGIN)) / 3f
        val boxHeight = 80f
        val netAmount = report.getNetAmount()

        // Income Box
        drawFinancialBox(
            canvas,
            MARGIN,
            yPos,
            boxWidth,
            boxHeight,
            "Total Income",
            report.getTotalIncome(),
            Color.parseColor("#4CAF50")
        )

        // Expenses Box
        drawFinancialBox(
            canvas,
            MARGIN + boxWidth + 10,
            yPos,
            boxWidth,
            boxHeight,
            "Total Expenses",
            report.getTotalExpenses(),
            Color.parseColor("#F44336")
        )

        // Net Box
        val netColor = if (netAmount >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        val netLabel = if (netAmount >= 0) "Net Profit" else "Net Loss"
        drawFinancialBox(
            canvas,
            MARGIN + (boxWidth + 10) * 2,
            yPos,
            boxWidth,
            boxHeight,
            netLabel,
            kotlin.math.abs(netAmount),
            netColor
        )

        return yPos + boxHeight + 40
    }

    private fun drawFinancialBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        amount: Double,
        color: Int
    ) {
        // Box background
        val boxPaint = Paint().apply {
            this.color = Color.argb(20, Color.red(color), Color.green(color), Color.blue(color))
            style = Paint.Style.FILL
        }
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, 8f, 8f, boxPaint)

        // Box border
        val borderPaint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        // Label
        val labelPaint = Paint().apply {
            this.color = Color.DKGRAY
            textSize = SMALL_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(label, x + 10, y + 25, labelPaint)

        // Amount
        val amountPaint = Paint().apply {
            this.color = color
            textSize = SUBHEADING_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val amountText = "₹${String.format("%,.2f", amount)}"
        canvas.drawText(amountText, x + 10, y + 55, amountPaint)
    }

    private fun drawPageHeader(canvas: Canvas, title: String): Float {
        var yPos = MARGIN

        // Title
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            textSize = HEADING_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, MARGIN, yPos, titlePaint)
        yPos += 10

        // Line
        val linePaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, linePaint)

        return yPos + 30
    }

    private fun drawBillsSummary(canvas: Canvas, summary: MonthlyReportSummary, startY: Float): Float {
        return drawSummaryBoxes(
            canvas,
            startY,
            summary.totalAmount,
            summary.paidAmount,
            summary.unpaidAmount,
            summary.totalCount,
            summary.paidCount,
            summary.unpaidCount,
            true
        )
    }

    private fun drawRentalsSummary(canvas: Canvas, summary: MonthlyReportSummary, startY: Float): Float {
        return drawSummaryBoxes(
            canvas,
            startY,
            summary.totalAmount,
            summary.paidAmount,
            summary.unpaidAmount,
            summary.totalCount,
            summary.paidCount,
            summary.unpaidCount,
            false
        )
    }

    private fun drawTasksSummary(canvas: Canvas, summary: MonthlyReportSummary, startY: Float): Float {
        var yPos = startY

        val boxWidth = (PAGE_WIDTH - (3 * MARGIN)) / 3f
        val boxHeight = 60f

        // Total Tasks
        drawCountBox(canvas, MARGIN, yPos, boxWidth, boxHeight, "Total Tasks", summary.totalCount, Color.DKGRAY)

        // Completed
        drawCountBox(canvas, MARGIN + boxWidth + 10, yPos, boxWidth, boxHeight, "Completed", summary.paidCount, Color.parseColor("#4CAF50"))

        // Pending
        drawCountBox(canvas, MARGIN + (boxWidth + 10) * 2, yPos, boxWidth, boxHeight, "Pending", summary.unpaidCount, Color.parseColor("#FF9800"))

        return yPos + boxHeight + 30
    }

    private fun drawSummaryBoxes(
        canvas: Canvas,
        startY: Float,
        totalAmount: Double,
        paidAmount: Double,
        unpaidAmount: Double,
        totalCount: Int,
        paidCount: Int,
        unpaidCount: Int,
        isExpense: Boolean
    ): Float {
        var yPos = startY

        val boxWidth = (PAGE_WIDTH - (3 * MARGIN)) / 3f
        val boxHeight = 60f
        val paidColor = if (isExpense) Color.parseColor("#F44336") else Color.parseColor("#4CAF50")

        // Total
        drawAmountBox(canvas, MARGIN, yPos, boxWidth, boxHeight, "Total", totalAmount, Color.DKGRAY)

        // Paid
        drawAmountBox(canvas, MARGIN + boxWidth + 10, yPos, boxWidth, boxHeight, "Paid", paidAmount, paidColor)

        // Unpaid
        drawAmountBox(canvas, MARGIN + (boxWidth + 10) * 2, yPos, boxWidth, boxHeight, "Unpaid", unpaidAmount, Color.parseColor("#FF9800"))

        yPos += boxHeight + 20

        // Counts
        drawCountBox(canvas, MARGIN, yPos, boxWidth, boxHeight, "Total Count", totalCount, Color.DKGRAY)
        drawCountBox(canvas, MARGIN + boxWidth + 10, yPos, boxWidth, boxHeight, "Paid Count", paidCount, paidColor)
        drawCountBox(canvas, MARGIN + (boxWidth + 10) * 2, yPos, boxWidth, boxHeight, "Unpaid Count", unpaidCount, Color.parseColor("#FF9800"))

        return yPos + boxHeight + 30
    }

    private fun drawAmountBox(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, label: String, amount: Double, color: Int) {
        val boxPaint = Paint().apply {
            this.color = Color.argb(10, Color.red(color), Color.green(color), Color.blue(color))
            style = Paint.Style.FILL
        }
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, 5f, 5f, boxPaint)

        val labelPaint = Paint().apply {
            this.color = Color.GRAY
            textSize = SMALL_SIZE
        }
        canvas.drawText(label, x + 8, y + 20, labelPaint)

        val amountPaint = Paint().apply {
            this.color = color
            textSize = BODY_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("₹${String.format("%,.2f", amount)}", x + 8, y + 40, amountPaint)
    }

    private fun drawCountBox(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, label: String, count: Int, color: Int) {
        val boxPaint = Paint().apply {
            this.color = Color.argb(10, Color.red(color), Color.green(color), Color.blue(color))
            style = Paint.Style.FILL
        }
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, 5f, 5f, boxPaint)

        val labelPaint = Paint().apply {
            this.color = Color.GRAY
            textSize = SMALL_SIZE
        }
        canvas.drawText(label, x + 8, y + 20, labelPaint)

        val countPaint = Paint().apply {
            this.color = color
            textSize = SUBHEADING_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(count.toString(), x + 8, y + 45, countPaint)
    }

    private fun drawBillsTable(canvas: Canvas, bills: List<BillReportItem>, startY: Float): Float {
        var yPos = startY

        // Table heading
        val headingPaint = Paint().apply {
            color = Color.BLACK
            textSize = SUBHEADING_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Bills Details", MARGIN, yPos, headingPaint)
        yPos += 25

        if (bills.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = Color.GRAY
                textSize = BODY_SIZE
            }
            canvas.drawText("No bills for this month", MARGIN, yPos, emptyPaint)
            return yPos + 20
        }

        // Table header background
        val headerPaint = Paint().apply {
            color = Color.parseColor("#F5F7FA")
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 25, headerPaint)

        // Table headers
        val headerTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = SMALL_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val col1X = MARGIN + 5
        val col2X = MARGIN + 60
        val col3X = MARGIN + 180
        val col4X = MARGIN + 280
        val col5X = MARGIN + 370
        val col6X = MARGIN + 450

        canvas.drawText("Bill #", col1X, yPos + 15, headerTextPaint)
        canvas.drawText("Title", col2X, yPos + 15, headerTextPaint)
        canvas.drawText("Category", col3X, yPos + 15, headerTextPaint)
        canvas.drawText("Amount", col4X, yPos + 15, headerTextPaint)
        canvas.drawText("Status", col5X, yPos + 15, headerTextPaint)
        canvas.drawText("Date", col6X, yPos + 15, headerTextPaint)

        yPos += 25

        // Table rows
        val cellPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = SMALL_SIZE
        }

        bills.take(15).forEach { bill ->  // Limit to prevent overflow
            // Alternate row background
            if (bills.indexOf(bill) % 2 == 0) {
                val rowBgPaint = Paint().apply {
                    color = Color.parseColor("#FAFAFA")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 20, rowBgPaint)
            }

            canvas.drawText(truncateText(bill.billNumber, 8), col1X, yPos + 14, cellPaint)
            canvas.drawText(truncateText(bill.title, 18), col2X, yPos + 14, cellPaint)
            canvas.drawText(truncateText(bill.category, 12), col3X, yPos + 14, cellPaint)
            canvas.drawText("₹${String.format("%,.0f", bill.amount)}", col4X, yPos + 14, cellPaint)

            val statusPaint = Paint().apply {
                color = if (bill.status == Bill.STATUS_PAID) Color.parseColor("#4CAF50") else Color.parseColor("#FF9800")
                textSize = SMALL_SIZE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(bill.status, col5X, yPos + 14, statusPaint)
            canvas.drawText(truncateText(bill.dueDate, 10), col6X, yPos + 14, cellPaint)

            yPos += 20
        }

        return yPos + 20
    }

    private fun drawRentalsTable(canvas: Canvas, rentals: List<RentalReportItem>, startY: Float): Float {
        var yPos = startY

        // Table heading
        val headingPaint = Paint().apply {
            color = Color.BLACK
            textSize = SUBHEADING_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Rentals Details", MARGIN, yPos, headingPaint)
        yPos += 25

        if (rentals.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = Color.GRAY
                textSize = BODY_SIZE
            }
            canvas.drawText("No rentals for this month", MARGIN, yPos, emptyPaint)
            return yPos + 20
        }

        // Table header
        val headerPaint = Paint().apply {
            color = Color.parseColor("#F5F7FA")
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 25, headerPaint)

        val headerTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = SMALL_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val col1X = MARGIN + 5
        val col2X = MARGIN + 120
        val col3X = MARGIN + 260
        val col4X = MARGIN + 360
        val col5X = MARGIN + 450

        canvas.drawText("Tenant Name", col1X, yPos + 15, headerTextPaint)
        canvas.drawText("Property", col2X, yPos + 15, headerTextPaint)
        canvas.drawText("Amount", col3X, yPos + 15, headerTextPaint)
        canvas.drawText("Status", col4X, yPos + 15, headerTextPaint)
        canvas.drawText("Month", col5X, yPos + 15, headerTextPaint)

        yPos += 25

        // Table rows
        val cellPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = SMALL_SIZE
        }

        rentals.take(15).forEach { rental ->
            if (rentals.indexOf(rental) % 2 == 0) {
                val rowBgPaint = Paint().apply {
                    color = Color.parseColor("#FAFAFA")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 20, rowBgPaint)
            }

            canvas.drawText(truncateText(rental.tenantName, 15), col1X, yPos + 14, cellPaint)
            canvas.drawText(truncateText(rental.property, 15), col2X, yPos + 14, cellPaint)
            canvas.drawText("₹${String.format("%,.0f", rental.amount)}", col3X, yPos + 14, cellPaint)

            val statusPaint = Paint().apply {
                color = if (rental.status == Rental.STATUS_PAID) Color.parseColor("#4CAF50") else Color.parseColor("#FF9800")
                textSize = SMALL_SIZE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(rental.status, col4X, yPos + 14, statusPaint)
            canvas.drawText(truncateText(rental.month, 10), col5X, yPos + 14, cellPaint)

            yPos += 20
        }

        return yPos + 20
    }

    private fun drawTasksTable(canvas: Canvas, tasks: List<TaskReportItem>, startY: Float): Float {
        var yPos = startY

        // Table heading
        val headingPaint = Paint().apply {
            color = Color.BLACK
            textSize = SUBHEADING_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Tasks Details", MARGIN, yPos, headingPaint)
        yPos += 25

        if (tasks.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = Color.GRAY
                textSize = BODY_SIZE
            }
            canvas.drawText("No tasks for this month", MARGIN, yPos, emptyPaint)
            return yPos + 20
        }

        // Table header
        val headerPaint = Paint().apply {
            color = Color.parseColor("#F5F7FA")
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 25, headerPaint)

        val headerTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = SMALL_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val col1X = MARGIN + 5
        val col2X = MARGIN + 180
        val col3X = MARGIN + 320
        val col4X = MARGIN + 420
        val col5X = MARGIN + 480

        canvas.drawText("Title", col1X, yPos + 15, headerTextPaint)
        canvas.drawText("Assigned To", col2X, yPos + 15, headerTextPaint)
        canvas.drawText("Due Date", col3X, yPos + 15, headerTextPaint)
        canvas.drawText("Status", col4X, yPos + 15, headerTextPaint)

        yPos += 25

        // Table rows
        val cellPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = SMALL_SIZE
        }

        tasks.take(20).forEach { task ->
            if (tasks.indexOf(task) % 2 == 0) {
                val rowBgPaint = Paint().apply {
                    color = Color.parseColor("#FAFAFA")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 20, rowBgPaint)
            }

            canvas.drawText(truncateText(task.title, 25), col1X, yPos + 14, cellPaint)
            canvas.drawText(truncateText(task.assignedTo, 18), col2X, yPos + 14, cellPaint)
            canvas.drawText(truncateText(task.dueDate, 12), col3X, yPos + 14, cellPaint)

            val statusPaint = Paint().apply {
                color = if (task.status.equals("completed", ignoreCase = true))
                    Color.parseColor("#4CAF50") else Color.parseColor("#FF9800")
                textSize = SMALL_SIZE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(task.status, col4X, yPos + 14, statusPaint)

            yPos += 20
        }

        return yPos + 20
    }

    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength - 3) + "..."
        } else {
            text
        }
    }

    private fun formatMonthDisplay(month: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = sdf.parse(month)
            val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            displayFormat.format(date ?: Date())
        } catch (e: Exception) {
            month
        }
    }
}
