package com.shawarma.billing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ─── DATA MODELS ───────────────────────────────────────────────────────────────

data class BillItem(
    val name: String,
    val qty: Int,
    val price: Int,
    val total: Int,
    val category: String,
    val isCustom: Boolean = false
)

data class OrderHistory(
    val id: Long,
    val dateStr: String,
    val timeStr: String,
    val text: String,
    val items: Map<String, Int>,
    val itemDetails: Map<String, BillItemDetail>,
    val total: Int,
    val totalQty: Int
)

data class BillItemDetail(
    val qty: Int,
    val price: Int,
    val total: Int,
    val category: String
)

data class ParseResult(
    val items: MutableMap<String, Int>,
    val unknowns: MutableList<UnknownItem>
)

data class UnknownItem(val qty: Int, val name: String)

// ─── MAIN ACTIVITY ─────────────────────────────────────────────────────────────

class MainActivity : AppCompatActivity() {

    // Base menu prices
    private val BASE_PRICES = mapOf(
        "Regular Shawarma" to 90,
        "Chicken Tikka Shawarma" to 110,
        "Jumbo Shawarma" to 130,
        "Cheese Shawarma" to 100,
        "Tandoori Shawarma" to 110,
        "Cheesy Paneer Shawarma" to 100,
        "Chicken Schezwan Shawarma" to 90,
        "Tandoori Paneer Shawarma" to 100,
        "Rumali Jumbo" to 140,
        "Lazeez Special" to 110,
        "Special Paneer Shawarma" to 110,
        "Paneer Tikka Shawarma" to 110,
        "Open Special 2 Bread" to 170,
        "Schezwan With Cheese" to 100,
        "Open Shawarma" to 130,
        "Rumali Schezwan" to 110,
        "Bread Peta" to 10,
        "BBQ Peri Peri Shawarma" to 120,
        "Rumali Shawarma" to 110,
        "Rumali Cheese" to 120,
        "Rumali Tandoori" to 110,
        "Paneer Special Rumali" to 130,
        "Rumali Cheesy Paneer" to 110,
        "Rumali Regular" to 100,
        "Extra Cheese" to 20,
        "Achaari Masti Shawarma" to 110,
        "Peri Peri Shawarma" to 110,
        "Rumali Nagpur Special" to 130,
        "Cheese Dip" to 20,
        "Rumali Achari Shawarma" to 120,
        "Chicken Special Lazeez Shawarma" to 110,
        "Chicken Achari Masti Shawarma" to 100,
        "Chicken Tandoori Shawarma" to 100,
        "Chicken Open Shawarma" to 130,
        "Chicken Cheese Shawarma" to 90,
        "Chicken Rumali Special Lazeez" to 120,
        "Chicken Rumali Shawarma" to 100,
        "Chicken Schezwan With Cheese Shawarma" to 100,
        "Chicken Jumbo Shawarma" to 120,
        "Chicken Rumali Tikka Shawarma" to 110,
        "Chicken Nagpur Special Shawarma" to 120,
        "Chicken Rumali Cheese Shawarma" to 100,
        "Chicken BBQ Peri Peri Shawarma" to 120,
        "Veg Cheesy Paneer Shawarma" to 90,
        "Veg Special Paneer Shawarma" to 110,
        "Veg Paneer Tikka Shawarma" to 110,
        "Veg Tandoori Paneer Shawarma" to 100,
        "Open Special Shawarma 2 Pita Bread" to 170,
        "Achari Masti Open Shawarma" to 140
    )

    private val savedItems = mutableMapOf<String, Int>()
    private val orderHistory = mutableListOf<OrderHistory>()
    private val PRICES get() = BASE_PRICES.toMutableMap().also { it.putAll(savedItems) }

    private var currentResult: ParseResult? = null
    private var billItems = listOf<BillItem>()
    private var tempPrices = mutableMapOf<String, Int>()
    private var tempNames = mutableMapOf<String, String>()
    private var newlyAdded = listOf<String>()
    private var currentTab = "input"

    private val PREFS = "shawarma_prefs"
    private val KEY_CUSTOM = "custom_prices"
    private val KEY_HISTORY = "order_history"

