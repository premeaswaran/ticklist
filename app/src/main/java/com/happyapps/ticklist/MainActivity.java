package com.happyapps.ticklist;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NewListDialog.DialogListener, RenameListDialog.DialogListener
{
    SQLiteDatabase mainDB;
    ArrayList<String> listTitles;
    ListView mainList;
    String selectedList;
    String shareText = null;
    RenameListDialog renameListDialog;
    Intent intent;

    public void openListContent(View view, int i){
        Intent intent = new Intent(getApplicationContext(), ListContentActivity.class);
        intent.putExtra("currentList", mainList.getItemAtPosition(i).toString());
        startActivity(intent);
    }

    public void openDialog(){
        NewListDialog newListDialog = new NewListDialog();
        newListDialog.show(getSupportFragmentManager(), "New list title");
    }

    public void openRenameDialog(String selectedList){
        renameListDialog = new RenameListDialog();
        renameListDialog.show(getSupportFragmentManager(), "New list title");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listTitles = new ArrayList<>();
        mainList = (ListView) findViewById(R.id.mainList);
        registerForContextMenu(mainList);
        mainDB = this.openOrCreateDatabase("Lists", MODE_PRIVATE, null);

        pullList();

        mainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                openListContent(view, i);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "New list to be created", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                openDialog();
            }
        });
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            return true;
        }
        if (id == R.id.action_feedback) {
            return true;
        }

        if (id==android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void transmitText(String title) {
        try {
            mainDB.execSQL("CREATE TABLE IF NOT EXISTS lists (ID INTEGER PRIMARY KEY AUTOINCREMENT, list VARCHAR)");
            String query = "SELECT * FROM lists WHERE list='" + title + "'";
            Cursor c1;
            c1 = mainDB.rawQuery(query, null);
            if(c1!=null && c1.moveToFirst()) {
                Toast.makeText(getApplicationContext(), "The list already exists", Toast.LENGTH_LONG).show();
            } else{
                AddToDB(title);
                FancyToast.makeText(getApplicationContext(), "New list '" + title + "' created", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, true).show();
            }
            c1.close();
        } catch (Exception e){
            Log.i("Exception error", e.getMessage());
        }

    }

    public void transmitTextForRename(String title){
        try {
            mainDB.execSQL("CREATE TABLE IF NOT EXISTS lists (ID INTEGER PRIMARY KEY AUTOINCREMENT, list VARCHAR)");
            String query = "SELECT * FROM lists WHERE list='" + selectedList + "'";
            Cursor c1;
            c1 = mainDB.rawQuery(query, null);
            if(c1!=null && c1.moveToFirst()) {
                String tempQuery = "SELECT * FROM lists WHERE list='" + title + "'";
                Cursor c2;
                c2 = mainDB.rawQuery(tempQuery, null);
                if(c2!=null && c2.moveToFirst()){
                    Toast.makeText(getApplicationContext(), "A list with the name '" + title + "' exists already. Enter a unique name", Toast.LENGTH_LONG).show();
                } else {
                    String query2 = "UPDATE lists SET list='" + title + "' WHERE list='" + selectedList + "'";
                    Log.i("Update Query", query2);
                    mainDB.execSQL(query2);
                    c1.moveToNext();
                }
                c2.close();
            }
            c1.close();
        } catch (Exception e){
            Log.i("Exception error", e.getMessage());
        }
        pullList();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedList = ((TextView) info.targetView).getText().toString();

        menu.setHeaderTitle("Select the action");
        menu.add(0, v.getId(), 0, "Delete");
        menu.add(0, v.getId(), 0, "Rename");
        menu.add(0, v.getId(), 0, "Share");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle()=="Delete"){
            RemoveFromDB(selectedList);
        } else if(item.getTitle()=="Rename"){
            openRenameDialog(selectedList);
        } else if(item.getTitle()=="Share"){
            shareList(selectedList);
        }
        return super.onContextItemSelected(item);
    }

    public void shareList(String selectedList){
        shareText="";
        try {
            mainDB.execSQL("CREATE TABLE IF NOT EXISTS listitems (list VARCHAR, item VARCHAR, checkStatus INTEGER)");
            String query = "SELECT * FROM listitems WHERE list='" + selectedList + "'";
            Cursor c1;
            c1 = mainDB.rawQuery(query, null);
            int itemIndex = c1.getColumnIndex("item");
            if(c1!=null && c1.moveToFirst()) {
                while(c1!=null){
                    if(shareText!=null) {
                        shareText += "*~ " + c1.getString(itemIndex) + "*\n";
                    } else {
                        shareText = "*~ " + c1.getString(itemIndex) + "*\n";
                    }
                    c1.moveToNext();
                }
            } else {
                shareText = "*The list is empty*";
            }
            c1.close();
        } catch (Exception e){
            Log.i("Exception error", e.getMessage());
        }
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, selectedList + " - Checklist (Shared via Ticklist: The simplest checklist app on Android)");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent,"Share via..."));
    }

    public void RemoveFromDB(String selectedList){
        try {
            mainDB.execSQL("CREATE TABLE IF NOT EXISTS lists (ID INTEGER PRIMARY KEY AUTOINCREMENT, list VARCHAR)");
            String query1 = "DELETE FROM lists WHERE list='" + selectedList + "'";
            mainDB.execSQL(query1);
            mainDB.execSQL("CREATE TABLE IF NOT EXISTS listitems (list VARCHAR, item VARCHAR, checkStatus INTEGER)");
            String query2 = "DELETE FROM listitems WHERE list='" + selectedList + "'";
            mainDB.execSQL(query2);
            pullList();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void AddToDB(String title){
        try {
            mainDB.execSQL("CREATE TABLE IF NOT EXISTS lists (ID INTEGER PRIMARY KEY AUTOINCREMENT, list VARCHAR)");
            String query = "INSERT INTO lists (list) VALUES ('" + title + "')";
            mainDB.execSQL(query);
            pullList();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void pullList(){
        listTitles.clear();
        String query = "SELECT * FROM lists";
        try {
            Cursor c1 = mainDB.rawQuery(query, null);
            int listIndex = c1.getColumnIndex("list");
            if(c1!=null && c1.moveToFirst()) {
                while (c1 != null) {
                    listTitles.add(c1.getString(listIndex));
                    c1.moveToNext();
                }
            }

        } catch (Exception e){
            Log.i("Exception error", e.getMessage());
        }

        ArrayAdapter<String> listArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listTitles);
        mainList.setAdapter(listArrayAdapter);
    }
}
