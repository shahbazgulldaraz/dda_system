package org.ddaSystem;

public class Venture {
    private String name;
    private int priority;
    // Other properties, getters, setters

    public Venture(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "Venture{" +
                "name='" + name + '\'' +
                ", priority=" + priority +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }


}
