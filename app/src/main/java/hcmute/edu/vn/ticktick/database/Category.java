package hcmute.edu.vn.ticktick.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String iconName;

    public Category() {}

    @Ignore
    public Category(String name, String iconName) {
        this.name = name;
        this.iconName = iconName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
