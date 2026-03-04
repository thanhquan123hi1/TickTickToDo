package hcmute.edu.vn.ticktick.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;     // ten_danh_muc
    private String iconName; // bieu_tuong

    public Category() {}

    public Category(String name, String iconName) {
        this.name = name;
        this.iconName = iconName;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    @Override
    public String toString() {
        return name;
    }
}
