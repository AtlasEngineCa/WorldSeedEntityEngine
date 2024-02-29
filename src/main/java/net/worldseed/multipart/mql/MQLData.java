package net.worldseed.multipart.mql;

import net.hollowcube.mql.foreign.Query;

public class MQLData {
    private double time;

    public void setTime(double time) {
        this.time = time;
    }

    @Query
    public double anim_time() {
        return time;
    }
}
