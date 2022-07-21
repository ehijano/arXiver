package com.example.arXiver_module;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class SearchActivity extends ParentActivity {

    EditText titleEditText;
    EditText authorEditText;
    EditText abstractEditText;
    EditText idEditText;
    EditText allEditText;
    RadioGroup radioGroupSB;
    RadioGroup radioGroupO;

    MultiAutoCompleteTextView categoriesMATV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        this.setTitle(getResources().getString(R.string.app_name)+" - "+ getResources().getString(R.string.search));

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Obtaining the edit fields
        titleEditText =  findViewById(R.id.titleEditText);
        authorEditText =  findViewById(R.id.authorEditText);
        abstractEditText =  findViewById(R.id.abstractEditText);
        idEditText = findViewById(R.id.idEditText);
        allEditText = findViewById(R.id.allEditText);

        // sort by
        radioGroupSB = findViewById(R.id.sortByRadioGroup);
        // order
        radioGroupO = findViewById(R.id.orderRadioGroup);

        // categories autocomplete
        categoriesMATV = findViewById(R.id.categoriesMultiAutoCompleteTextView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, getAllCategoriesMemory()
        );
        categoriesMATV.setAdapter(adapter);
        categoriesMATV.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        // Button
        Button searchButton = findViewById(R.id.searchButton);

        View.OnClickListener searchListener = view -> {
            // Obtain data from textFields
            String query = getQuery();
            // Start Activity for found papers
            if(!query.isEmpty()){
                Intent intent = new Intent(getBaseContext(), ResultsActivity.class);
                intent.putExtra("QUERY", query);
                startActivity(intent);
            }else{
                Toast.makeText(this, getResources().getString(R.string.search_empty) , Toast.LENGTH_SHORT).show();
            }
        };
        searchButton.setOnClickListener(searchListener);

    }

    public static String addToQuery(String q) {
        if(!q.isEmpty()) {
            return q + "+AND+";
        }else {
            return q;
        }
    }

    public String getQuery() {
        String query = "";
        String allCategories = categoriesMATV.getText().toString();
        if(!allCategories.isEmpty()) {
            String[] categories = categoriesMATV.getText().toString().split("\\s*,\\s*");
            query = addToQuery(query);
            query = query + "(cat:" + ArXivScanner.join(")+AND+(cat:",categories)+")";
        }

        String allS = allEditText.getText().toString();
        query = addTypedToQuery(query, allS,"all");

        String titleS = titleEditText.getText().toString();
        query = addTypedToQuery(query, titleS,"ti");

        String authorS = authorEditText.getText().toString();
        query = addTypedToQuery(query, authorS,"au");

        String abstractS = abstractEditText.getText().toString();
        query = addTypedToQuery(query, abstractS,"abs");

        String idS = idEditText.getText().toString();
        if(!idS.isEmpty()) {
            query = addToQuery(query);
            query = query + "(id:"+idS+")";
        }

        if(!query.isEmpty()){
            int radioSBId = radioGroupSB.getCheckedRadioButtonId();
            RadioButton rbSB = findViewById(radioSBId);
            String choiceSB = rbSB.getText().toString();
            if(choiceSB.equals(getResources().getString(R.string.sort_by_relevance))){
                query = query + "&sortBy=relevance";
            } else if(choiceSB.equals(getResources().getString(R.string.sort_by_last_updated))){
                query = query + "&sortBy=lastUpdatedDate";
            } else if(choiceSB.equals(getResources().getString(R.string.sort_by_published))){
                query = query + "&sortBy=submittedDateDate";
            }

            int radioOId = radioGroupO.getCheckedRadioButtonId();
            RadioButton rbO = findViewById(radioOId);
            String choiceO = rbO.getText().toString();
            if(choiceO.equals(getResources().getString(R.string.ascending))){
                query = query + "&sortOrder=ascending";
            } else if(choiceO.equals(getResources().getString(R.string.descending))){
                query = query + "&sortOrder=descending";
            }
        }

        return query;
    }

    public String addTypedToQuery(String q, String typed, String code) {
        if(typed.isEmpty()){
            return q;
        }else{
            String newQuery;
            if(q.isEmpty()){
                newQuery = "";
            }else {
                newQuery = q + "+AND+";
            }
            String[] typedWordsEncoded = typed.split("\\s+");
            String[] typedWordsDecoded = new String[typedWordsEncoded.length];
            for(int i=0; i<typedWordsEncoded.length; i++){
                try {
                    typedWordsDecoded[i] = URLEncoder.encode(typedWordsEncoded[i], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Toast.makeText(this, getResources().getString(R.string.encoding_exception), Toast.LENGTH_SHORT).show();
                }
            }
            newQuery = newQuery + "("+code+":" + ArXivScanner.join(")+AND+("+code+":",typedWordsDecoded)+")";
            return newQuery;
        }
    }

}