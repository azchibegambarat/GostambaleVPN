package com.example.gostambalevpn;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;


import com.example.gostambalevpn.utils.ListModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationListActivity extends Activity {
    private ListView listView;
    private CheckBox checkbox;
    private boolean toggle = false;
    private EditText search_app;
    private TextView tv_contact_title;

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }
    class IgnoreCaseComparator implements Comparator<String> {
        public int compare(String strA, String strB) {
            return strA.compareToIgnoreCase(strB);
        }
    }
    private boolean isUserApp(ApplicationInfo ai, boolean toggle) {
        if(toggle){
            return true;
        }
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (ai.flags & mask) == 0;
    }
    private String getAppLabel(Context context, ApplicationInfo applicationInfo) {
        return (applicationInfo != null ? context.getPackageManager().getApplicationLabel(applicationInfo).toString(): "Unknown");
    }
    public static ArrayList<ListModel> getApplicationList(Context context){
        SharedPreferences prefs = context.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);
        String user_t = prefs.getString("ListModel", null);
        if(user_t != null){
            try {
                return new ObjectMapper().readValue(user_t, new TypeReference<ArrayList<ListModel>>(){});
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
    private void saveApplicationList(Context context, List<ListModel> listModels) throws IOException {
        SharedPreferences prefs = context.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);
        prefs.edit().putString("ListModel", new ObjectMapper().writeValueAsString(listModels)).apply();
    }
    private void load(String search){
        String back = tv_contact_title.getText().toString();
        tv_contact_title.setText("Loading...");
        final PackageManager pm = getPackageManager();

        // List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        ArrayList<ListModel> listModels = new ArrayList<>();
        ArrayList<ListModel> savedListModel = getApplicationList(this);

        HashMap<String, Boolean> stringStringDictionary = new HashMap<>();
        if (savedListModel != null) {
            for (ListModel model : savedListModel) {
                stringStringDictionary.put(model.getPackageName(), true);
            }
        }
        for (PackageInfo packageInfo : packages) {
            if (isUserApp(packageInfo.applicationInfo, toggle) && !packageInfo.packageName.equals(getPackageName())) {
                listModels.add(new ListModel(packageInfo.packageName, getAppLabel(this, packageInfo.applicationInfo), stringStringDictionary.getOrDefault(packageInfo.packageName, false)));
            }
        }
        if(search != null && !search.equalsIgnoreCase("")) {
            listModels = (ArrayList<ListModel>) listModels.stream().filter(ll -> {
                return ll.getPackageName().contains(search) || ll.getCompanyName().contains(search);
            }).collect(Collectors.toList());
        }
        ArrayList<ListModel> modelArrayList = listModels;
        runOnUiThread(()->{
            ApplicationListAdapter applicationListAdapter = new ApplicationListAdapter(ApplicationListActivity.this, modelArrayList);
            IgnoreCaseComparator icc = new IgnoreCaseComparator();
            modelArrayList.sort((a,b)-> icc.compare(a.getCompanyName(),b.getCompanyName()));
            ArrayList<ListModel> finalListModels = modelArrayList;


            applicationListAdapter.setOnItemClickListener((adapterView, view, i, l) -> {
                if (finalListModels.isEmpty()) return;
                ListModel listModel = finalListModels.get(i);
                listModel.setSelected(!listModel.isSelected());
                applicationListAdapter.notifyDataSetChanged();
                try {
                    saveApplicationList(ApplicationListActivity.this, finalListModels.stream().filter(ListModel::isSelected).collect(Collectors.toList()));
                    //Toast.makeText(ApplicationListActivity.this, listModel.getCompanyName(), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(ApplicationListActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            });
            listView.setAdapter(applicationListAdapter);
//            search_app.setEnabled(true);
//            search_app.requestFocus();
            tv_contact_title.setText(back);
        });

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_list);
        search_app = findViewById(R.id.search_app);
        tv_contact_title = findViewById(R.id.tv_contact_title);
        listView = findViewById(R.id.application_list);
        checkbox = findViewById(R.id.checkBox2);
        checkbox.setChecked(toggle);
        checkbox.setOnClickListener(view -> {
            toggle = !toggle;
            load(null);
        });
        load(null);


        LinearLayout ll_about_contact_close = findViewById(R.id.ll_contact_back);
        ll_about_contact_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
            }
        });


        search_app.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //search_app.setEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                load(s.toString());
            }
        });
    }


    public class ApplicationListAdapter extends BaseAdapter implements View.OnClickListener {
        private final SharedPreferences SettingsDetails;
        private AdapterView.OnItemClickListener onItemClickListener;
        /*********** Declare Used Variables *********/
        private Activity activity;
        private ArrayList<ListModel> data;
        private LayoutInflater inflater=null;
        ListModel tempValues=null;
        int i=0;
        public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }
        /*************  CustomAdapter Constructor *****************/
        public ApplicationListAdapter(Activity a, ArrayList<ListModel> d) {

            /********** Take passed values **********/
            activity = a;
            data=d;

            /***********  Layout inflator to call external xml layout () ***********/
            inflater = ( LayoutInflater )activity.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            SettingsDetails = getSharedPreferences("settings_data", 0);

        }

        /******** What is the size of Passed Arraylist Size ************/
        public int getCount() {

            if(data.size()<=0)
                return 1;
            return data.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        /********* Create a holder Class to contain inflated xml file elements *********/
        public  class ViewHolder{

            public TextView text;
            public TextView text1;
            public TextView textWide;
            public ImageView image;
            public CheckBox checkBox;

            public ConstraintLayout khar;
        }

        /****** Depends upon data size called for each row , Create each ListView row *****/
        public View getView(int position, View convertView, ViewGroup parent) {

            View vi = convertView;
            ViewHolder holder;

            if(convertView==null){

                /****** Inflate tabitem.xml file for each row ( Defined below ) *******/
                vi = inflater.inflate(R.layout.application_list, null);

                /****** View Holder Object to contain tabitem.xml file elements ******/

                holder = new ViewHolder();
                holder.text = (TextView) vi.findViewById(R.id.txtTitle);
                holder.text1=(TextView)vi.findViewById(R.id.description);
                holder.image=(ImageView)vi.findViewById(R.id.imgIcon);
                holder.khar = (ConstraintLayout)vi.findViewById(R.id.khar);
                holder.checkBox = (CheckBox)vi.findViewById(R.id.checkBox);

                /************  Set holder with LayoutInflater ************/
                vi.setTag( holder );


            }
            else{

                holder=(ViewHolder)vi.getTag();
            }


            if(data.size()<=0)
            {
                holder.text.setText("No Data");

            }
            else
            {
                /***** Get each Model object from Arraylist ********/
                tempValues=null;
                tempValues = data.get( position );


                /************  Set Model values in Holder elements ***********/
                holder.checkBox.setVisibility(View.VISIBLE);
                if(tempValues.isSelected()){
                    holder.checkBox.setChecked(true);
                    //holder.khar.setBackgroundColor(Color.rgb(0, 34,55));
                    //holder.khar.setBackgroundColor(Color.rgb(0x05, 0xBF,0xDB));
                }else{
                    //holder.khar.setBackgroundColor(Color.rgb(0xFF, 0xFF, 0xFF));
                    holder.checkBox.setChecked(false);
                }
                holder.text.setText( tempValues.getCompanyName() );
                holder.text1.setText( tempValues.getPackageName() );
                Drawable icon = null;
                try {
                    icon = activity.getApplicationContext().getPackageManager().getApplicationIcon(tempValues.getPackageName());
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                holder.image.setImageDrawable(icon);

                /******** Set Item Click Listner for LayoutInflater for each row *******/
                final View view = vi;
                vi.setOnClickListener(v -> {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(null, view, position, -1);
                    }
                });
            }
            return vi;
        }

        @Override
        public void onClick(View v) {
            Log.v("CustomAdapter", "=====Row button clicked=====");
        }

        /********* Called when Item click in ListView ************/

    }

}