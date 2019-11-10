package com.happyapps.ticklist;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class ListContentActivity extends AppCompatActivity {

    ArrayList<String> selectedItems;
    EditText addItemText;
    Button addItemBtn;
    ArrayList<String> items;
    ListView listItems;
    SQLiteDatabase mainDB;
    String currentList;
    String selectedItem;
    ArrayAdapter<String> adapter;
    CheckedTextView checkableItem;
    ArrayList<Boolean> temp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_content);

        selectedItems = new ArrayList<>();
        addItemText = (EditText) findViewById(R.id.add_item_text);
        addItemText.setFocusableInTouchMode(true);
        addItemText.requestFocus();
        addItemBtn = (Button) findViewById(R.id.add_item_btn);
        items = new ArrayList<>();
        listItems = (ListView) findViewById(R.id.listItems);
        listItems.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        registerForContextMenu(listItems);
        checkableItem = (CheckedTextView) findViewById(R.id.checkableItem);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        currentList = intent.getStringExtra("currentList");
        this.setTitle(currentList);

        try {
            mainDB = this.openOrCreateDatabase("Lists", MODE_PRIVATE, null);
            mainDB.execSQL("CREATE TABLE IF NOT EXISTS listitems (list VARCHAR, item VARCHAR, checkStatus INTEGER)");
        } catch (Exception e){
            e.printStackTrace();
        }
        pullItems();

        addItemText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (i == EditorInfo.IME_ACTION_DONE)) {
                    //do what you want on the press of 'done'
                    addItemBtn.performClick();
                    addItemText.getText().clear();
                }
                return false;
            }
        });

        listItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String clickedItem = ((TextView)view).getText().toString();

                try{
                    mainDB.execSQL("CREATE TABLE IF NOT EXISTS listitems (list VARCHAR, item VARCHAR, checkStatus INTEGER)");
                } catch (Exception e){
                    e.printStackTrace();
                }

                if(selectedItems.contains(clickedItem)){
                    try {
                        String updateQuery1 = "UPDATE listitems SET checkStatus=0 WHERE list='" + currentList + "' AND item='" + clickedItem + "'";
                        mainDB.execSQL(updateQuery1);
                        Log.i("Checkstatus", "Updated as False");
                        selectedItems.remove(clickedItem);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                } else {
                    try {
                        String updateQuery2 = "UPDATE listitems SET checkStatus=1 WHERE list='" + currentList + "' AND item='" + clickedItem + "'";
                        mainDB.execSQL(updateQuery2);
                        Log.i("Checkstatus", "Updated as True");
                        selectedItems.add(clickedItem);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                pullItems();
            }
        });

        addItemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddItemToList();
                addItemText.getText().clear();
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedItem = ((TextView) info.targetView).getText().toString();

        menu.setHeaderTitle("Select the action");
        menu.add(0, v.getId(), 0, "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle() == "Delete") {
            RemoveItem(currentList, selectedItem);
            return super.onContextItemSelected(item);
        }
        return false;
    }

    public void AddItemToList(){
        String itemText = addItemText.getText().toString();
        if(!itemText.isEmpty()) {
            items.add(itemText);
            AddItemToDB(itemText, currentList);
        } else {
            Toast.makeText(getApplicationContext(), "Item cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

    public void AddItemToDB(String itemText, String currentList){
        try {
            mainDB.execSQL("CREATE TABLE IF NOT EXISTS listitems (list VARCHAR, item VARCHAR, checkStatus INTEGER)");
            String query = "INSERT INTO listitems (list, item, checkStatus) VALUES ('" + currentList + "', '" + itemText + "', 0)";
            mainDB.execSQL(query);
        } catch (Exception e){
            e.printStackTrace();
            Log.i("Not Inserted", e.getMessage());
        }
        pullItems();
    }

    public void RemoveItem(String currentList, String selectedItem){
        try {
            mainDB.execSQL("CREATE TABLE IF NOT EXISTS listitems (list VARCHAR, item VARCHAR, checkStatus INTEGER)");
            String query1 = "DELETE FROM listitems WHERE list='" + currentList + "' AND item='" + selectedItem + "'";
            mainDB.execSQL(query1);
        } catch (Exception e){
            e.printStackTrace();
        }
        pullItems();
    }

    public void pullItems(){
        temp = null;
        items.clear();
        temp = new ArrayList<>();
        String query = "SELECT * FROM listitems WHERE list='" + currentList + "'";
        Log.i("Came", "Here 1");
        try {
            Cursor c = mainDB.rawQuery(query, null);
            int itemIndex = c.getColumnIndex("item");
            int checkIndex = c.getColumnIndex("checkStatus");
            Log.i("Came", "Here 2");
            if(c!=null && c.moveToFirst()) {
                while (c != null) {
                    items.add(c.getString(itemIndex));
                    boolean checkState = false;
                    if(c.getInt(checkIndex)==1){
                        Log.i("Came", "Here 3");
                        checkState = true;
                        temp.add(c.getPosition(), checkState);
                        selectedItems.add(c.getString(itemIndex));
                    } else {
                        Log.i("Came", "Here 4");
                        temp.add(c.getPosition(), checkState);
                    }
                    Log.i("Lets check the position of the cursor", String.valueOf(c.getPosition()));
                    c.moveToNext();
                }
            }
            c.close();
        } catch (Exception e){
            Log.i("Exception error", e.getMessage());
        }

        adapter = new ArrayAdapter<String>(this, R.layout.item_layout, items);
        listItems.setAdapter(adapter);
        for(int i=0; i<temp.size(); i++) {
            listItems.setItemChecked(i,temp.get(i));
        }
    }
}