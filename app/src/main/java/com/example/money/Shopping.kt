
package com.example.shoppinglist

import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import org.json.JSONArray
import org.json.JSONObject
import android.view.Gravity
import android.graphics.Paint
import android.view.ViewGroup
import android.content.res.ColorStateList

class Shopping : AppCompatActivity() {

    private var searchQuery = ""
    private lateinit var editButton: TextView
    private lateinit var scroll: LinearLayout
    private val collapsedSections = mutableSetOf<String>()
    private var isEditMode = false
    private val editSelections = mutableSetOf<String>()
    private val white = Color.WHITE
    private val gray = Color.rgb(102, 102, 102)
    private val muted = Color.rgb(153, 153, 153)
    private val dark = Color.rgb(29, 27, 32)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContentView(R.layout.shopping)

        window.setBackgroundDrawableResource(android.R.color.black)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                bars.left + 20,
                bars.top + 50,
                bars.right + 20,
                bars.bottom + 50
            )

            insets
        }

        scroll = findViewById(R.id.scroll)


        editButton = findViewById(R.id.s_edit)

        val searchBar = findViewById<EditText>(R.id.a_search)

        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                searchQuery = s.toString().trim().lowercase()

                if (searchQuery.isEmpty()) {
                    loadShoppingData()
                } else {
                    applySearchFilter()
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        editButton.text = "Edit"

        editButton.setOnClickListener {
            if (!isEditMode) {
                // Enter Edit mode with an empty checklist
                isEditMode = true
                editSelections.clear()
                editButton.text = "Save"
            } else {
                // Delete checked entries before leaving Edit mode
                if (editSelections.isNotEmpty()) {
                    ShoppingListStore.deleteSelectedShoppingData(
                        this,
                        editSelections
                    )

                    android.util.Log.d(
                        "SELECTION_DEBUG",
                        "Deleting keys: ${editSelections.map { ShoppingListStore.parts(it) }}"
                    )
                }

                android.util.Log.d(
                    "SELECTION_DEBUG",
                    "After deletion: ${ShoppingListStore.getSelections(this).map {
                        ShoppingListStore.parts(it)
                    }}"
                )

                isEditMode = false
                editSelections.clear()
                editButton.text = "Edit"
            }

            loadShoppingData()
        }


        findViewById<TextView>(R.id.a_complete).setOnClickListener {
            startActivity(Intent(this, Complete::class.java))
        }

        findViewById<TextView>(R.id.a_gucci_mane).setOnClickListener {
            startActivity(Intent(this, Gucci_Mane::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadShoppingData()
    }

    private fun loadShoppingData() {
        val data = ShoppingListStore.getShoppingData(this)

        scroll.removeAllViews()

        for (i in 0 until data.length()) {
            val place = data.optJSONObject(i) ?: continue
            addPlace(place)
        }

        updateEditButtonAppearance()
    }


    private fun addPlace(place: JSONObject) {
        val placeName = place.optString("place")
        val placeKey = ShoppingListStore.path("P", placeName)
        val selections = currentSelections()
        val placeSelected = selections.contains(placeKey)

        val placeSection = verticalLayout()
        val placeBar = horizontalLayout()
        placeBar.setPadding(dp(7), 0, 0, 0)

        val expandButton = expandButton()

        val name = textView(placeName, 20f, white, true)
        name.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)

        val checkbox = checkBox()
        checkbox.isChecked = placeSelected

        placeBar.addView(expandButton)
        placeBar.addView(name)
        placeBar.addView(checkbox)
        placeSection.addView(placeBar)

        val categoryContainer = verticalLayout()
        val categories = place.optJSONArray("categories") ?: JSONArray()

        for (i in 0 until categories.length()) {
            val category = categories.optJSONObject(i) ?: continue
            addCategory(categoryContainer, category, placeName, placeSelected)
        }

        placeSection.addView(categoryContainer)
        setupExpansion(
            expandButton,
            categoryContainer,
            ShoppingListStore.path("P", placeName)
        )
        if (placeSelected) {
            name.paintFlags = name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            name.setTextColor(gray)
            updateDescendantSelection(categoryContainer, true)
        }

        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.tag == "hierarchy_update") {
                return@setOnCheckedChangeListener
            }
            updateHierarchySelection(
                "P",
                place,
                null,
                null,
                isChecked
            )
            loadShoppingData()
        }

        scroll.addView(placeSection)
    }


    private fun addCategory(
        parent: LinearLayout,
        category: JSONObject,
        placeName: String,
        parentSelected: Boolean
    ) {
        val categoryName = category.optString("category")
        val categoryKey = ShoppingListStore.path("C", placeName, categoryName)
        val selections = currentSelections()
        val categorySelected = selections.contains(categoryKey)

        val categorySection = verticalLayout()
        val categoryBar = horizontalLayout()
        categoryBar.setPadding(dp(37), 0, 0, 0)

        val expandButton = expandButton()

        val name = textView(categoryName, 20f, white, false)
        name.setTypeface(null, android.graphics.Typeface.ITALIC)
        name.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)

        val checkbox = checkBox()
        checkbox.isChecked = categorySelected
        checkbox.visibility = if (parentSelected) View.GONE else View.VISIBLE

        categoryBar.addView(expandButton)
        categoryBar.addView(name)
        categoryBar.addView(checkbox)
        categorySection.addView(categoryBar)

        val itemContainer = verticalLayout()
        val items = category.optJSONArray("items") ?: JSONArray()

        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            addItem(
                itemContainer,
                item,
                placeName,
                categoryName,
                parentSelected || categorySelected
            )
        }

        categorySection.addView(itemContainer)
        setupExpansion(
            expandButton,
            itemContainer,
            ShoppingListStore.path("C", placeName, categoryName)
        )

        if (categorySelected) {
            name.paintFlags = name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            name.setTextColor(gray)
            updateDescendantSelection(itemContainer, true)
        }

        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.tag == "hierarchy_update") {
                return@setOnCheckedChangeListener
            }

            updateHierarchySelection("C", null, placeName, category, isChecked)
            loadShoppingData()
        }

        parent.addView(categorySection)
    }




    private fun addItem(
        parent: LinearLayout,
        item: JSONObject,
        placeName: String,
        categoryName: String,
        parentSelected: Boolean
    ) {
        val itemName = item.optString("item")
        val itemKey = ShoppingListStore.path(
            "I",
            placeName,
            categoryName,
            itemName
        )

        // Always use the active checklist
        val selections = currentSelections()
        val itemSelected = selections.contains(itemKey)

        val itemSection = verticalLayout()
        val itemBar = horizontalLayout()
        itemBar.setPadding(dp(67), 0, 0, 0)

        val expandButton = expandButton()

        val name = textView(itemName, 20f, white, false)
        name.setTypeface(null, android.graphics.Typeface.ITALIC)
        name.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)

        val checkbox = checkBox()
        checkbox.isChecked = itemSelected
        checkbox.visibility =
            if (parentSelected) View.GONE else View.VISIBLE

        itemBar.addView(expandButton)
        itemBar.addView(name)
        itemBar.addView(checkbox)
        itemSection.addView(itemBar)

        val brandContainer = verticalLayout().apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            clipChildren = false
            clipToPadding = false
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val brands = item.optJSONArray("brands") ?: JSONArray()

        if (brands.length() > 0) {
            addBrandRows(brandContainer, brands)
        }

        itemSection.addView(brandContainer)

        if (itemSelected) {
            name.paintFlags =
                name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            name.setTextColor(gray)
            updateDescendantSelection(brandContainer, true)
        }

        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.tag == "hierarchy_update") {
                return@setOnCheckedChangeListener
            }

            updateHierarchySelection(
                "I",
                null,
                placeName,
                item,
                isChecked,
                categoryName
            )

            loadShoppingData()
        }

        if (brands.length() == 0) {
            expandButton.text = "×"
            expandButton.textSize = 30f
            expandButton.gravity = Gravity.CENTER
            expandButton.includeFontPadding = false
            expandButton.setPadding(0, 0, 0, 0)

            expandButton.minWidth = 0
            expandButton.minimumWidth = 0
            expandButton.minHeight = 0
            expandButton.minimumHeight = 0

            expandButton.isEnabled = false
            expandButton.alpha = 0.5f
        } else {
            setupExpansion(
                expandButton,
                brandContainer,
                ShoppingListStore.path(
                    "I",
                    placeName,
                    categoryName,
                    itemName
                )
            )
        }

        parent.addView(itemSection)
    }



    private fun addBrandRows(
        parent: LinearLayout,
        brands: JSONArray
    ) {
        val table = TableLayout(this)

        table.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        table.setPadding(0, 0, 0, 0)

        table.setColumnStretchable(0, true)
        table.setColumnStretchable(2, true)
        table.setColumnStretchable(4, true)
        table.setColumnStretchable(6, true)

        val placeholders = listOf(
            "[Brand]",
            "[Name]",
            "[Weight]",
            "[Quantity]"
        )

        for (i in 0 until brands.length()) {
            val brand = brands.optJSONObject(i) ?: continue

            val values = listOf(
                brand.optString("brand"),
                brand.optString("name"),
                brand.optString("weight"),
                brand.optString("quantity")
            )

            val row = TableRow(this)
            row.gravity = Gravity.CENTER_VERTICAL

            for (column in values.indices) {
                val value = values[column].trim()

                val cell = TextView(this)

                cell.text = if (value.isEmpty()) {
                    placeholders[column]
                } else {
                    value
                }

                cell.layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )

                cell.gravity = Gravity.CENTER
                cell.setPadding(dp(5), dp(5), dp(5), dp(5))
                cell.textSize = 15f

                cell.setTextColor(
                    if (value.isEmpty()) {
                        Color.rgb(102, 102, 102)
                    } else {
                        Color.WHITE
                    }
                )

                row.addView(cell)

                if (column < 3) {
                    val separator = TextView(this)

                    separator.text = "|"
                    separator.layoutParams = TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                    )

                    separator.gravity = Gravity.CENTER
                    separator.setTextColor(Color.rgb(102, 102, 102))
                    separator.textSize = 20f
                    separator.setTypeface(
                        null,
                        android.graphics.Typeface.BOLD
                    )

                    row.addView(separator)
                }
            }

            table.addView(row)
        }

        parent.addView(
            table,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }


    private fun brandCell(
        value: String,
        color: Int = muted
    ): TextView {
        val text = TextView(this)

        text.text = value
        text.layoutParams = TableRow.LayoutParams(
            0,
            -2,
            1f
        )
        text.gravity = Gravity.CENTER
        text.setPadding(dp(3), dp(5), dp(3), dp(5))
        text.setTextColor(color)
        text.textSize = 13f

        return text
    }


    private fun separatorCell(): TextView {
        val text = TextView(this)

        text.text = "|"
        text.layoutParams = TableRow.LayoutParams(
            -2,
            -2
        )
        text.gravity = Gravity.CENTER
        text.setTextColor(muted)
        text.textSize = 20f
        text.setTypeface(null, android.graphics.Typeface.BOLD)

        return text
    }


    private fun updateHierarchySelection(
        type: String,
        place: JSONObject? = null,
        placeName: String? = null,
        target: JSONObject? = null,
        selected: Boolean,
        categoryName: String = ""
    ) {
        val selections = currentSelections()
        when (type) {
            "P" -> {
                val keys = allKeysInPlace(place!!)
                if (selected) selections.addAll(keys)
                else selections.removeAll(keys.toSet())
            }

            "C" -> {
                val keys = allKeysInCategory(
                    placeName!!,
                    target!!
                )
                if (selected) selections.addAll(keys)
                else selections.removeAll(keys.toSet())
            }

            "I" -> {
                val key = ShoppingListStore.path(
                    "I",
                    placeName!!,
                    categoryName,
                    target!!.optString("item")
                )

                if (selected) selections.add(key)
                else selections.remove(key)
            }
        }

        // Find the affected Place in the saved shopping data
        val data = ShoppingListStore.getShoppingData(this)
        var affectedPlace: JSONObject? = place

        if (affectedPlace == null) {
            for (i in 0 until data.length()) {
                val candidate = data.optJSONObject(i) ?: continue

                if (candidate.optString("place") == placeName) {
                    affectedPlace = candidate
                    break
                }
            }
        }

        // Recalculate every Category under this Place
        if (affectedPlace != null) {
            val currentPlaceName = affectedPlace.optString("place")
            val categories = affectedPlace.optJSONArray("categories") ?: JSONArray()

            for (i in 0 until categories.length()) {
                val category = categories.optJSONObject(i) ?: continue
                val currentCategoryName = category.optString("category")
                val items = category.optJSONArray("items") ?: JSONArray()

                val itemKeys = mutableListOf<String>()

                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j) ?: continue

                    itemKeys.add(
                        ShoppingListStore.path(
                            "I",
                            currentPlaceName,
                            currentCategoryName,
                            item.optString("item")
                        )
                    )
                }

                val categoryKey = ShoppingListStore.path(
                    "C",
                    currentPlaceName,
                    currentCategoryName
                )

                if (itemKeys.isNotEmpty()) {
                    if (itemKeys.all { selections.contains(it) }) {
                        selections.add(categoryKey)
                    } else {
                        selections.remove(categoryKey)
                    }
                }
            }

            // Recalculate the Place
            val categoryKeys = mutableListOf<String>()

            for (i in 0 until categories.length()) {
                val category = categories.optJSONObject(i) ?: continue

                categoryKeys.add(
                    ShoppingListStore.path(
                        "C",
                        currentPlaceName,
                        category.optString("category")
                    )
                )
            }

            val placeKey = ShoppingListStore.path("P", currentPlaceName)

            if (categoryKeys.isNotEmpty()) {
                if (categoryKeys.all { selections.contains(it) }) {
                    selections.add(placeKey)
                } else {
                    selections.remove(placeKey)
                }
            }
        }

        // Save only in normal mode
        if (!isEditMode) {
            ShoppingListStore.saveSelections(this, selections)
        }}

    private fun allKeysInCategory(
        placeName: String,
        category: JSONObject
    ): List<String> {
        val keys = mutableListOf<String>()
        val categoryName = category.optString("category")

        keys.add(
            ShoppingListStore.path("C", placeName, categoryName)
        )

        val items = category.optJSONArray("items") ?: JSONArray()

        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue

            keys.add(
                ShoppingListStore.path(
                    "I",
                    placeName,
                    categoryName,
                    item.optString("item")
                )
            )
        }

        return keys
    }

    private fun allKeysInPlace(place: JSONObject): List<String> {
        val keys = mutableListOf<String>()
        val placeName = place.optString("place")

        keys.add(ShoppingListStore.path("P", placeName))

        val categories = place.optJSONArray("categories") ?: JSONArray()

        for (i in 0 until categories.length()) {
            val category = categories.optJSONObject(i) ?: continue
            keys.addAll(allKeysInCategory(placeName, category))
        }

        return keys
    }

    private fun updateDescendantSelection(
        container: ViewGroup,
        checked: Boolean
    ) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)

            when (child) {
                is AppCompatCheckBox -> {
                    child.tag = "hierarchy_update"
                    child.isChecked = checked
                    child.visibility = if (checked) View.GONE else View.VISIBLE
                    child.tag = null
                }

                is TextView -> {
                    if (child !is AppCompatButton) {
                        child.paintFlags = if (checked) {
                            child.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        } else {
                            child.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        }

                        child.setTextColor(
                            if (checked) Color.rgb(102, 102, 102) else white
                        )
                    }
                }
            }

            if (child is ViewGroup) {
                updateDescendantSelection(child, checked)
            }
        }
    }

    private fun applySearchFilter() {
        for (i in 0 until scroll.childCount) {
            val section = scroll.getChildAt(i) as? ViewGroup ?: continue
            filterSection(section)
        }
    }

    private fun filterSection(section: ViewGroup): Boolean {
        val bar = section.getChildAt(0) as? ViewGroup ?: return false
        val content = section.getChildAt(1) as? ViewGroup ?: return false

        var titleMatches = false

        for (i in 0 until bar.childCount) {
            val child = bar.getChildAt(i)

            if (child is TextView &&
                child !is AppCompatButton &&
                child !is AppCompatCheckBox
            ) {
                if (child.text.toString().lowercase().contains(searchQuery)) {
                    titleMatches = true
                }
            }
        }

        if (titleMatches) {
            section.visibility = View.VISIBLE
            content.visibility = View.VISIBLE
            revealSections(content)
            return true
        }

        var childMatches = false

        for (i in 0 until content.childCount) {
            val child = content.getChildAt(i)

            if (child is LinearLayout &&
                child.childCount >= 2 &&
                child.getChildAt(0) is LinearLayout &&
                child.getChildAt(1) is LinearLayout
            ) {
                if (filterSection(child)) {
                    childMatches = true
                }
            } else if (containsSearchText(child)) {
                child.visibility = View.VISIBLE
                childMatches = true
            } else {
                child.visibility = View.GONE
            }
        }

        section.visibility = if (childMatches) View.VISIBLE else View.GONE
        content.visibility = if (childMatches) View.VISIBLE else View.GONE

        return childMatches
    }

    private fun containsSearchText(view: View): Boolean {
        if (view is TextView &&
            view !is AppCompatButton &&
            view !is AppCompatCheckBox
        ) {
            if (view.text.toString().lowercase().contains(searchQuery)) {
                return true
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (containsSearchText(view.getChildAt(i))) {
                    return true
                }
            }
        }

        return false
    }

    private fun revealSections(container: ViewGroup) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)

            if (child is LinearLayout &&
                child.childCount >= 2 &&
                child.getChildAt(0) is LinearLayout &&
                child.getChildAt(1) is LinearLayout
            ) {
                child.visibility = View.VISIBLE

                val content = child.getChildAt(1)
                content.visibility = View.VISIBLE

                if (content is ViewGroup) {
                    revealSections(content)
                }
            }
        }
    }

    private fun setupExpansion(
        button: AppCompatButton,
        content: View,
        key: String
    ) {
        val expanded = key !in collapsedSections

        content.visibility = if (expanded) {
            View.VISIBLE
        } else {
            View.GONE
        }

        button.text = if (expanded) "v" else ">"

        button.textSize = 20f
        button.gravity = Gravity.CENTER
        button.includeFontPadding = false

        button.setOnClickListener {
            val currentlyExpanded = content.visibility == View.VISIBLE

            if (currentlyExpanded) {
                collapsedSections.add(key)
                content.visibility = View.GONE
                button.text = ">"
            } else {
                collapsedSections.remove(key)
                content.visibility = View.VISIBLE
                button.text = "v"
            }
        }
    }

    private fun expandButton(): AppCompatButton {
        val button = AppCompatButton(this)

        button.layoutParams = LinearLayout.LayoutParams(
            dp(35),
            dp(40)
        )

        button.text = "−"
        button.textSize = 20f
        button.setTextColor(dark)
        button.backgroundTintList =
            android.content.res.ColorStateList.valueOf(gray)

        button.setPadding(0, 0, 0, 0)
        button.minWidth = 0
        button.minimumWidth = 0
        button.minHeight = 0
        button.minimumHeight = 0

        return button
    }

    private fun checkBox(): AppCompatCheckBox {
        val checkbox = AppCompatCheckBox(this)

        checkbox.layoutParams = LinearLayout.LayoutParams(
            dp(30),
            dp(30)
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        checkbox.buttonTintList =
            android.content.res.ColorStateList.valueOf(gray)

        checkbox.setPadding(0, 0, 0, 0)

        return checkbox
    }

    private fun textView(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        val text = TextView(this)

        text.text = value
        text.setTextColor(color)
        text.textSize = size
        text.setPadding(dp(5), dp(5), dp(5), dp(5))

        if (bold) {
            text.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        return text
    }

    private fun horizontalLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                -1,
                -2
            )
        }
    }

    private fun verticalLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                -1,
                -2
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun currentSelections(): MutableSet<String> {
        return if (isEditMode) {
            editSelections
        } else {
            ShoppingListStore.getSelections(this)
        }
    }


    private fun updateEditButtonAppearance() {
        val canSave = isEditMode && editSelections.isNotEmpty()

        editButton.text = if (isEditMode) "Save" else "Edit"
        editButton.isEnabled = true

        editButton.backgroundTintList = ColorStateList.valueOf(
            if (canSave) Color.rgb(255, 128, 0) else gray
        )

        editButton.setTextColor(Color.WHITE)
    }

}
