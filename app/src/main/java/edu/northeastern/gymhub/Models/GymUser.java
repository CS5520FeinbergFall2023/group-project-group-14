package edu.northeastern.gymhub.Models;

import java.util.ArrayList;
import java.util.List;

public class GymUser {
    private String name;
    private String username;
    private String password;
    private String email;
    private String gym;
    private List<Workout> workoutLog;

    /** Default constructor
     */
    public GymUser(){};

    /**
     * Constructor with params.
     */
    public GymUser(String name, String username, String password, String email, String gym){
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.gym = gym;
        this.workoutLog = new ArrayList<>();
    }

    public void addWorkout(Workout workout){
        this.workoutLog.add(workout);
    }

    public List<Workout> getWorkoutLog(){return this.workoutLog;}

    public String getName(){return this.name;}
    public void setName(String name){this.name = name;}
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGym() {
        return gym;
    }

    public void setGym(String gym) {
        this.gym = gym;
    }
}