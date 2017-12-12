package nu.te4;

import java.util.Objects;

public class Box {

    int id;
    String name;
    int color;

    public Box(String name) {
        this.name = name;
        this.id = Integer.parseInt(name.substring(name.indexOf("x") + 1));
    }

    public int getId() {
        return this.id;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    @Override
    public boolean equals(Object obj) {
        Box other = (Box) obj;
        if (!(this.name == null ? other.name == null : this.name.equals(other.name))) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.name);
        return hash;
    }
}
