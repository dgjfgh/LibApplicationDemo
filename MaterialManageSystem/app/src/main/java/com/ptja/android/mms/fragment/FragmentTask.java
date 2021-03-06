package com.ptja.android.mms.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ptja.android.mms.R;
import com.ptja.android.mms.activity.task.TaskListActivity;
import com.ptja.android.mms.adapter.TaskListAdapter;
import com.ptja.android.mms.adapter.TaskListCaptainAdapter;
import com.ptja.android.mms.bean.TaskBean;
import com.ptja.android.mms.commons.GlobeVariable;
import com.ptja.android.mms.commons.UrlConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import hz.framework.android.View.RefreshLoadMoreListView;
import hz.framework.android.base.BaseFragment;

/**
 * Created by zhenghou on 2016/6/3.
 */
public class FragmentTask extends BaseFragment {

    @Bind(R.id.taskListView)
    RefreshLoadMoreListView taskPullRefreshListView;
    private TaskListAdapter taskListAdapter;
    List<TaskBean> taskBeans = new ArrayList<>();
    private int currentPageIndex = 1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.activity_task_list, null);
            ButterKnife.bind(this, view);
            createTaskListview();
            return view;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void requestTaskListData(final RefreshLoadMoreListView.RefreshUIListener listener) {
        HashMap<String, String> params = new HashMap<>();
        params.put("page", currentPageIndex + "");
        params.put("access_token", GlobeVariable.UserInfos.getAccess_token());
        sendPostRequest(params, "查询中", null, UrlConstants.URL_QUERY_TASK, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    if (currentPageIndex == 1) {
                        taskBeans.clear();
                    }
                    JSONObject response = new JSONObject(s);
                    if (response.getInt("code") == 0) {
                        if (response.get("response") instanceof JSONArray) {
                            JSONArray array = ((JSONArray) response.get("response"));

                            for (int i = 0; i < array.length(); i++) {
                                TaskBean taskBean = com.alibaba.fastjson.JSONObject.parseObject(array.getJSONObject(i).toString(), TaskBean.class);
                               if (taskBean.getStatus_text().equals("未处理")&& Integer.parseInt(GlobeVariable.UserInfos.getUser_type_id())==GlobeVariable.USER_TYPE_STORE_ADMIN){
                                   taskBeans.add(taskBean);
                               }

                            }
                        }
                        Message msg = new Message();
                        msg.what = 2;
                        mHandler.sendMessage(msg);
                        if (listener != null) {
                            listener.onUpdate();
                        }
                    } else {
                        Message msg = new Message();
                        msg.what = 1;
                        msg.obj = response.getString("msg");
                        mHandler.sendMessage(msg);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });
    }

    private void createTaskListview() {
        this.taskPullRefreshListView.setRefreshLoadmoreListenr(new RefreshLoadMoreListView.RefreshLoadMoreListviewListener() {
            @Override
            public void onRefrsh(final RefreshLoadMoreListView.RefreshUIListener listener) {
                currentPageIndex = 1;
                requestTaskListData(listener);
            }

            @Override
            public void loadMore(final RefreshLoadMoreListView.RefreshUIListener listener) {
                requestTaskListData(listener);
            }
        });
        if (Integer.parseInt(GlobeVariable.UserInfos.getUser_type_id())==GlobeVariable.USER_TYPE_CAPTAIN){
            this.taskListAdapter = new TaskListCaptainAdapter(
                    getActivity(), taskBeans, mHandler);
        }else{
            this.taskListAdapter = new TaskListAdapter(
                    getActivity(), taskBeans, mHandler);
        }


        this.taskPullRefreshListView.setAdapter(taskListAdapter, new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view,
                                    int position, long id) {
//                        Intent i = new Intent();
//                        i.setClass(getApplicationContext(),
//                                TaskInfoActivity.class);
//                        i.putExtra(TASK, JSONUtils.toJson(unfinishedTaskListItems.get(position-1)));
//                        startActivity(i);
            }
        });
        requestTaskListData(null);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                Toast.makeText(getActivity(), (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
                break;
            case 2:
                currentPageIndex++;
                taskListAdapter.notifyDataSetChanged();
                break;
            case 100://deal task
                TaskBean taskBean = (TaskBean) msg.obj;
                    Intent intent = new Intent(getContext(), TaskListActivity.class);
                    intent.putExtra("data", (Serializable) msg.obj);
                    startActivity(intent);
                break;
            case 101://complete task
                TaskBean taskBean2 = (TaskBean) msg.obj;
                completeTask(taskBean2);
                break;
        }
        return true;
    }

    private void completeTask(TaskBean taskBean2) {
        HashMap<String, String> params = new HashMap<>();
        params.put("task_id", taskBean2.getTask_id() + "");
        params.put("access_token", GlobeVariable.UserInfos.getAccess_token());
        sendPostRequest(params, "提交中", null, UrlConstants.URL_COMPLETE_TASK, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    JSONObject response = new JSONObject(s);
                    if (response.getInt("code") == 0) {
                        currentPageIndex = 1;
                        requestTaskListData(null);
                    } else {
                        Message msg = new Message();
                        msg.what = 1;
                        msg.obj = response.getString("msg");
                        mHandler.sendMessage(msg);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });
    }

    @Override
    public void onCancel() {

    }

    public void onClick(View view) {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
