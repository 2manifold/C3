package com.example.myapplication__volume.game;

public class Quest {
    public enum Status
    {
        UnFinished, Pending, Finished
    }

    private String content = "";
    private Status status = Status.UnFinished;

    private int alreadyDone = 0;
    private int toBeDone = 0;

    private int reward = 0;

    Quest(){

    }

    Quest(String content, Status status, int alreadyDone, int toBeDone){
        this.content = content;
        this.status = status;
        this.alreadyDone = alreadyDone;
        this.toBeDone = toBeDone;
    }

    Quest(String content, int alreadyDone, int toBeDone, int reward){
        this.content = content;
        this.alreadyDone = alreadyDone;
        this.toBeDone = toBeDone;
        this.reward = reward;
    }

    public void setContent(String content){
        this.content = content;
    }

    public void setStatus(Status status){
        this.status = status;
    }

    public void setAlreadyDone(int alreadyDone) {
        this.alreadyDone = alreadyDone;
    }

    public void setToBeDone(int toBeDone) {
        this.toBeDone = toBeDone;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public String getContent() {
        return content;
    }

    public Status getStatus() {
        return status;
    }

    public int getAlreadyDone() {
        return alreadyDone;
    }

    public int getToBeDone() {
        return toBeDone;
    }

    public int getReward() {
        return reward;
    }

}