package com.example.suggest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class Suggest extends Activity {
	
	private EditText origText;
	private ListView suggList;
	private Handler guiThread;
	private ExecutorService suggThread;
	private Runnable updateTask;
	private Future<?> suggPending;
	private List<String> items;
	private ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_suggest);
		initThreading();
		findViews();
		setListeners();
		setAdapters();
	}
	
	private void findViews(){
		origText = (EditText) findViewById(R.id.original_text);
		suggList = (ListView) findViewById(R.id.result_list);
		
	}
	
	private void setAdapters(){
		items = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
		suggList.setAdapter(adapter);
	}
	
	private void setListeners(){
		TextWatcher textWatcher = new TextWatcher(){
			public void beforeTextChanged(CharSequence s, int start, int count, int after){
				/* Do nothing */
			}
			public void onTextChanged(CharSequence s, int start, int before, int count){
				queueUpdate(1000); //milliseconds
			}
			@Override
			public void afterTextChanged(Editable s) {
				// Do nothing 
			}
		};
		origText.addTextChangedListener(textWatcher);
		
		OnItemClickListener clickListener = new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position, long id){
				String query = (String) parent.getItemAtPosition(position);
				doSearch(query);
			}
		};
		suggList.setOnItemClickListener(clickListener);
	}
	
	public void doSearch(String query){
		Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
		intent.putExtra(SearchManager.QUERY, query);
		startActivity(intent);
	}

	private void initThreading(){
		guiThread = new Handler();
		suggThread = Executors.newSingleThreadScheduledExecutor();
		
		updateTask = new Runnable(){

			@Override
			public void run() {
				String original = origText.getText().toString().trim();
				if (suggPending != null)
					suggPending.cancel(true);
				if (original.length() != 0)
					setText(R.string.working);
				try {
					SuggestTask suggestTask = new SuggestTask(
							Suggest.this, 
							original);
					suggPending = suggThread.submit(suggestTask);
				} catch (RejectedExecutionException e) {
					setText(R.string.error);
				}
			}
			
		};
	}
	
	private void queueUpdate(long delayMillis){
		guiThread.removeCallbacks(updateTask);
		guiThread.postDelayed(updateTask, delayMillis);
	}
	
	private void setText(int id){
		adapter.clear();
		adapter.add(getResources().getString(id));
	}
	
	private void setList(List<String> list){
		adapter.clear();
		for (String item : list){
			adapter.add(item);
		}
	}
	
	public void setSuggestions(List<String> suggestions){
		guiSetList(suggList, suggestions);
	}
	
	private void guiSetList(final ListView view, final List<String> list){
		guiThread.post(new Runnable(){
			public void run(){
				setList(list);
			}
		});
	}

}