    private val SAMPLE = """1 x Regular Shawarma 2 x Chicken Tikka Shawarma 1 x Jumbo Shawarma
1 x Cheese Shawarma, 2 x Rumali Cheese, 1 x Chicken Jumbo Shawarma
Note: Extra spicy please
3 x Veg Paneer Tikka Shawarma 1 x Extra Cheese 2 x Cheese Dip"""

    // View refs
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var containerInput: View
    private lateinit var containerBill: View
    private lateinit var containerHistory: View
    private lateinit var containerMenu: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadFromPrefs()
        initViews()
        switchTab("input")
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottom_nav)
        containerInput = findViewById(R.id.container_input)
        containerBill = findViewById(R.id.container_bill)
        containerHistory = findViewById(R.id.container_history)
        containerMenu = findViewById(R.id.container_menu)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_input -> { switchTab("input"); true }
                R.id.nav_bill -> { switchTab("bill"); true }
                R.id.nav_history -> { switchTab("history"); true }
                R.id.nav_menu -> { switchTab("menu"); true }
                else -> false
            }
        }

        setupInputTab()
        setupBillTab()
        setupHistoryTab()
        setupMenuTab()
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        containerInput.visibility = if (tab == "input") View.VISIBLE else View.GONE
        containerBill.visibility = if (tab == "bill") View.VISIBLE else View.GONE
        containerHistory.visibility = if (tab == "history") View.VISIBLE else View.GONE
        containerMenu.visibility = if (tab == "menu") View.VISIBLE else View.GONE

        val itemId = when (tab) {
            "input" -> R.id.nav_input
            "bill" -> R.id.nav_bill
            "history" -> R.id.nav_history
            "menu" -> R.id.nav_menu
            else -> R.id.nav_input
        }
        bottomNav.selectedItemId = itemId

        when (tab) {
            "bill" -> renderBill()
            "history" -> renderHistory()
            "menu" -> renderMenu()
        }
    }

    // ─── INPUT TAB ─────────────────────────────────────────────────────────────

    private fun setupInputTab() {
        val orderTextInput = findViewById<TextInputEditText>(R.id.order_text_input)
        val processBtn = findViewById<MaterialButton>(R.id.btn_process)
        val sampleBtn = findViewById<MaterialButton>(R.id.btn_sample)
        val clearBtn = findViewById<MaterialButton>(R.id.btn_clear_input)

        processBtn.setOnClickListener {
            val text = orderTextInput.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                processOrder(text)
            } else {
                showSnack("Order text likhein pehle!")
            }
        }

        sampleBtn.setOnClickListener {
            orderTextInput.setText(SAMPLE)
        }

        clearBtn.setOnClickListener {
            orderTextInput.setText("")
            currentResult = null
            billItems = listOf()
            showSnack("Cleared!")
        }
    }

    private fun processOrder(text: String) {
        val result = parseOrders(text)
        currentResult = result

        if (result.unknowns.isNotEmpty()) {
            showUnknownItemsDialog(result, text)
        } else {
            billItems = getBillItems(result)
            switchTab("bill")
        }
    }

    // ─── UNKNOWN ITEMS DIALOG ──────────────────────────────────────────────────

    private fun showUnknownItemsDialog(result: ParseResult, originalText: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unknown_items, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.unknown_items_container)
        val tempFieldsMap = mutableMapOf<String, Pair<EditText, EditText>>() // name, price fields

        for (unknown in result.unknowns) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_unknown, container, false)
            val nameField = itemView.findViewById<EditText>(R.id.unknown_name)
            val priceField = itemView.findViewById<EditText>(R.id.unknown_price)
            val qtyLabel = itemView.findViewById<TextView>(R.id.unknown_qty)

            nameField.setText(unknown.name)
            qtyLabel.text = "Qty: ${unknown.qty}"
            tempFieldsMap[unknown.name] = Pair(nameField, priceField)
            container.addView(itemView)
        }

        AlertDialog.Builder(this)
            .setTitle("Naye Items Mile - Price Set Karein")
            .setView(dialogView)
            .setPositiveButton("Save & Bill") { _, _ ->
                for ((origName, fields) in tempFieldsMap) {
                    val finalName = fields.first.text.toString().trim()
                    val price = fields.second.text.toString().toIntOrNull() ?: 0
                    if (finalName.isNotEmpty() && price > 0) {
                        savedItems[finalName] = price
                        val qty = result.unknowns.find { it.name == origName }?.qty ?: 0
                        result.items[finalName] = (result.items[finalName] ?: 0) + qty
                        result.items.remove(origName)
                        newlyAdded = newlyAdded + finalName
                    }
                }
                result.unknowns.clear()
                saveToPrefs()
                billItems = getBillItems(result)
                switchTab("bill")
                showSnack("${newlyAdded.size} items menu mein save ho gaye!")
            }
            .setNegativeButton("Sirf Is Baar") { _, _ ->
                for ((origName, fields) in tempFieldsMap) {
                    val finalName = fields.first.text.toString().trim()
                    val price = fields.second.text.toString().toIntOrNull() ?: 0
                    if (finalName.isNotEmpty() && price > 0) {
                        savedItems[finalName] = price // temp only - will not persist
                        val qty = result.unknowns.find { it.name == origName }?.qty ?: 0
                        result.items[finalName] = (result.items[finalName] ?: 0) + qty
                        result.items.remove(origName)
                    }
                }
                result.unknowns.clear()
                billItems = getBillItems(result)
                switchTab("bill")
            }
            .show()
    }

    // ─── BILL TAB ──────────────────────────────────────────────────────────────

    private fun setupBillTab() {
        findViewById<MaterialButton>(R.id.btn_save_history).setOnClickListener {
            saveBillToHistory()
        }
        findViewById<MaterialButton>(R.id.btn_print).setOnClickListener {
            printBill()
        }
        findViewById<MaterialButton>(R.id.btn_share_bill).setOnClickListener {
            shareBill()
        }
    }

    private fun renderBill() {
        if (billItems.isEmpty()) {
            findViewById<View>(R.id.bill_empty_state).visibility = View.VISIBLE
            findViewById<View>(R.id.bill_content).visibility = View.GONE
            return
        }
        findViewById<View>(R.id.bill_empty_state).visibility = View.GONE
        findViewById<View>(R.id.bill_content).visibility = View.VISIBLE

        val grandTotal = billItems.sumOf { it.total }
        val totalQty = billItems.sumOf { it.qty }

        findViewById<TextView>(R.id.tv_grand_total).text = "₹${grandTotal.fmt()}"
        findViewById<TextView>(R.id.tv_total_qty).text = "$totalQty"
        findViewById<TextView>(R.id.tv_unique_items).text = "${billItems.size}"
        val avgUnit = if (totalQty > 0) grandTotal / totalQty else 0
        findViewById<TextView>(R.id.tv_avg_unit).text = "₹$avgUnit"

        val rv = findViewById<RecyclerView>(R.id.rv_bill_items)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = BillAdapter(billItems)
    }

    private fun saveBillToHistory() {
        if (billItems.isEmpty()) { showSnack("Bill mein koi item nahi!"); return }
        val now = Date()
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        val itemMap = billItems.associate { it.name to it.qty }
        val detailMap = billItems.associate { it.name to BillItemDetail(it.qty, it.price, it.total, it.category) }
        val order = OrderHistory(
            id = now.time,
            dateStr = dateStr,
            timeStr = timeStr,
            text = currentResult?.items?.entries?.joinToString("\n") { "${it.value} x ${it.key}" } ?: "",
            items = itemMap,
            itemDetails = detailMap,
            total = billItems.sumOf { it.total },
            totalQty = billItems.sumOf { it.qty }
        )
        orderHistory.add(0, order)
        if (orderHistory.size > 500) orderHistory.removeLastOrNull()
        saveToPrefs()
        showSnack("Order history mein save ho gaya!")
        updateHistoryBadge()
    }

    private fun printBill() {
        val grandTotal = billItems.sumOf { it.total }
        val totalQty = billItems.sumOf { it.qty }
        val now = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val rows = billItems.joinToString("") {
            "<tr><td>${it.name}</td><td align='right'>${it.qty}</td><td align='right'>₹${it.price}</td><td align='right'>₹${it.total}</td></tr>"
        }
        val html = """
            <html><head><title>Shawarma Bill</title>
            <style>body{font-family:sans-serif;padding:24px;max-width:420px;margin:0 auto}
            h2{text-align:center;font-size:22px;margin-bottom:4px}
            .sub{text-align:center;color:#666;font-size:12px;margin-bottom:16px}
            table{width:100%;border-collapse:collapse}
            th,td{padding:7px 9px;font-size:13px}
            th{border-bottom:2px solid #000;text-align:left}
            td{border-bottom:1px solid #eee}
            .total{font-weight:bold;font-size:15px;background:#fef3c7;border-top:2px solid #000}
            </style></head><body>
            <h2>🥙 Shawarma Billing</h2>
            <div class='sub'>$now</div>
            <table><thead><tr><th>Item</th><th align='right'>Qty</th><th align='right'>Rate</th><th align='right'>Total</th></tr></thead>
            <tbody>$rows</tbody>
            <tfoot><tr class='total'><td colspan='2'>Grand Total</td><td align='right'>$totalQty</td><td align='right'>₹${grandTotal.fmt()}</td></tr></tfoot>
            </table></body></html>
        """.trimIndent()

        val webView = WebView(this)
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        webView.settings.javaScriptEnabled = false

        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Shawarma Bill - $now"
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    private fun shareBill() {
        val grandTotal = billItems.sumOf { it.total }
        val totalQty = billItems.sumOf { it.qty }
        val sb = StringBuilder()
        sb.appendLine("🥙 *Shawarma Billing*")
        sb.appendLine(SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()))
        sb.appendLine("─────────────────────")
        for (item in billItems) {
            sb.appendLine("${item.name}")
            sb.appendLine("  ${item.qty} x ₹${item.price} = ₹${item.total}")
        }
        sb.appendLine("─────────────────────")
        sb.appendLine("Total Qty: $totalQty")
        sb.appendLine("*Grand Total: ₹${grandTotal.fmt()}*")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Bill Share Karein"))
    }

    // ─── HISTORY TAB ───────────────────────────────────────────────────────────

    private fun setupHistoryTab() {
        val searchView = findViewById<EditText>(R.id.history_search)
        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderHistory(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<MaterialButton>(R.id.btn_clear_history).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("History Clear Karein?")
                .setMessage("Sari ${orderHistory.size} orders delete ho jayengi. Yeh undo nahi hoga.")
                .setPositiveButton("Haan, Clear Karo") { _, _ ->
                    orderHistory.clear()
                    saveToPrefs()
                    renderHistory()
                    showSnack("History clear ho gayi!")
                    updateHistoryBadge()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun renderHistory(search: String = "") {
        val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val todayOrders = orderHistory.filter { it.dateStr == todayStr }
        val todayTotal = todayOrders.sumOf { it.total }
        val allTotal = orderHistory.sumOf { it.total }

        findViewById<TextView>(R.id.tv_today_orders).text = "${todayOrders.size}"
        findViewById<TextView>(R.id.tv_today_revenue).text = "₹${todayTotal.fmt()}"
        findViewById<TextView>(R.id.tv_all_orders).text = "${orderHistory.size}"
        findViewById<TextView>(R.id.tv_all_revenue).text = "₹${allTotal.fmt()}"

        val filtered = if (search.isBlank()) orderHistory
        else orderHistory.filter { e ->
            e.dateStr.contains(search, true) ||
            e.items.keys.any { it.contains(search, true) } ||
            e.total.toString().contains(search)
        }

        val rv = findViewById<RecyclerView>(R.id.rv_history)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = HistoryAdapter(filtered) { entry ->
            showHistoryDetailDialog(entry)
        }

        val emptyState = findViewById<View>(R.id.history_empty)
        emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showHistoryDetailDialog(entry: OrderHistory) {
        val message = buildString {
            appendLine("📅 ${entry.dateStr} · ${entry.timeStr}")
            appendLine()
            appendLine("Items:")
            for ((name, detail) in entry.itemDetails) {
                appendLine("  $name — ${detail.qty} x ₹${detail.price} = ₹${detail.total}")
            }
            appendLine()
            appendLine("Total Qty: ${entry.totalQty}")
            appendLine("Grand Total: ₹${entry.total.fmt()}")
        }

        AlertDialog.Builder(this)
            .setTitle("Order Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .setNeutralButton("Delete") { _, _ ->
                orderHistory.removeAll { it.id == entry.id }
                saveToPrefs()
                renderHistory()
                showSnack("Order delete ho gaya!")
                updateHistoryBadge()
            }
            .show()
    }

    private fun updateHistoryBadge() {
        val badge = bottomNav.getOrCreateBadge(R.id.nav_history)
        if (orderHistory.isNotEmpty()) {
            badge.isVisible = true
            badge.number = orderHistory.size
        } else {
            badge.isVisible = false
        }
    }

    // ─── MENU TAB ──────────────────────────────────────────────────────────────

    private fun setupMenuTab() {
        val searchView = findViewById<EditText>(R.id.menu_search)
        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderMenu(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val addNameField = findViewById<EditText>(R.id.manual_item_name)
        val addPriceField = findViewById<EditText>(R.id.manual_item_price)
        val addBtn = findViewById<MaterialButton>(R.id.btn_add_manual)

        addBtn.setOnClickListener {
            val name = addNameField.text.toString().trim()
            val price = addPriceField.text.toString().toIntOrNull() ?: 0
            if (name.isEmpty() || price <= 0) {
                showSnack("Item name aur valid price daalein!")
                return@setOnClickListener
            }
            savedItems[name] = price
            saveToPrefs()
            addNameField.setText("")
            addPriceField.setText("")
            renderMenu()
            showSnack("\"$name\" menu mein add ho gaya at ₹$price!")
        }

        findViewById<MaterialButton>(R.id.btn_clear_custom).setOnClickListener {
            if (savedItems.isEmpty()) { showSnack("Koi custom item nahi hai!"); return@setOnClickListener }
            AlertDialog.Builder(this)
                .setTitle("Custom Items Clear Karein?")
                .setMessage("${savedItems.size} custom items delete ho jayenge.")
                .setPositiveButton("Haan") { _, _ ->
                    savedItems.clear()
                    saveToPrefs()
                    renderMenu()
                    showSnack("Custom items clear ho gaye!")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun renderMenu(search: String = "") {
        val customCount = savedItems.size
        val customLabel = findViewById<TextView>(R.id.tv_custom_count)
        customLabel.text = if (customCount > 0) "$customCount custom items saved" else "Koi custom item nahi"

        val rvCustom = findViewById<RecyclerView>(R.id.rv_custom_items)
        rvCustom.layoutManager = LinearLayoutManager(this)
        rvCustom.adapter = CustomMenuAdapter(savedItems.entries.toList()) { name ->
            AlertDialog.Builder(this)
                .setTitle("Remove \"$name\"?")
                .setPositiveButton("Remove") { _, _ ->
                    savedItems.remove(name)
                    saveToPrefs()
                    renderMenu()
                    showSnack("\"$name\" removed!")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val filtered = if (search.isBlank()) BASE_PRICES.entries.toList()
        else BASE_PRICES.entries.filter { it.key.contains(search, true) }

        val rvMenu = findViewById<RecyclerView>(R.id.rv_menu_items)
        rvMenu.layoutManager = LinearLayoutManager(this)
        rvMenu.adapter = MenuAdapter(filtered)

        val menuCount = findViewById<TextView>(R.id.tv_menu_count)
        menuCount.text = "${filtered.size} items"
    }

    // ─── PARSE & MATCH ─────────────────────────────────────────────────────────

    private fun normStr(s: String) = s.lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ").trim()

    private fun matchItem(raw: String): Pair<String, Int>? {
        val prices = PRICES
        val n = normStr(raw)
        if (n.length < 2) return null
        val exactMatch = prices.entries.find { normStr(it.key) == n }
        if (exactMatch != null) return Pair(exactMatch.key, exactMatch.value)
        val words = n.split(" ").filter { it.length > 2 }
        if (words.isEmpty()) return null
        var bestKey = ""
        var bestScore = 0.0
        var bestPrice = 0
        for ((k, v) in prices) {
            val iw = normStr(k).split(" ")
            var matched = 0
            for (w in words) {
                if (iw.any { it == w || it.startsWith(w.dropLast(1)) || w.startsWith(it.dropLast(1)) }) matched++
            }
            val score = matched.toDouble() / maxOf(words.size, iw.size)
            if (score > bestScore) { bestScore = score; bestKey = k; bestPrice = v }
        }
        return if (bestScore >= 0.5) Pair(bestKey, bestPrice) else null
    }

    private fun parseOrders(text: String): ParseResult {
        val items = mutableMapOf<String, Int>()
        val unknowns = mutableListOf<UnknownItem>()
        val seen = mutableSetOf<String>()
        val skipRe = Regex("^(note|offer|comment|instruction|#|//|total|please|add|remove)", RegexOption.IGNORE_CASE)
        val chunks = text.split(Regex("[,\n]+"))

        for (chunk in chunks) {
            val t = chunk.trim()
            if (t.isEmpty() || skipRe.containsMatchIn(t)) continue
            val re = Regex("(\\d+)\\s*[xX×]\\s*([\\w\\s()/]+?)(?=\\s*\\d+\\s*[xX×]|\$)")
            for (m in re.findAll(t)) {
                val qty = m.groupValues[1].toIntOrNull() ?: continue
                val name = m.groupValues[2].trim()
                if (name.isEmpty() || qty <= 0 || qty > 500) continue
                val found = matchItem(name)
                if (found != null) {
                    items[found.first] = (items[found.first] ?: 0) + qty
                } else {
                    val k = name.lowercase()
                    if (!seen.contains(k)) { seen.add(k); unknowns.add(UnknownItem(qty, name)) }
                }
            }
        }
        return ParseResult(items, unknowns)
    }

    private fun getBillItems(result: ParseResult): List<BillItem> {
        val prices = PRICES
        return result.items.entries.map { (name, qty) ->
            val price = prices[name] ?: 0
            BillItem(name, qty, price, qty * price, getCategory(name), savedItems.containsKey(name))
        }.sortedByDescending { it.total }
    }

    private fun getCategory(name: String): String {
        val n = name.lowercase()
        return when {
            n.contains("rumali") -> "Rumali"
            n.startsWith("chicken") -> "Chicken"
            n.startsWith("veg ") -> "Veg"
            n.contains("paneer") -> "Paneer"
            n.contains("open") -> "Open Special"
            n in listOf("extra cheese", "cheese dip", "bread peta") -> "Extras"
            savedItems.containsKey(name) -> "Custom"
            else -> "Classic"
        }
    }

    // ─── STORAGE ───────────────────────────────────────────────────────────────

    private fun loadFromPrefs() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        try {
            val customJson = prefs.getString(KEY_CUSTOM, null)
            if (customJson != null) {
                val obj = JSONObject(customJson)
                for (key in obj.keys()) savedItems[key] = obj.getInt(key)
            }
        } catch (e: Exception) {}

        try {
            val histJson = prefs.getString(KEY_HISTORY, null)
            if (histJson != null) {
                val arr = JSONArray(histJson)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val items = mutableMapOf<String, Int>()
                    val itemsObj = o.getJSONObject("items")
                    for (k in itemsObj.keys()) items[k] = itemsObj.getInt(k)
                    val details = mutableMapOf<String, BillItemDetail>()
                    if (o.has("itemDetails")) {
                        val dObj = o.getJSONObject("itemDetails")
                        for (k in dObj.keys()) {
                            val d = dObj.getJSONObject(k)
                            details[k] = BillItemDetail(d.getInt("qty"), d.getInt("price"), d.getInt("total"), d.optString("category", "Classic"))
                        }
                    }
                    orderHistory.add(OrderHistory(
                        id = o.getLong("id"),
                        dateStr = o.getString("dateStr"),
                        timeStr = o.getString("timeStr"),
                        text = o.optString("text", ""),
                        items = items,
                        itemDetails = details,
                        total = o.getInt("total"),
                        totalQty = o.getInt("totalQty")
                    ))
                }
            }
        } catch (e: Exception) {}
    }

    private fun saveToPrefs() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val customJson = JSONObject()
        for ((k, v) in savedItems) customJson.put(k, v)
        editor.putString(KEY_CUSTOM, customJson.toString())

        val histArr = JSONArray()
        for (h in orderHistory) {
            val o = JSONObject()
            o.put("id", h.id)
            o.put("dateStr", h.dateStr)
            o.put("timeStr", h.timeStr)
            o.put("text", h.text)
            o.put("total", h.total)
            o.put("totalQty", h.totalQty)
            val items = JSONObject()
            for ((k, v) in h.items) items.put(k, v)
            o.put("items", items)
            val details = JSONObject()
            for ((k, d) in h.itemDetails) {
                val dObj = JSONObject()
                dObj.put("qty", d.qty); dObj.put("price", d.price); dObj.put("total", d.total); dObj.put("category", d.category)
                details.put(k, dObj)
            }
            o.put("itemDetails", details)
            histArr.put(o)
        }
        editor.putString(KEY_HISTORY, histArr.toString())
        editor.apply()
    }

    private fun showSnack(msg: String) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()
    }

    private fun Int.fmt() = String.format("%,d", this)
}

// ─── ADAPTERS ──────────────────────────────────────────────────────────────────

class BillAdapter(private val items: List<BillItem>) : RecyclerView.Adapter<BillAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.item_name)
        val category: TextView = v.findViewById(R.id.item_category)
        val qty: TextView = v.findViewById(R.id.item_qty)
        val price: TextView = v.findViewById(R.id.item_price)
        val total: TextView = v.findViewById(R.id.item_total)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_bill_row, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.name.text = item.name + if (item.isCustom) " ★" else ""
        h.category.text = item.category
        h.qty.text = "${item.qty}"
        h.price.text = "₹${item.price}"
        h.total.text = "₹${item.total}"
    }
}

class HistoryAdapter(private val items: List<OrderHistory>, private val onClick: (OrderHistory) -> Unit) : RecyclerView.Adapter<HistoryAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val date: TextView = v.findViewById(R.id.hist_date)
        val time: TextView = v.findViewById(R.id.hist_time)
        val summary: TextView = v.findViewById(R.id.hist_summary)
        val total: TextView = v.findViewById(R.id.hist_total)
        val card: MaterialCardView = v.findViewById(R.id.hist_card)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = items[pos]
        h.date.text = e.dateStr
        h.time.text = e.timeStr
        val preview = e.items.keys.take(2).joinToString(", ") + if (e.items.size > 2) " +${e.items.size - 2} more" else ""
        h.summary.text = "${e.items.size} items · ${e.totalQty} pcs · $preview"
        h.total.text = "₹${String.format("%,d", e.total)}"
        h.card.setOnClickListener { onClick(e) }
    }
}

class MenuAdapter(private val items: List<Map.Entry<String, Int>>) : RecyclerView.Adapter<MenuAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.menu_item_name)
        val price: TextView = v.findViewById(R.id.menu_item_price)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_menu_row, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        h.name.text = items[pos].key
        h.price.text = "₹${items[pos].value}"
    }
}

class CustomMenuAdapter(private val items: List<Map.Entry<String, Int>>, private val onDelete: (String) -> Unit) : RecyclerView.Adapter<CustomMenuAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.custom_name)
        val price: TextView = v.findViewById(R.id.custom_price)
        val deleteBtn: MaterialButton = v.findViewById(R.id.btn_delete_custom)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_custom_menu, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        h.name.text = items[pos].key
        h.price.text = "₹${items[pos].value}"
        h.deleteBtn.setOnClickListener { onDelete(items[pos].key) }
    }
}
