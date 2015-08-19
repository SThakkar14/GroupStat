package com.me.shubham.groupstat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
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

    int optionSelected;
    GraphRequest.Callback callback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse graphResponse) {
            processResponse(graphResponse);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setTitle(getIntent().getStringExtra("groupName"));

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

    resultCodeAdapter rca;

    private void displayResults(int optionSelected) {

        rca = new resultCodeAdapter(optionSelected);
        ListView resultListView = (ListView) findViewById(R.id.resultListView);
        resultListView.setAdapter(rca);

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
                    rca.sort(which);
                    rca.notifyDataSetChanged();
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

    public class resultCodeAdapter extends BaseAdapter {

        List<Map.Entry<person, resultingData>> list;

        public resultCodeAdapter(int optionSelected) {
            list = new ArrayList<>(from.entrySet());
            sort(optionSelected);
        }

        public void sort(int optionSelected) {
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
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) ResultPage.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.result_adapter_display, parent, false);
            }
            TextView personNameTextView = (TextView) convertView.findViewById(R.id.personNameTextView);
            TextView likesTextView = (TextView) convertView.findViewById(R.id.likesTextView);
            TextView postsTextView = (TextView) convertView.findViewById(R.id.postsTextView);

            Map.Entry<person, resultingData> entry = list.get(position);
            personNameTextView.setText(entry.getKey().name);
            likesTextView.setText(String.valueOf("Likes: " + entry.getValue().numLikes));
            postsTextView.setText(String.valueOf("Posts: " + entry.getValue().numPosts));

            return convertView;
        }
    }
}
