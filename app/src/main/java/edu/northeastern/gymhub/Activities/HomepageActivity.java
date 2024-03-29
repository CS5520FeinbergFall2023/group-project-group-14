package edu.northeastern.gymhub.Activities;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.northeastern.gymhub.Models.GymUser;
import edu.northeastern.gymhub.Models.ScheduleItem;
import edu.northeastern.gymhub.R;
import edu.northeastern.gymhub.Utils.AndroidUtil;
import edu.northeastern.gymhub.Views.ScheduleAdapter;
import edu.northeastern.gymhub.Adapters.HorizontalRVAdapter;

public class HomepageActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ScheduleAdapter scheduleAdapter;
    private RecyclerView recyclerViewPlan;
    private ScheduleAdapter todaysPlanAdapter;
    private DatabaseReference schedulesRef;
    private DatabaseReference trafficRef;
    private BarChart barChart;
    private String gymName;
    private String curUsername;
    private Button scanInButton;
    private RecyclerView horizontalRV;
    private LinearLayoutManager linearLayoutManager;
    private HorizontalRVAdapter horizontalRVAdapter;
    private FirebaseDatabase database;
    private DatabaseReference usersRef;
    private HorizontalRVAdapter.RecyclerClickListener listener;
    private List<GymUser> horizontalRVUsers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        horizontalRVUsers = new ArrayList<>();

        // Disable ability to go back to login page without logging out
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });

        // Inside onCreate() method, after setting the content view
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        gymName = preferences.getString("GymName", "Default Gym Name").toLowerCase();
        curUsername = preferences.getString("Username", "Default User");
        TextView gymNameTextView = findViewById(R.id.textViewGymName);
        gymNameTextView.setText(gymName);

        // connect database
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference().child("users");

        // Profile pic recycler view
        horizontalRV = findViewById(R.id.horizontalRecyclerView);
        fetchHorizontalRvData();

        // Go to workout page
        ImageButton workout = findViewById(R.id.imageButtonWorkout);
        workout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomepageActivity.this, WorkoutPageActivity.class));
            }
        });

        // Go to forum page
        ImageButton forum = findViewById(R.id.imageButtonForum);
        forum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomepageActivity.this, ForumActivity.class);
                startActivity(intent);
            }
        });

        // Go to person page
        ImageButton personPage = findViewById(R.id.imageButtonInbox);
        personPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomepageActivity.this, PersonalInformationDetailsPageActivity.class));
            }
        });

        // Scan in user to gym
        scanInButton = findViewById(R.id.scanInButton);
        setButtonInitialState();

        scanInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanInCurUser();
            }
        });

        // Schedule
        Button schedule = findViewById(R.id.buttonCheckThisWeek);
        schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomepageActivity.this, ScheduleActivity.class));
            }
        });

        // Connect to the schedules node in the database
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules").child(gymName);
        trafficRef = FirebaseDatabase.getInstance().getReference("gyms").child(gymName);

        // Initialize RecyclerView and its adapter
        recyclerView = findViewById(R.id.recyclerViewSchedule);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleAdapter = new ScheduleAdapter();
        recyclerView.setAdapter(scheduleAdapter);

        recyclerViewPlan = findViewById(R.id.recyclerViewTodayPlan);
        recyclerViewPlan.setLayoutManager(new LinearLayoutManager(this));
        todaysPlanAdapter = new ScheduleAdapter();
        recyclerViewPlan.setAdapter(todaysPlanAdapter);
        fetchAndDisplayTodayPlan();

        // Fetch and display today's schedule
        fetchAndDisplayTodaySchedule();

        barChart = findViewById(R.id.barChart);
        fetchAndDisplayHourlyTraffic();
    }

    private void fetchHorizontalRvData() {
        final ArrayList<String> userConnections = new ArrayList<>();

        // Get list of connections to user
        database.getReference("users").child(curUsername).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GymUser curUser = snapshot.getValue(GymUser.class);
                List<String> connections = curUser.getConnections();

                for(String connection : connections){
                    if(!connection.equals("") || !connection.isEmpty()){
                        userConnections.add(connection);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AndroidUtil.handleDatabaseError(error);
            }
        });

        // Fetch all users
        final ArrayList<String> usernamesList = new ArrayList<>();
        final ArrayList<String> nameList = new ArrayList<>();
        final ArrayList<String> imageUris = new ArrayList<>();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    // Get username, status, and name from each user
                    GymUser user = userSnapshot.getValue(GymUser.class);

                    String username = user.getUsername();
                    Boolean status = user.getStatus();
                    String name = user.getName();

                    // Check if the user is at gym and is a connection
                    if (status && userConnections.contains(username)) {
                        // Add data to respective lists
                        horizontalRVUsers.add(user);
                        usernamesList.add(username);
                        nameList.add(name);

                    }
                }
                // Set recycler view
                setHorizontalRV(usernamesList, nameList);

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                AndroidUtil.handleDatabaseError(databaseError);
            }
        });
    }

    private void setHorizontalRV(ArrayList<String> usernamesList, ArrayList<String> nameList) {
        setOnClickListener();
        linearLayoutManager = new LinearLayoutManager(HomepageActivity.this, LinearLayoutManager.HORIZONTAL, false);
        horizontalRVAdapter = new HorizontalRVAdapter(HomepageActivity.this, listener, usernamesList, nameList);
        horizontalRV.setLayoutManager(linearLayoutManager);
        horizontalRV.setAdapter(horizontalRVAdapter);
    }

    /** Sets methods to adapter listener **/
    private void setOnClickListener() {
        listener = new HorizontalRVAdapter.RecyclerClickListener() {

            @Override
            public void onUserClicked(View v, int position) {
                GymUser clickedUser = horizontalRVUsers.get(position);
                showClickedUser(clickedUser);
            }

        };
    }

    private void showClickedUser(GymUser gymUser) {
        AndroidUtil.showClickedUserDialogBox(this, gymUser);
    }

    private void setButtonInitialState() {
        DatabaseReference statusRef = usersRef.child(curUsername).child("status");
        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean status = snapshot.getValue(Boolean.class);

                // Set the initial state based on the database status
                if (status != null && status) {
                    // If scanned in
                    scanInButton.setText("SCAN OUT");
                    int color = ContextCompat.getColor(HomepageActivity.this, R.color.lightRed);
                    scanInButton.setBackgroundColor(color);
                } else {
                    // If not scanned in (or status is null)
                    scanInButton.setText("SCAN IN");
                    int color = ContextCompat.getColor(HomepageActivity.this, R.color.green);
                    scanInButton.setBackgroundColor(color);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AndroidUtil.showToast(HomepageActivity.this, "Error getting initial status.");
            }
        });
    }

    private void scanInCurUser() {

        DatabaseReference statusRef = usersRef.child(curUsername).child("status");
        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean status = snapshot.getValue(Boolean.class);

                // If not scanned in
                if(!status){
                    statusRef.setValue(true);
                    scanInButton.setText("SCAN OUT");
                    int color = ContextCompat.getColor(HomepageActivity.this, R.color.lightRed);
                    scanInButton.setBackgroundColor(color);
                    AndroidUtil.showToast(HomepageActivity.this, "You have scanned in.");

                    // If scanned out
                } else{
                    statusRef.setValue(false);
                    scanInButton.setText("SCAN IN");
                    int color = ContextCompat.getColor(HomepageActivity.this, R.color.green);
                    scanInButton.setBackgroundColor(color);
                    AndroidUtil.showToast(HomepageActivity.this, "You have scanned out.");
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AndroidUtil.showToast(HomepageActivity.this, "Error scanning in.");
            }
        });
    }

    private void fetchAndDisplayTodaySchedule() {
        // Get the current day
        String currentDay = getCurrentDay();

        // Add a ValueEventListener to fetch data from Firebase for today's classes
        schedulesRef.child(currentDay).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Call the method to handle data changes
                handleDataChange(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomepageActivity.this, "Failed to load today's schedule.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to handle data changes in onDataChange
    private void handleDataChange(DataSnapshot dataSnapshot) {
        // Clear the previous data
        List<ScheduleItem> todayScheduleList = new ArrayList<>();

        // Iterate through the classes in the current day
        for (DataSnapshot timeSnapshot : dataSnapshot.getChildren()) {
            // Check if the snapshot has children (classes)
            if (timeSnapshot.hasChildren()) {
                for (DataSnapshot classSnapshot : timeSnapshot.getChildren()) {
                    String className = classSnapshot.child("name").getValue(String.class);
                    String startTime = classSnapshot.child("start_time").getValue(String.class);
                    String finishTime = classSnapshot.child("finish_time").getValue(String.class);

                    // Assuming you have a method to convert time to a readable format
                    String formattedTime = formatTime(startTime, finishTime);

                    // Add the data to the list
                    todayScheduleList.add(new ScheduleItem(className, formattedTime));
                }
            }
        }

        // Clear the previous data in the adapter
        scheduleAdapter.clearSchedule();

        // Add the today's schedule to the adapter
        scheduleAdapter.setScheduleList(todayScheduleList);

        // Notify the adapter that the data has changed
        scheduleAdapter.notifyDataSetChanged();
    }

    private String getCurrentDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        Date d = new Date();
        return sdf.format(d);
    }

    private String formatTime(String startTime, String finishTime) {
        // You may need to adjust the formatting based on your needs
        return startTime + " - " + finishTime;
    }

    private void fetchAndDisplayHourlyTraffic() {
        // Get the current day
        String currentDay = getCurrentDay();

        // Add a ValueEventListener to fetch data from Firebase for today's hourly traffic
        trafficRef.child("hourly_traffic").child(currentDay).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Call the method to handle hourly traffic data changes
                handleHourlyTrafficChange(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomepageActivity.this, "Failed to load hourly traffic data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to handle hourly traffic data changes
    private void handleHourlyTrafficChange(DataSnapshot dataSnapshot) {
        List<Integer> hours = new ArrayList<>();
        List<Integer> trafficValues = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        // Iterate through days (e.g., "Monday", "Tuesday", etc.)
        for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
            if (daySnapshot.hasChildren()) {
                // Extracting the traffic value for each hour
                int hour = daySnapshot.child("hour").getValue(Integer.class);
                int traffic = daySnapshot.child("traffic").getValue(Integer.class);
                int color = determineColor(traffic);

                // Append data to arrays
                hours.add(hour);
                trafficValues.add(traffic);
                colors.add(color);
            }
        }
        // Now you have the list of hours and traffic values for all days
        setupBarChart(hours, trafficValues, colors);
    }

    private int determineColor(int traffic) {
        // Customize the color based on the number of people
        if (traffic > 200) {
            return Color.rgb(255, 69, 0); // Dark Red
        } else if (traffic >= 150 && traffic <= 200) {
            return Color.rgb(255, 99, 71); // Red
        } else if (traffic >= 100 && traffic < 150) {
            return Color.rgb(255, 165, 0); // Orange
        } else {
            return Color.rgb(144, 238, 144); // Light Green
        }
    }

    private void setupBarChart(List<Integer> hours, List<Integer> trafficValues, List<Integer> colors) {
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < hours.size(); i++) {
            entries.add(new BarEntry(hours.get(i), trafficValues.get(i)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Hourly Traffic");
        dataSet.setColors(colors);

        BarData data = new BarData(dataSet);

        // Set the data to the chart
        barChart.setData(data);

        // Customize the appearance of the chart if needed

        // Refresh the chart to update the display
        barChart.invalidate();
    }

    private void fetchAndDisplayTodayPlan() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(curUsername).child("classScheduled");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ScheduleItem> todayPlanItems = new ArrayList<>();

                // Iterate through registration keys
                for (DataSnapshot registrationKeySnapshot : dataSnapshot.getChildren()) {
                    String date = registrationKeySnapshot.child("date").getValue(String.class);

                    if (todayDate.equals(date)) {
                        String className = registrationKeySnapshot.child("className").getValue(String.class);

                        // Retrieve the start and end times as integers
                        int startHour = registrationKeySnapshot.child("startTime").child("hour").getValue(Integer.class);
                        int startMinute = registrationKeySnapshot.child("startTime").child("minute").getValue(Integer.class);
                        int endHour = registrationKeySnapshot.child("endTime").child("hour").getValue(Integer.class);
                        int endMinute = registrationKeySnapshot.child("endTime").child("minute").getValue(Integer.class);

                        // Format the time
                        String formattedTime = formatTime(startHour, startMinute, endHour, endMinute);

                        // Create a ScheduleItem object with the retrieved data
                        ScheduleItem todayPlanItem = new ScheduleItem(className, formattedTime);
                        todayPlanItems.add(todayPlanItem);
                    }

                }

                // Clear the previous data in the adapter
                todaysPlanAdapter.clearSchedule();

                // Add the today's plan to the adapter
                todaysPlanAdapter.setScheduleList(todayPlanItems);

                // Notify the adapter that the data has changed
                todaysPlanAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors
            }
        });
    }

    private String formatTime(int startHour, int startMinute, int endHour, int endMinute) {
        // Format the time as needed
        return String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute);
    }
}