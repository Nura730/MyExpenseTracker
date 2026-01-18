package com.example.myexpensetracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var adapter: ArrayAdapter<String>
    private var categoryList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnAddCategory = findViewById<Button>(R.id.btnAddCategory)

        spinner = findViewById(R.id.spCategory)

        loadCategories()

        btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        // LONG PRESS TO DELETE CATEGORY
        spinner.setOnLongClickListener {
            deleteCategoryDialog()
            true
        }

        btnSave.setOnClickListener {

            val amountStr = etAmount.text.toString().trim()
            val note = etNote.text.toString().trim()

            if(amountStr.isEmpty()){
                Toast.makeText(this,"Enter amount",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toFloatOrNull()
            if(amount == null || amount <= 0){
                Toast.makeText(this,"Enter valid amount",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pref = getSharedPreferences("expense", MODE_PRIVATE)
            val oldData = pref.getString("list","") ?: ""

            val category = spinner.selectedItem?.toString() ?: "Others"
            val date = System.currentTimeMillis()

            val newItem = "$amount|$note|$category|$date\n"

            pref.edit()
                .putString("list", oldData + newItem)
                .apply()

            Toast.makeText(this,"Saved ₹$amount",Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ---------- LOAD CATEGORY ----------

    private fun loadCategories(){

        val pref = getSharedPreferences("expense", MODE_PRIVATE)
        val data = pref.getString("categories","") ?: ""

        categoryList = if(data.isEmpty()){
            mutableListOf("Food","Travel","Bills","Shopping")
        }else{
            data.split(",").toMutableList()
        }

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoryList
        )
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spinner.adapter = adapter
    }

    // ---------- ADD CATEGORY ----------

    private fun showAddCategoryDialog(){

        val et = EditText(this)
        et.hint = "Category name"

        AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(et)
            .setPositiveButton("ADD"){_,_ ->

                val newCat = et.text.toString().trim()

                if(newCat.isEmpty()){
                    Toast.makeText(this,"Enter name",Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if(categoryList.contains(newCat)){
                    Toast.makeText(this,"Already exists",Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                categoryList.add(newCat)
                saveCategories()
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    // ---------- DELETE CATEGORY ----------

    private fun deleteCategoryDialog(){

        val selected = spinner.selectedItem?.toString() ?: return

        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Delete \"$selected\" ?")
            .setPositiveButton("DELETE"){_,_ ->

                if(categoryList.size > 1){
                    categoryList.remove(selected)
                    saveCategories()
                    adapter.notifyDataSetChanged()
                }else{
                    Toast.makeText(
                        this,
                        "At least one category required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    // ---------- SAVE ----------

    private fun saveCategories(){

        val pref = getSharedPreferences("expense", MODE_PRIVATE)
        pref.edit()
            .putString("categories", categoryList.joinToString(","))
            .apply()
    }
}
