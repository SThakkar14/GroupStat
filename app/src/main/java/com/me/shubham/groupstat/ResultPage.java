package com.me.shubham.groupstat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResultPage extends Activity {
    Map<person, resultingData> from;
    ProgressDialog progressDialog;

    TextView test;
    GraphRequest.Callback callback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse graphResponse) {
            processResponse(graphResponse);
        }
    };
    int optionSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setTitle(getIntent().getStringExtra("groupName"));

        test = (TextView) findViewById(R.id.something);
        from = new HashMap<>();

        optionSelected = getIntent().getIntExtra("optionSelected", 0);

        progressDialog = new ProgressDialog(this, R.style.MyTheme);
        progressDialog.setCancelable(false);
        progressDialog.show();

        getPosts();
    }

    private void getPosts() {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "from, likes.summary(true)");
        parameters.putString("limit", "1000000");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + getIntent().getStringExtra("groupId") + "/feed",
                parameters,
                HttpMethod.GET,
                callback
        ).executeAsync();
    }

    private void nextPosts(GraphRequest request) {
        request.setCallback(callback);
        GraphRequest.executeBatchAsync(request);
    }

    private void processResponse(GraphResponse graphResponse) {
        try {
            JSONArray allData = graphResponse.getJSONObject().getJSONArray("data");
            if (allData.length() == 0) {
                displayResults(optionSelected);
            } else {
                for (int numDataPoint = 0; numDataPoint < allData.length(); numDataPoint++) {

                    JSONObject post = allData.getJSONObject(numDataPoint);

                    JSONObject dataPoint = post.getJSONObject("from");
                    person newOne = new person(dataPoint.get("name").toString(), dataPoint.get("id").toString());

                    resultingData dataAboutPost = from.get(newOne);
                    int numLikes = (Integer) post.getJSONObject("likes").getJSONObject("summary").get("total_count");

                    if (dataAboutPost == null) {
                        dataAboutPost = new resultingData(1, numLikes);
                        from.put(newOne, dataAboutPost);
                    } else {
                        dataAboutPost.numPosts += 1;
                        dataAboutPost.numLikes += numLikes;
                    }
                }
                nextPosts(graphResponse.getRequestForPagedResults(GraphResponse.PagingDirection.NEXT));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayResults(int optionSelected) {

        List<Map.Entry<person, resultingData>> list = new ArrayList<>(from.entrySet());

        if (optionSelected == 0) {
            Collections.sort(list, new Comparator<Map.Entry<person, resultingData>>() {
                @Override
                public int compare(Map.Entry<person, resultingData> lhs, Map.Entry<person, resultingData> rhs) {
                    return rhs.getValue().numLikes - lhs.getValue().numLikes;
                }
            });
        } else if (optionSelected == 1) {
            Collections.sort(list, new Comparator<Map.Entry<person, resultingData>>() {
                @Override
                public int compare(Map.Entry<person, resultingData> lhs, Map.Entry<person, resultingData> rhs) {
                    return rhs.getValue().numPosts - lhs.getValue().numPosts;
                }
            });
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<person, resultingData> entry : list) {
            if (optionSelected == 0) {
                if (entry.getValue().numLikes != 0)
                    sb.append(entry.getKey().name).append(": ").append(entry.getValue().numLikes).append("\n");
            } else if (optionSelected == 1) {
                sb.append(entry.getKey().name).append(": ").append(entry.getValue().numPosts).append("\n");
            }
        }

        test.setText(sb.toString());
        progressDialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_result_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            LoginManager.getInstance().logOut();

            Intent intent = new Intent(this, LoginPage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            return true;
        } else if (id == R.id.action_sort) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ResultPage.this);
            String[] options = {"Likes", "Posts"};
            builder.setTitle("Sort by...").setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which == 0) {
                        displayResults(0);
                    } else if (which == 1){
                        displayResults(1);
                    }
                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).create().show();
        }

        return super.onOptionsItemSelected(item);
    }

    public class person {
        String name;
        String id;

        public person(String name, String id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            if (getClass() != other.getClass())
                return false;

            person p = (person) other;
            return Objects.equals(p.id, this.id) && Objects.equals(this.name, p.name);
        }

        @Override
        public String toString() {
            return this.name + ": " + this.id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    public class resultingData {
        int numPosts;
        int numLikes;

        public resultingData(int numPosts, int numLikes) {
            this.numPosts = numPosts;
            this.numLikes = numLikes;
        }

        @Override
        public String toString() {
            return String.valueOf(numPosts) + String.valueOf(numLikes);
        }
    }
}
