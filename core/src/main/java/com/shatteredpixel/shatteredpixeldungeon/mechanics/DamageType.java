package com.shatteredpixel.shatteredpixeldungeon.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;

import java.util.HashSet;

public class DamageType {

    public enum Properties {
        IGN_SHIELD,
        INDIRECT,
        MAGIC
    }

    public Object src;

    private HashSet<Properties> props = new HashSet<>();


    public void addProperty(Properties prop) {
        props.add(prop);
    }
    public void removeProperty(Properties prop) { props.remove(prop); }

    public boolean contains(Properties prop) {
        return props.contains(prop);
    }


    //--- Constructors
    public DamageType() {
        this.src = null;
    }

    public DamageType(Object source) {
        this.src = source;
    }
    public DamageType(HashSet<Properties> props) {
        this.src = null;
        this.props = cloneSet(props);
    }
    public DamageType(HashSet<Properties> props, Object source) {
        this.src = source;
        this.props = cloneSet(props);
    }

    private HashSet<Properties> cloneSet(HashSet<Properties> toClone) {
        HashSet<Properties> clone = new HashSet<Properties>();

        for (Properties p : toClone) {
            clone.add(p);
        }

        return clone;
    }
    //---
}