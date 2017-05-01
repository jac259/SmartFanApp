package com.dt11.student16.smartfan;

public class Schedule {
    public int id;
    public int speed;
    public String startTime;
    public String endTime;
    public String direction;
    public String days;
    public Boolean enabled;

    public Schedule() {
        super();
    }

    public Schedule(int id, int speed, String startTime, String endTime, String direction, String days, Boolean enabled) {
        this.id = id;
        this.speed = speed;
        this.startTime = startTime;
        this.endTime = endTime;
        this.direction = direction;
        this.days = days;
        this.enabled = enabled;
    }
}
