package in.deostroll.powerlogger.database;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
//import org.greenrobot.greendao.annotation.Property;

import java.util.Date;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class LogEntry {
    @Id(autoincrement=true)
    private Long id;

    private String powerStatus;

    private int batteryReading;

    private Date timestamp;

    private Boolean isSynched;

    @Generated(hash = 2024188948)
    public LogEntry(Long id, String powerStatus, int batteryReading, Date timestamp,
            Boolean isSynched) {
        this.id = id;
        this.powerStatus = powerStatus;
        this.batteryReading = batteryReading;
        this.timestamp = timestamp;
        this.isSynched = isSynched;
    }

    @Generated(hash = 1393228716)
    public LogEntry() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPowerStatus() {
        return this.powerStatus;
    }

    public void setPowerStatus(String powerStatus) {
        this.powerStatus = powerStatus;
    }

    public int getBatteryReading() {
        return this.batteryReading;
    }

    public void setBatteryReading(int batteryReading) {
        this.batteryReading = batteryReading;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getIsSynched() {
        return this.isSynched;
    }

    public void setIsSynched(Boolean isSynched) {
        this.isSynched = isSynched;
    }
    
}
