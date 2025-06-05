package com.eventHub.backend_eventHub.events.dto;

public class InscriptionStatsDto {
    private int confirmed;
    private int canceled;
    private int available;
    private int maxAttendees;
    private double occupancyRate;

    public InscriptionStatsDto(int confirmed, int canceled, int available,
                               int maxAttendees, double occupancyRate) {
        this.confirmed = confirmed;
        this.canceled = canceled;
        this.available = available;
        this.maxAttendees = maxAttendees;
        this.occupancyRate = occupancyRate;
    }

    // Getters y setters
    public int getConfirmed() { return confirmed; }
    public void setConfirmed(int confirmed) { this.confirmed = confirmed; }

    public int getCanceled() { return canceled; }
    public void setCanceled(int canceled) { this.canceled = canceled; }

    public int getAvailable() { return available; }
    public void setAvailable(int available) { this.available = available; }

    public int getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(int maxAttendees) { this.maxAttendees = maxAttendees; }

    public double getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(double occupancyRate) { this.occupancyRate = occupancyRate; }
}
