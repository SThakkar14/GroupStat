package com.me.shubham.groupstat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import java.util.List;

public class GroupPage extends Activity {
    ListView listview;
    CodeLearnAdapter cda;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_page);
        listview = (ListView) findViewById(R.id.listview);

        progressDialog = new ProgressDialog(this, R.style.MyTheme);
        progressDialog.setCancelable(false);
        progressDialog.show();

        createList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group_page, menu);
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
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void createList() {
        Bundle parameters = new Bundle();
        parameters.putString("limit", "1000");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/groups",
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        if (response != null) {
                            try {
                                List<GroupInfo> groups = new ArrayList<>();
                                JSONArray allGroups = response.getJSONObject().getJSONArray("data");
                                for (int numGroups = 0; numGroups < allGroups.length(); numGroups++) {
                                    JSONObject group = allGroups.getJSONObject(numGroups);

                                    GroupInfo g = new GroupInfo(group.get("id").toString(), group.get("name").toString());
                                    groups.add(g);
                                }

                                cda = new CodeLearnAdapter(groups);
                                listview.setAdapter(cda);
                                progressDialog.dismiss();

                                listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        GroupInfo groupInfo = cda.getGroupInfo(position);

                                        Intent intent = new Intent(GroupPage.this, ResultPage.class);
                                        intent.putExtra("groupId", groupInfo.id);
                                        intent.putExtra("groupName", groupInfo.name);
                                        startActivity(intent);
                                    }
                                });

                            } catch (JSONException ignored) {
                            }
                        }
                    }
                }
        ).executeAsync();
    }

    private class GroupInfo {
        String name;
        String id;

        public GroupInfo(String id, String name) {
            this.name = name;
            this.id = id;
        }
    }

    public class CodeLearnAdapter extends BaseAdapter {
        List<GroupInfo> groups;

        public CodeLearnAdapter(List<GroupInfo> groups) {
            this.groups = groups;
        }

        @Override
        public int getCount() {
            return groups.size();
        }

        @Override
        public Object getItem(int position) {
            return groups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) GroupPage.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.group_adapter_display, parent, false);
            }
            TextView name = (TextView) convertView.findViewById(R.id.textview);
            name.setText(groups.get(position).name);

            return convertView;
        }

        public GroupInfo getGroupInfo(int position) {
            return groups.get(position);
        }
    }
}
